package com.server.edge.gallery.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ChatSessionRepository].
 *
 * Tests session persistence logic: load, save, upsert, delete, and active chat tracking.
 */
class ChatSessionRepositoryTest {

    private val mockContext: Context = mockk()
    private val mockPrefs: SharedPreferences = mockk()
    private val mockEditor: SharedPreferences.Editor = mockk()

    private lateinit var repository: ChatSessionRepository

    @Before
    fun setUp() {
        every { mockContext.applicationContext } returns mockContext
        every {
            mockContext.getSharedPreferences("chat_sessions_prefs", Context.MODE_PRIVATE)
        } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit

        repository = ChatSessionRepository(mockContext)
    }

    @Test
    fun `loadSessions returns empty list when no sessions stored`() {
        every { mockPrefs.getString("chat_sessions", null) } returns null

        val sessions = repository.loadSessions()
        assertTrue(sessions.isEmpty())
    }

    @Test
    fun `loadSessions returns stored sessions`() {
        val sessions = listOf(
            ChatSession("1", "Test", "2025-01-01T00:00:00Z", emptyList())
        )
        val json = ChatSession.toJson(sessions)
        every { mockPrefs.getString("chat_sessions", null) } returns json

        val loaded = repository.loadSessions()

        assertEquals(1, loaded.size)
        assertEquals("Test", loaded[0].title)
    }

    @Test
    fun `saveSessions persists sessions`() {
        val sessions = listOf(
            ChatSession("1", "First", "2025-01-01T00:00:00Z", emptyList()),
            ChatSession("2", "Second", "2025-01-02T00:00:00Z", emptyList())
        )

        repository.saveSessions(sessions)

        val jsonSlot = slot<String>()
        verify { mockEditor.putString("chat_sessions", capture(jsonSlot)) }
        verify { mockEditor.apply() }

        val capturedJson = jsonSlot.captured
        assertTrue(capturedJson.contains("First"))
        assertTrue(capturedJson.contains("Second"))
    }

    @Test
    fun `upsertSession adds new session to front`() {
        every { mockPrefs.getString("chat_sessions", null) } returns null

        val session = ChatSession("new-id", "New Chat", "2025-07-15T00:00:00Z", emptyList())
        repository.upsertSession(session)

        // Should store with the new session at front
        val jsonSlot = slot<String>()
        verify { mockEditor.putString("chat_sessions", capture(jsonSlot)) }
        verify { mockEditor.apply() }

        val saved = ChatSession.fromJson(jsonSlot.captured)
        assertEquals(1, saved.size)
        assertEquals("new-id", saved[0].id)
    }

    @Test
    fun `upsertSession updates existing session`() {
        val existing = ChatSession("1", "Old Title", "2025-01-01T00:00:00Z", emptyList())
        every { mockPrefs.getString("chat_sessions", null) } returns ChatSession.toJson(listOf(existing))

        val updated = existing.copy(title = "Updated Title")
        repository.upsertSession(updated)

        val jsonSlot = slot<String>()
        verify { mockEditor.putString("chat_sessions", capture(jsonSlot)) }
        verify { mockEditor.apply() }

        val saved = ChatSession.fromJson(jsonSlot.captured)
        assertEquals(1, saved.size)
        assertEquals("Updated Title", saved[0].title)
    }

    @Test
    fun `upsertSession adds new session while preserving existing ones`() {
        val existing = ChatSession("1", "First", "2025-01-01T00:00:00Z", emptyList())
        every { mockPrefs.getString("chat_sessions", null) } returns ChatSession.toJson(listOf(existing))

        val newSession = ChatSession("2", "Second", "2025-07-15T00:00:00Z", emptyList())
        repository.upsertSession(newSession)

        val jsonSlot = slot<String>()
        verify { mockEditor.putString("chat_sessions", capture(jsonSlot)) }

        val saved = ChatSession.fromJson(jsonSlot.captured)
        assertEquals(2, saved.size)
        assertEquals("Second", saved[0].title) // new session at front
        assertEquals("First", saved[1].title)
    }

