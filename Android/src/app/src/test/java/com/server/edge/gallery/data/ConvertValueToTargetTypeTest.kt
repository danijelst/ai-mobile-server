package com.server.edge.gallery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [convertValueToTargetType] utility function.
 *
 * Tests all type conversions across INT, FLOAT, DOUBLE, BOOLEAN, and STRING
 * value types with various input types.
 */
class ConvertValueToTargetTypeTest {

    // ---- INT ----

    @Test
    fun `convert to INT from Int`() {
        assertEquals(42, convertValueToTargetType(42, ValueType.INT))
    }

    @Test
    fun `convert to INT from Float`() {
        assertEquals(42, convertValueToTargetType(42.7f, ValueType.INT))
    }

    @Test
    fun `convert to INT from Double`() {
        assertEquals(100, convertValueToTargetType(100.9, ValueType.INT))
    }

    @Test
    fun `convert to INT from String`() {
        assertEquals(50, convertValueToTargetType("50", ValueType.INT))
    }

    @Test
    fun `convert to INT from invalid String returns empty string`() {
        assertEquals("", convertValueToTargetType("not-a-number", ValueType.INT))
    }

    @Test
    fun `convert to INT from Boolean true`() {
        assertEquals(1, convertValueToTargetType(true, ValueType.INT))
    }

    @Test
    fun `convert to INT from Boolean false`() {
        assertEquals(0, convertValueToTargetType(false, ValueType.INT))
    }

    @Test
    fun `convert to INT from unknown type returns empty string`() {
        assertEquals("", convertValueToTargetType(listOf(1, 2, 3), ValueType.INT))
    }

    // ---- FLOAT ----

    @Test
    fun `convert to FLOAT from Int`() {
        assertEquals(42.0f, convertValueToTargetType(42, ValueType.FLOAT))
    }

    @Test
    fun `convert to FLOAT from Float`() {
        assertEquals(3.14f, convertValueToTargetType(3.14f, ValueType.FLOAT))
    }

    @Test
    fun `convert to FLOAT from Double`() {
        assertEquals(2.5f, convertValueToTargetType(2.5, ValueType.FLOAT))
    }

    @Test
    fun `convert to FLOAT from String`() {
        assertEquals(0.5f, convertValueToTargetType("0.5", ValueType.FLOAT))
    }

    @Test
    fun `convert to FLOAT from invalid String returns empty string`() {
        assertEquals("", convertValueToTargetType("abc", ValueType.FLOAT))
    }

    @Test
    fun `convert to FLOAT from Boolean true`() {
        assertEquals(1.0f, convertValueToTargetType(true, ValueType.FLOAT))
    }

    // ---- DOUBLE ----

    @Test
    fun `convert to DOUBLE from Int`() {
        assertEquals(99.0, convertValueToTargetType(99, ValueType.DOUBLE))
    }

    @Test
    fun `convert to DOUBLE from Float`() {
        assertEquals(1.5, convertValueToTargetType(1.5f, ValueType.DOUBLE) as Double, 1e-10)
    }

    @Test
    fun `convert to DOUBLE from Double`() {
        assertEquals(3.14159, convertValueToTargetType(3.14159, ValueType.DOUBLE))
    }

    @Test
    fun `convert to DOUBLE from String`() {
        assertEquals(2.718, convertValueToTargetType("2.718", ValueType.DOUBLE))
    }

    @Test
    fun `convert to DOUBLE from Boolean true`() {
        assertEquals(1.0, convertValueToTargetType(true, ValueType.DOUBLE))
    }

    // ---- BOOLEAN ----

    @Test
    fun `convert to BOOLEAN from Boolean true`() {
        assertTrue(convertValueToTargetType(true, ValueType.BOOLEAN) as Boolean)
    }

    @Test
    fun `convert to BOOLEAN from Boolean false`() {
        assertFalse(convertValueToTargetType(false, ValueType.BOOLEAN) as Boolean)
    }

    @Test
    fun `convert to BOOLEAN from Int zero`() {
        assertTrue(convertValueToTargetType(0, ValueType.BOOLEAN) as Boolean)
    }

    @Test
    fun `convert to BOOLEAN from non-zero Int`() {
        // Implementation: Int -> value == 0 → true, else false
        assertFalse(convertValueToTargetType(1, ValueType.BOOLEAN) as Boolean)
    }

    @Test
    fun `convert to BOOLEAN from non-zero Float`() {
        // Implementation: Float -> abs(value) > 1e-6
        assertFalse(convertValueToTargetType(0.0f, ValueType.BOOLEAN) as Boolean)
    }

    @Test
    fun `convert to BOOLEAN from non-empty String`() {
        assertTrue(convertValueToTargetType("hello", ValueType.BOOLEAN) as Boolean)
    }

    @Test
    fun `convert to BOOLEAN from empty String`() {
        assertFalse(convertValueToTargetType("", ValueType.BOOLEAN) as Boolean)
    }

    // ---- STRING ----

    @Test
    fun `convert to STRING from Int`() {
        assertEquals("42", convertValueToTargetType(42, ValueType.STRING))
    }

    @Test
    fun `convert to STRING from Boolean`() {
        assertEquals("true", convertValueToTargetType(true, ValueType.STRING))
    }
}
