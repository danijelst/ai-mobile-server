package com.server.edge.gallery.openai

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [OpenAiServerState].
 *
 * Tests the state management logic including:
 * - Authorization preference loading/persisting
 * - Tunnel preference loading/persisting
 * - Server running state
 * - Cloudflare and ngrok configuration persistence
 */
class OpenAiServerStateTest {

    private val mockContext: Context = mockk()
    private val mockSharedPreferences: SharedPreferences = mockk()
    private val mockEditor: SharedPreferences.Editor = mockk()

    private val prefsName = "openai_server_prefs"
    private val prefsCaptor = slot<SharedPreferences>()

    @Before
    fun setUp() {
        // Reset state before each test
        OpenAiServerState.setRunning(false, local = null, public = null)
        OpenAiServerState.setTunnelEnabled(false)

        // Setup mock SharedPreferences
        every { mockContext.applicationContext } returns mockContext
        every {
            mockContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
    }

    @After
    fun tearDown() {
        // Reset state after each test
        OpenAiServerState.setRunning(false, local = null, public = null)
        OpenAiServerState.setTunnelEnabled(false)
    }

    // ---- Running State ----

    @Test
    fun `setRunning updates isRunning and urls`() = runTest {
        assertFalse(OpenAiServerState.isRunning.first())
        assertNull(OpenAiServerState.localUrl.first())
        assertNull(OpenAiServerState.publicUrl.first())

        OpenAiServerState.setRunning(true, local = "http://192.168.1.5:8080", public = "https://example.ngrok.io")

        assertTrue(OpenAiServerState.isRunning.first())
        assertEquals("http://192.168.1.5:8080", OpenAiServerState.localUrl.first())
        assertEquals("https://example.ngrok.io", OpenAiServerState.publicUrl.first())
    }

    @Test
    fun `setRunning with null urls clears them`() = runTest {
        OpenAiServerState.setRunning(true, local = "http://localhost:8080", public = "https://tunnel.com")
        OpenAiServerState.setRunning(false)

        assertFalse(OpenAiServerState.isRunning.first())
        assertNull(OpenAiServerState.localUrl.first())
        assertNull(OpenAiServerState.publicUrl.first())
    }

    @Test
    fun `setPublicUrl updates only public url`() = runTest {
        OpenAiServerState.setRunning(true, local = "http://localhost:8080")
        OpenAiServerState.setPublicUrl("https://new-tunnel.com")

        assertEquals("https://new-tunnel.com", OpenAiServerState.publicUrl.first())
        assertEquals("http://localhost:8080", OpenAiServerState.localUrl.first())
    }

    // ---- Tunnel State ----

    @Test
    fun `setTunnelEnabled updates tunnel state and clears public url when disabled`() = runTest {
        OpenAiServerState.setPublicUrl("https://tunnel.com")
        OpenAiServerState.setTunnelEnabled(true)

        assertTrue(OpenAiServerState.isTunnelEnabled.first())

        OpenAiServerState.setTunnelEnabled(false)

        assertFalse(OpenAiServerState.isTunnelEnabled.first())
        assertNull(OpenAiServerState.publicUrl.first())
    }

    // ---- Authorization Preferences ----

    @Test
    fun `loadAuthorizationPreference loads disabled state from prefs`() {
        every { mockSharedPreferences.getBoolean("authorization_enabled", false) } returns false
        every { mockSharedPreferences.getString("authorization_token", "") } returns ""

        OpenAiServerState.loadAuthorizationPreference(mockContext)

        assertFalse(OpenAiServerState.isAuthorizationEnabled())
        assertEquals("", OpenAiServerState.getAuthorizationToken())
    }

    @Test
    fun `loadAuthorizationPreference loads enabled state with token from prefs`() {
        every { mockSharedPreferences.getBoolean("authorization_enabled", false) } returns true
        every { mockSharedPreferences.getString("authorization_token", "") } returns "my-secret-token"

        OpenAiServerState.loadAuthorizationPreference(mockContext)

        assertTrue(OpenAiServerState.isAuthorizationEnabled())
        assertEquals("my-secret-token", OpenAiServerState.getAuthorizationToken())
    }

    @Test
    fun `loadAuthorizationPreference defaults to disabled when preference missing`() {
        every { mockSharedPreferences.getBoolean("authorization_enabled", false) } returns false
        every { mockSharedPreferences.getString("authorization_token", "") } returns null

        OpenAiServerState.loadAuthorizationPreference(mockContext)

        assertFalse(OpenAiServerState.isAuthorizationEnabled())
        assertEquals("", OpenAiServerState.getAuthorizationToken())
    }

    @Test
    fun `persistAuthorizationEnabled saves enabled state and updates flow`() = runTest {
        OpenAiServerState.persistAuthorizationEnabled(mockContext, enabled = true)

        verify { mockEditor.putBoolean("authorization_enabled", true) }
        verify { mockEditor.apply() }
        assertTrue(OpenAiServerState.isAuthorizationRequired.first())
    }

    @Test
    fun `persistAuthorizationEnabled saves disabled state`() {
        OpenAiServerState.persistAuthorizationEnabled(mockContext, enabled = false)

        verify { mockEditor.putBoolean("authorization_enabled", false) }
        verify { mockEditor.apply() }
        assertFalse(OpenAiServerState.isAuthorizationEnabled())
    }

    @Test
    fun `persistAuthorizationToken trims and saves token`() {
        OpenAiServerState.persistAuthorizationToken(mockContext, "  my-token  ")

        verify { mockEditor.putString("authorization_token", "my-token") }
        verify { mockEditor.apply() }
        assertEquals("my-token", OpenAiServerState.getAuthorizationToken())
    }

    @Test
    fun `persistAuthorizationToken saves empty token`() {
        OpenAiServerState.persistAuthorizationToken(mockContext, "")

        verify { mockEditor.putString("authorization_token", "") }
        verify { mockEditor.apply() }
        assertEquals("", OpenAiServerState.getAuthorizationToken())
    }

    // ---- Tunnel Preferences ----

    @Test
    fun `loadTunnelPreference loads enabled tunnel with ngrok provider`() = runTest {
        every { mockSharedPreferences.getBoolean("tunnel_enabled", true) } returns true
        every {
            mockSharedPreferences.getString("tunnel_provider", OpenAiServerState.TUNNEL_PROVIDER_NGROK)
        } returns OpenAiServerState.TUNNEL_PROVIDER_NGROK

        OpenAiServerState.loadTunnelPreference(mockContext)

        assertTrue(OpenAiServerState.isTunnelEnabled.first())
        assertEquals(OpenAiServerState.TUNNEL_PROVIDER_NGROK, OpenAiServerState.tunnelProvider.first())
    }

    @Test
    fun `loadTunnelPreference loads disabled tunnel with cloudflare provider`() = runTest {
        every { mockSharedPreferences.getBoolean("tunnel_enabled", true) } returns false
        every {
            mockSharedPreferences.getString("tunnel_provider", OpenAiServerState.TUNNEL_PROVIDER_NGROK)
        } returns OpenAiServerState.TUNNEL_PROVIDER_CLOUDFLARE

        OpenAiServerState.loadTunnelPreference(mockContext)

        assertFalse(OpenAiServerState.isTunnelEnabled.first())
        assertEquals(OpenAiServerState.TUNNEL_PROVIDER_CLOUDFLARE, OpenAiServerState.tunnelProvider.first())
    }

    @Test
    fun `loadTunnelPreference defaults to ngrok when provider is invalid`() = runTest {
        every { mockSharedPreferences.getBoolean("tunnel_enabled", true) } returns true
        every {
            mockSharedPreferences.getString("tunnel_provider", OpenAiServerState.TUNNEL_PROVIDER_NGROK)
        } returns "invalid_provider"

        OpenAiServerState.loadTunnelPreference(mockContext)

        assertEquals(OpenAiServerState.TUNNEL_PROVIDER_NGROK, OpenAiServerState.tunnelProvider.first())
    }

    @Test
    fun `persistTunnelEnabled saves enabled and updates state`() = runTest {
        OpenAiServerState.persistTunnelEnabled(mockContext, enabled = true)

        verify { mockEditor.putBoolean("tunnel_enabled", true) }
        verify { mockEditor.apply() }
        assertTrue(OpenAiServerState.isTunnelEnabled.first())
    }

    @Test
    fun `persistTunnelEnabled saves disabled and clears public url`() = runTest {
        OpenAiServerState.setPublicUrl("https://tunnel.com")

        OpenAiServerState.persistTunnelEnabled(mockContext, enabled = false)

        verify { mockEditor.putBoolean("tunnel_enabled", false) }
        verify { mockEditor.apply() }
        assertFalse(OpenAiServerState.isTunnelEnabled.first())
        assertNull(OpenAiServerState.publicUrl.first())
    }

    @Test
    fun `persistTunnelProvider saves ngrok and updates flow`() = runTest {
        OpenAiServerState.persistTunnelProvider(mockContext, OpenAiServerState.TUNNEL_PROVIDER_NGROK)

        verify { mockEditor.putString("tunnel_provider", OpenAiServerState.TUNNEL_PROVIDER_NGROK) }
        verify { mockEditor.apply() }
        assertEquals(OpenAiServerState.TUNNEL_PROVIDER_NGROK, OpenAiServerState.tunnelProvider.first())
    }

    @Test
    fun `persistTunnelProvider normalizes alternative values to cloudflare`() = runTest {
        OpenAiServerState.persistTunnelProvider(mockContext, "some_other_provider")

        verify {
            mockEditor.putString(
                "tunnel_provider",
                OpenAiServerState.TUNNEL_PROVIDER_CLOUDFLARE
            )
        }
        assertEquals(
            OpenAiServerState.TUNNEL_PROVIDER_CLOUDFLARE,
            OpenAiServerState.tunnelProvider.first()
        )
    }

    @Test
    fun `persistTunnelProvider preserves ngrok when explicitly set`() = runTest {
        OpenAiServerState.persistTunnelProvider(mockContext, OpenAiServerState.TUNNEL_PROVIDER_NGROK)

        verify {
            mockEditor.putString("tunnel_provider", OpenAiServerState.TUNNEL_PROVIDER_NGROK)
        }
        assertEquals(OpenAiServerState.TUNNEL_PROVIDER_NGROK, OpenAiServerState.tunnelProvider.first())
    }

    // ---- Cloudflare Config ----

    @Test
    fun `persistCloudflareTunnelConfig saves token and url`() {
        OpenAiServerState.persistCloudflareTunnelConfig(
            mockContext,
            tunnelToken = "cf-token-123",
            publicUrl = "https://api.example.com"
        )

        verify { mockEditor.putString("cloudflare_tunnel_token", "cf-token-123") }
        verify { mockEditor.putString("cloudflare_public_url", "https://api.example.com") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `persistCloudflareTunnelConfig trims values and removes trailing slash`() {
        OpenAiServerState.persistCloudflareTunnelConfig(
            mockContext,
            tunnelToken = "  cf-token  ",
            publicUrl = "https://api.example.com/"
        )

        verify { mockEditor.putString("cloudflare_tunnel_token", "cf-token") }
        verify { mockEditor.putString("cloudflare_public_url", "https://api.example.com") }
    }

    @Test
    fun `cloudflareTunnelToken reads token from prefs`() {
        every { mockSharedPreferences.getString("cloudflare_tunnel_token", "") } returns "stored-token"

        val token = OpenAiServerState.cloudflareTunnelToken(mockContext)

        assertEquals("stored-token", token)
    }

    @Test
    fun `cloudflareTunnelToken returns empty string when not set`() {
        every { mockSharedPreferences.getString("cloudflare_tunnel_token", "") } returns null

        val token = OpenAiServerState.cloudflareTunnelToken(mockContext)

        assertEquals("", token)
    }

    @Test
    fun `cloudflarePublicUrl reads url from prefs`() {
        every { mockSharedPreferences.getString("cloudflare_public_url", "") } returns "https://public.url"

        val url = OpenAiServerState.cloudflarePublicUrl(mockContext)

        assertEquals("https://public.url", url)
    }

    @Test
    fun `cloudflarePublicUrl returns empty string when not set`() {
        every { mockSharedPreferences.getString("cloudflare_public_url", "") } returns null

        val url = OpenAiServerState.cloudflarePublicUrl(mockContext)

        assertEquals("", url)
    }

    // ---- ngrok Config ----

    @Test
    fun `persistNgrokConfig saves auth token and domain`() {
        OpenAiServerState.persistNgrokConfig(
            mockContext,
            authToken = "ngrok-auth-123",
            domain = "my-domain.ngrok.io"
        )

        verify { mockEditor.putString("ngrok_auth_token", "ngrok-auth-123") }
        verify { mockEditor.putString("ngrok_domain", "my-domain.ngrok.io") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `persistNgrokConfig trims values and removes trailing slash`() {
        OpenAiServerState.persistNgrokConfig(
            mockContext,
            authToken = "  token  ",
            domain = "my-domain.ngrok.io/"
        )

        verify { mockEditor.putString("ngrok_auth_token", "token") }
        verify { mockEditor.putString("ngrok_domain", "my-domain.ngrok.io") }
    }

    @Test
    fun `ngrokAuthToken reads token from prefs`() {
        every { mockSharedPreferences.getString("ngrok_auth_token", "") } returns "stored-ngrok-token"

        val token = OpenAiServerState.ngrokAuthToken(mockContext)

        assertEquals("stored-ngrok-token", token)
    }

    @Test
    fun `ngrokAuthToken returns empty string when not set`() {
        every { mockSharedPreferences.getString("ngrok_auth_token", "") } returns null

        val token = OpenAiServerState.ngrokAuthToken(mockContext)

        assertEquals("", token)
    }

    @Test
    fun `ngrokDomain reads domain from prefs`() {
        every { mockSharedPreferences.getString("ngrok_domain", "") } returns "my-domain.ngrok.io"

        val domain = OpenAiServerState.ngrokDomain(mockContext)

        assertEquals("my-domain.ngrok.io", domain)
    }

    @Test
    fun `ngrokDomain returns empty string when not set`() {
        every { mockSharedPreferences.getString("ngrok_domain", "") } returns null

        val domain = OpenAiServerState.ngrokDomain(mockContext)

        assertEquals("", domain)
    }

    // ---- requestOpenServerScreen ----

    @Test
    fun `requestOpenServerScreen updates timestamp`() = runTest {
        val initialTimestamp = OpenAiServerState.openServerScreenRequest.first()

        OpenAiServerState.requestOpenServerScreen()

        val newTimestamp = OpenAiServerState.openServerScreenRequest.first()
        assertTrue("Timestamp should increase", newTimestamp >= initialTimestamp)
        assertNotNull("Timestamp should be positive", newTimestamp)
        assertTrue("Timestamp should be positive", newTimestamp > 0)
    }

    // ---- Constants ----

    @Test
    fun `tunnel provider constants are correct`() {
        assertEquals("cloudflare", OpenAiServerState.TUNNEL_PROVIDER_CLOUDFLARE)
        assertEquals("ngrok", OpenAiServerState.TUNNEL_PROVIDER_NGROK)
    }
}