    @Test
    fun `deleteSession removes session by id`() {
        val sessions = listOf(
            ChatSession("1", "Keep", "2025-01-01T00:00:00Z", emptyList()),
            ChatSession("2", "Delete", "2025-01-02T00:00:00Z", emptyList())
        )
        every { mockPrefs.getString("chat_sessions", null) } returns ChatSession.toJson(sessions)
        every { mockPrefs.getString("active_chat_id", null) } returns null

        repository.deleteSession("2")

        val jsonSlot = slot<String>()
        verify { mockEditor.putString("chat_sessions", capture(jsonSlot)) }

        val saved = ChatSession.fromJson(jsonSlot.captured)
        assertEquals(1, saved.size)
        assertEquals("Keep", saved[0].title)
    }

    @Test
    fun `deleteSession clears active chat if deleted session was active`() {
        val sessions = listOf(
            ChatSession("1", "Active", "2025-01-01T00:00:00Z", emptyList())
        )
        every { mockPrefs.getString("chat_sessions", null) } returns ChatSession.toJson(sessions)
        every { mockPrefs.getString("active_chat_id", null) } returns "1"

        repository.deleteSession("1")

        verify { mockEditor.remove("active_chat_id") }
    }

    @Test
    fun `deleteSession does not clear active chat if different session deleted`() {
        val sessions = listOf(
            ChatSession("1", "Active", "2025-01-01T00:00:00Z", emptyList()),
            ChatSession("2", "Other", "2025-01-02T00:00:00Z", emptyList())
        )
        every { mockPrefs.getString("chat_sessions", null) } returns ChatSession.toJson(sessions)
        every { mockPrefs.getString("active_chat_id", null) } returns "1"

        repository.deleteSession("2")

        verify(exactly = 0) { mockEditor.remove("active_chat_id") }
    }

    @Test
    fun `getActiveChatId returns null when not set`() {
        every { mockPrefs.getString("active_chat_id", null) } returns null

        assertNull(repository.getActiveChatId())
    }

    @Test
    fun `getActiveChatId returns stored id`() {
        every { mockPrefs.getString("active_chat_id", null) } returns "session-123"

        assertEquals("session-123", repository.getActiveChatId())
    }

    @Test
    fun `setActiveChatId stores the id`() {
        repository.setActiveChatId("session-456")

        verify { mockEditor.putString("active_chat_id", "session-456") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `setActiveChatId removes key when null`() {
        repository.setActiveChatId(null)

        verify { mockEditor.remove("active_chat_id") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `setActiveChatId removes key when empty`() {
        repository.setActiveChatId("")

        verify { mockEditor.remove("active_chat_id") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `loadSessions handles malformed JSON gracefully`() {
        every { mockPrefs.getString("chat_sessions", null) } returns "{not valid json}"

        val sessions = repository.loadSessions()
        assertTrue(sessions.isEmpty())
    }

    @Test
    fun `full CRUD round-trip`() {
        // Create
        val session1 = ChatSession("1", "First", "2025-01-01T00:00:00Z", emptyList())
        val session2 = ChatSession("2", "Second", "2025-01-02T00:00:00Z", emptyList())

        every { mockPrefs.getString("chat_sessions", null) } returns null
        repository.upsertSession(session1)

        // Read - mock returns what was "saved"
        every { mockPrefs.getString("chat_sessions", null) } returns ChatSession.toJson(listOf(session1))
        var loaded = repository.loadSessions()
        assertEquals(1, loaded.size)

        // Update
        every { mockPrefs.getString("chat_sessions", null) } returns ChatSession.toJson(listOf(session1))
        val updated1 = session1.copy(title = "Updated First")
        repository.upsertSession(updated1)

        // Add another
        every { mockPrefs.getString("chat_sessions", null) } returns ChatSession.toJson(listOf(updated1))
        repository.upsertSession(session2)

        // Delete
        every { mockPrefs.getString("chat_sessions", null) } returns ChatSession.toJson(listOf(updated1, session2))
        every { mockPrefs.getString("active_chat_id", null) } returns null
        repository.deleteSession("1")

        // Verify final state
        val jsonSlot = slot<String>()
        verify { mockEditor.putString("chat_sessions", capture(jsonSlot)) }
        val finalSessions = ChatSession.fromJson(jsonSlot.captured)
        assertEquals(1, finalSessions.size)
        assertEquals("Second", finalSessions[0].title)
    }
}
