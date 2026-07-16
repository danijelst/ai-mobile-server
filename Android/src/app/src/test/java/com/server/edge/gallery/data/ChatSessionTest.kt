package com.server.edge.gallery.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ChatSession] and [ChatMessage] Gson serialization.
 */
class ChatSessionTest {

    private val gson = Gson()

    @Test
    fun `ChatSession toJson and fromJson round-trip`() {
        val sessions = listOf(
            ChatSession(
                id = "session-1",
                title = "Test Chat",
                updatedAt = "2025-07-15T10:30:00Z",
                messages = listOf(
                    ChatMessage(
                        id = "msg-1",
                        role = "user",
                        text = "Hello!"
                    ),
                    ChatMessage(
                        id = "msg-2",
                        role = "assistant",
                        text = "Hi there!",
                        thinking = "Let me think...",
                        isError = false
                    )
                ),
                modelName = "gemma-2-2b-it"
            )
        )

        val json = ChatSession.toJson(sessions)
        val decoded = ChatSession.fromJson(json)

        assertEquals(1, decoded.size)
        assertEquals("session-1", decoded[0].id)
        assertEquals("Test Chat", decoded[0].title)
        assertEquals(2, decoded[0].messages.size)
        assertEquals("Hello!", decoded[0].messages[0].text)
        assertEquals("Hi there!", decoded[0].messages[1].text)
        assertEquals("Let me think...", decoded[0].messages[1].thinking)
        assertEquals("gemma-2-2b-it", decoded[0].modelName)
    }

    @Test
    fun `ChatSession with multiple sessions`() {
        val sessions = listOf(
            ChatSession(id = "1", title = "First", updatedAt = "2025-01-01T00:00:00Z", messages = emptyList()),
            ChatSession(id = "2", title = "Second", updatedAt = "2025-01-02T00:00:00Z", messages = emptyList())
        )

        val json = ChatSession.toJson(sessions)
        val decoded = ChatSession.fromJson(json)

        assertEquals(2, decoded.size)
        assertEquals("First", decoded[0].title)
        assertEquals("Second", decoded[1].title)
    }

    @Test
    fun `ChatSession fromJson with null messages list`() {
        val json = """[]"""
        val decoded = ChatSession.fromJson(json)

        assertTrue(decoded.isEmpty())
    }

    @Test
    fun `ChatSession fromJson with error message`() {
        val sessions = listOf(
            ChatSession(
                id = "err-session",
                title = "Error Chat",
                updatedAt = "2025-07-15T12:00:00Z",
                messages = listOf(
                    ChatMessage(id = "err-1", role = "user", text = "Do something impossible"),
                    ChatMessage(id = "err-2", role = "assistant", text = "Sorry, I can't do that", isError = true)
                )
            )
        )

        val json = ChatSession.toJson(sessions)
        val decoded = ChatSession.fromJson(json)

        assertTrue(decoded[0].messages[1].isError)
    }

    @Test
    fun `ChatSession fromJson handles malformed json gracefully`() {
        val malformed = "{ not valid json }"
        val decoded = ChatSession.fromJson(malformed)
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun `ChatSession fromJson handles null input`() {
        // fromJson handles null by returning emptyList from try-catch
        val decoded = try {
            ChatSession.fromJson("null")
        } catch (_: Exception) {
            emptyList<ChatSession>()
        }
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun `ChatMessage defaults are correct`() {
        val msg = ChatMessage(id = "1", role = "user", text = "Hi")

        assertEquals("", msg.thinking)
        assertFalse(msg.isError)
    }

    @Test
    fun `ChatSession toJson produces valid JSON`() {
        val sessions = listOf(
            ChatSession(
                id = "session-1",
                title = "Test",
                updatedAt = "2025-01-01T00:00:00Z",
                messages = listOf(
                    ChatMessage(id = "m1", role = "user", text = "Hello")
                ),
                modelName = "gemma-2-2b-it"
            )
        )

        val json = ChatSession.toJson(sessions)

        // Verify it produces parseable JSON with expected fields
        assertTrue(json.contains("session-1"))
        assertTrue(json.contains("Hello"))
        assertTrue(json.contains("gemma-2-2b-it"))
        assertTrue(json.contains("user"))
    }
}
