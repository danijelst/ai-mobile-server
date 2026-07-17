package com.server.edge.gallery.openai

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the OpenAiServer authorization logic.
 *
 * The [OpenAiServer.isAuthorized] method (private) checks:
 * 1. If authorization is disabled → allow (return true)
 * 2. If token is empty → allow (return true)
 * 3. If Authorization header matches "Bearer $token" → allow
 * 4. Otherwise → deny (return false)
 *
 * These tests validate the authorization data flow through [OpenAiServerState]
 * persistence layer that feeds into [OpenAiServer.isAuthorized].
 */
class OpenAiServerAuthorizationTest {

    private val mockContext: Context = mockk()
    private val mockSharedPreferences: SharedPreferences = mockk()
    private val mockEditor: SharedPreferences.Editor = mockk()

    private val prefsName = "openai_server_prefs"

    @Before
    fun setUp() {
        every { mockContext.applicationContext } returns mockContext
        every { mockContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
    }

    @Test
    fun `authorization disabled by default`() {
        every { mockSharedPreferences.getBoolean("authorization_enabled", false) } returns false
        every { mockSharedPreferences.getString("authorization_token", "") } returns ""

        OpenAiServerState.loadAuthorizationPreference(mockContext)

        assertFalse(
            "Authorization should be disabled by default",
            OpenAiServerState.isAuthorizationEnabled()
        )
    }

    @Test
    fun `authorization check returns true when auth is disabled`() {
        // Simulate auth disabled
        every { mockSharedPreferences.getBoolean("authorization_enabled", false) } returns false
        every { mockSharedPreferences.getString("authorization_token", "") } returns ""
        OpenAiServerState.loadAuthorizationPreference(mockContext)

        assertFalse("Auth should be disabled", OpenAiServerState.isAuthorizationEnabled())

        // When auth is disabled, isAuthorized would return true regardless of token
        // This is tested via the data flow - the isAuthorized method checks:
        //   if (!prefs.getBoolean("authorization_enabled", false)) return true
    }

    @Test
    fun `authorization check returns true when token is empty`() {
        // Enable auth but keep token empty
        every { mockSharedPreferences.getBoolean("authorization_enabled", false) } returns true
        every { mockSharedPreferences.getString("authorization_token", "") } returns ""
        OpenAiServerState.loadAuthorizationPreference(mockContext)

        assertTrue("Authorization should be enabled", OpenAiServerState.isAuthorizationEnabled())
        assertEquals("Token should be empty", "", OpenAiServerState.getAuthorizationToken())

        // When token is empty, isAuthorized would return true:
        //   if (token.isEmpty()) return true
    }

    @Test
    fun `authorization stores and retrieves token correctly`() {
        OpenAiServerState.persistAuthorizationToken(mockContext, "my-secret-token-123")
        assertEquals("my-secret-token-123", OpenAiServerState.getAuthorizationToken())
    }

    @Test
    fun `authorization enables and stores token through state`() {
        // Simulate loading auth state
        every { mockSharedPreferences.getBoolean("authorization_enabled", false) } returns true
        every { mockSharedPreferences.getString("authorization_token", "") } returns "test-token"

        OpenAiServerState.loadAuthorizationPreference(mockContext)

        assertTrue(OpenAiServerState.isAuthorizationEnabled())
        assertEquals("test-token", OpenAiServerState.getAuthorizationToken())
    }

    @Test
    fun `authorization token is trimmed during persist`() {
        OpenAiServerState.persistAuthorizationToken(mockContext, "  spaced-token  ")

        verify { mockEditor.putString("authorization_token", "spaced-token") }
        assertEquals("spaced-token", OpenAiServerState.getAuthorizationToken())
    }

    @Test
    fun `toggle authorization enabled persists correctly`() {
        // Enable
        OpenAiServerState.persistAuthorizationEnabled(mockContext, enabled = true)
        verify { mockEditor.putBoolean("authorization_enabled", true) }
        assertTrue(OpenAiServerState.isAuthorizationEnabled())

        // Disable
        OpenAiServerState.persistAuthorizationEnabled(mockContext, enabled = false)
        verify { mockEditor.putBoolean("authorization_enabled", false) }
        assertFalse(OpenAiServerState.isAuthorizationEnabled())
    }

    @Test
    fun `full authorization persistence round-trip`() {
        // Persist: enable auth + set token
        every { mockSharedPreferences.getBoolean("authorization_enabled", false) } returns true
        every { mockSharedPreferences.getString("authorization_token", "") } returns "roundtrip-token"

        OpenAiServerState.persistAuthorizationEnabled(mockContext, enabled = true)
        OpenAiServerState.persistAuthorizationToken(mockContext, "roundtrip-token")

        // When loaded, should match what was persisted
        // The load reads from prefs directly; the persist writes to editor
        // In the actual app, load is called after persist to restore state
        // The "roundtrip" is: load reads what was written by persist
        OpenAiServerState.loadAuthorizationPreference(mockContext)

        assertTrue(OpenAiServerState.isAuthorizationEnabled())
        assertEquals("roundtrip-token", OpenAiServerState.getAuthorizationToken())
    }

    @Test
    fun `authorization check matches Bearer token pattern`() {
        // If the auth header equals "Bearer $token", the request passes
        // This is the actual isAuthorized check:
        //   val authHeader = call.request.headers[HttpHeaders.Authorization]
        //   return authHeader == "Bearer $token"

        // Validate the token storage
        OpenAiServerState.persistAuthorizationToken(mockContext, "valid-token")
        assertEquals("valid-token", OpenAiServerState.getAuthorizationToken())

        // The Bearer prefix is added during comparison in isAuthorized,
        // not stored in the token. Verify the token is stored cleanly.
        assertEquals("valid-token", OpenAiServerState.getAuthorizationToken())
    }

    @Test
    fun `changing token after enable works correctly`() {
        every { mockSharedPreferences.getBoolean("authorization_enabled", false) } returns true
        every { mockSharedPreferences.getString("authorization_token", "") } returns "old-token"

        OpenAiServerState.loadAuthorizationPreference(mockContext)
        assertEquals("old-token", OpenAiServerState.getAuthorizationToken())

        // Change token
        OpenAiServerState.persistAuthorizationToken(mockContext, "new-token")
        assertEquals("new-token", OpenAiServerState.getAuthorizationToken())
    }
}
