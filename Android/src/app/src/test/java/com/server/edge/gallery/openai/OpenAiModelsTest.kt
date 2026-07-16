package com.server.edge.gallery.openai

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for OpenAI API model data classes serialization.
 *
 * Tests round-trip JSON serialization/deserialization for all API request
 * and response models defined in [OpenAiModels].
 */
class OpenAiModelsTest {

    private val json = Json {
        prettyPrint = false
        isLenient = true
        ignoreUnknownKeys = true
    }

    // ---- Chat Completion Request ----

    @Test
    fun `ChatCompletionRequest serialization round-trip`() {
        val request = ChatCompletionRequest(
            model = "gemma-2-2b-it",
            messages = listOf(
                ChatMessage(role = "system", content = "You are helpful."),
                ChatMessage(role = "user", content = "Hello!")
            ),
            temperature = 0.7f,
            stream = false,
        )

        val encoded = json.encodeToString(request)
        val decoded = json.decodeFromString<ChatCompletionRequest>(encoded)

        assertEquals(request.model, decoded.model)
        assertEquals(request.messages.size, decoded.messages.size)
        assertEquals(request.messages[0].role, decoded.messages[0].role)
        assertEquals(request.messages[0].content, decoded.messages[0].content)
        assertEquals(request.messages[1].role, decoded.messages[1].role)
        assertEquals(request.messages[1].content, decoded.messages[1].content)
        assertEquals(request.temperature, decoded.temperature)
        assertEquals(request.stream, decoded.stream)
    }

    @Test
    fun `ChatCompletionRequest with all fields`() {
        val request = ChatCompletionRequest(
            model = "test-model",
            messages = listOf(ChatMessage(role = "user", content = "Hi")),
            temperature = 0.5f,
            top_p = 0.9f,
            top_k = 40,
            max_tokens = 2048,
            stream = true,
        )

        val encoded = json.encodeToString(request)
        val decoded = json.decodeFromString<ChatCompletionRequest>(encoded)

        assertEquals(0.5f, decoded.temperature)
        assertEquals(0.9f, decoded.top_p)
        assertEquals(40, decoded.top_k)
        assertEquals(2048, decoded.max_tokens)
        assertTrue(decoded.stream)
    }

    @Test
    fun `ChatCompletionRequest with tools`() {
        val request = ChatCompletionRequest(
            model = "test-model",
            messages = listOf(ChatMessage(role = "user", content = "Weather?")),
            tools = listOf(
                OpenAiTool(
                    type = "function",
                    function = OpenAiToolFunction(
                        name = "get_weather",
                        description = "Get weather for a city",
                        parameters = OpenAiToolParameters(
                            type = "object",
                            properties = mapOf(
                                "city" to OpenAiToolProperty(
                                    type = "string",
                                    description = "City name"
                                )
                            ),
                            required = listOf("city")
                        )
                    )
                )
            )
        )

        val encoded = json.encodeToString(request)
        val decoded = json.decodeFromString<ChatCompletionRequest>(encoded)

        assertNotNull(decoded.tools)
        assertEquals(1, decoded.tools!!.size)
        assertEquals("function", decoded.tools!![0].type)
        assertEquals("get_weather", decoded.tools!![0].function.name)
        assertEquals("string", decoded.tools!![0].function.parameters?.properties?.get("city")?.type)
        assertTrue(decoded.tools!![0].function.parameters?.required?.contains("city") == true)
    }

    @Test
    fun `ChatCompletionRequest defaults are correct`() {
        val request = ChatCompletionRequest(
            model = "test",
            messages = listOf(ChatMessage(role = "user", content = "Hi"))
        )

        assertNull(request.temperature)
        assertNull(request.top_p)
        assertNull(request.top_k)
        assertNull(request.max_tokens)
        assertEquals(false, request.stream)
        assertNull(request.tools)
    }

    @Test
    fun `ChatCompletionRequest deserializes from JSON string`() {
        val jsonString = """
            {
                "model": "gemma-3-12b-it",
                "messages": [
                    {"role": "user", "content": "Hello"}
                ]
            }
        """.trimIndent()

        val request = json.decodeFromString<ChatCompletionRequest>(jsonString)

        assertEquals("gemma-3-12b-it", request.model)
        assertEquals(1, request.messages.size)
        assertEquals("user", request.messages[0].role)
        assertEquals("Hello", request.messages[0].content)
    }

    // ---- Chat Completion Response ----

    @Test
    fun `ChatCompletionResponse serialization round-trip`() {
        val response = ChatCompletionResponse(
            id = "chatcmpl-123",
            created = 1234567890L,
            model = "gemma-2-2b-it",
            choices = listOf(
                ChatChoice(
                    index = 0,
                    message = ChatMessage(role = "assistant", content = "Hello! How can I help?"),
                    finish_reason = "stop"
                )
            ),
            usage = Usage(
                prompt_tokens = 10,
                completion_tokens = 20,
                total_tokens = 30
            )
        )

        val encoded = json.encodeToString(response)
        val decoded = json.decodeFromString<ChatCompletionResponse>(encoded)

        assertEquals(response.id, decoded.id)
        assertEquals(response.created, decoded.created)
        assertEquals(response.model, decoded.model)
        assertEquals(response.choices.size, decoded.choices.size)
        assertEquals(response.choices[0].message.content, decoded.choices[0].message.content)
        assertEquals(response.choices[0].finish_reason, decoded.choices[0].finish_reason)
        assertEquals(response.usage?.total_tokens, decoded.usage?.total_tokens)
    }

    @Test
    fun `ChatCompletionResponse without usage`() {
        val response = ChatCompletionResponse(
            id = "chatcmpl-456",
            created = 1234567890L,
            model = "test-model",
            choices = listOf(
                ChatChoice(
                    index = 0,
                    message = ChatMessage(role = "assistant", content = "OK")
                )
            )
        )

        val encoded = json.encodeToString(response)
        val decoded = json.decodeFromString<ChatCompletionResponse>(encoded)

        assertNull(decoded.usage)
    }

    @Test
    fun `ChatCompletionResponse defaults are correct`() {
        val response = ChatCompletionResponse(
            id = "id",
            created = 100L,
            model = "m",
            choices = listOf(ChatChoice(0, ChatMessage("a", "b")))
        )

        assertEquals("chat.completion", response.`object`)
        assertNull(response.usage)
    }

    @Test
    fun `ChatCompletionResponse deserializes from JSON string`() {
        val jsonString = """
            {
                "id": "chatcmpl-abc",
                "created": 1700000000,
                "model": "gemma-3-12b-it",
                "choices": [
                    {
                        "index": 0,
                        "message": {
                            "role": "assistant",
                            "content": "Sure, here you go!"
                        },
                        "finish_reason": "stop"
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<ChatCompletionResponse>(jsonString)

        assertEquals("chatcmpl-abc", response.id)
        assertEquals(1, response.choices.size)
        assertEquals("assistant", response.choices[0].message.role)
        assertEquals("Sure, here you go!", response.choices[0].message.content)
    }

    // ---- Streaming Chunks ----

    @Test
    fun `ChatCompletionChunk serialization round-trip`() {
        val chunk = ChatCompletionChunk(
            id = "chatcmpl-123",
            created = 1234567890L,
            model = "test-model",
            choices = listOf(
                ChatChunkChoice(
                    index = 0,
                    delta = ChatDelta(content = "Hello"),
                    finish_reason = null
                )
            )
        )

        val encoded = json.encodeToString(chunk)
        val decoded = json.decodeFromString<ChatCompletionChunk>(encoded)

        assertEquals(chunk.id, decoded.id)
        assertEquals(chunk.model, decoded.model)
        assertEquals("chat.completion.chunk", decoded.`object`)
        assertEquals("Hello", decoded.choices[0].delta.content)
    }

    @Test
    fun `ChatDelta with role and content`() {
        val delta = ChatDelta(role = "assistant", content = "Hi")

        val encoded = json.encodeToString(delta)
        val decoded = json.decodeFromString<ChatDelta>(encoded)

        assertEquals("assistant", decoded.role)
        assertEquals("Hi", decoded.content)
    }

    @Test
    fun `ChatDelta defaults are null`() {
        val delta = ChatDelta()

        assertNull(delta.role)
        assertNull(delta.content)
    }

    // ---- Models List ----

    @Test
    fun `ModelsListResponse serialization round-trip`() {
        val response = ModelsListResponse(
            data = listOf(
                ModelData(id = "gemma-2-2b-it", created = 1000L),
                ModelData(id = "gemma-3-12b-it", created = 2000L)
            )
        )

        val encoded = json.encodeToString(response)
        val decoded = json.decodeFromString<ModelsListResponse>(encoded)

        assertEquals(2, decoded.data.size)
        assertEquals("gemma-2-2b-it", decoded.data[0].id)
        assertEquals(1000L, decoded.data[0].created)
        assertEquals("list", decoded.`object`)
    }

    @Test
    fun `ModelData defaults are correct`() {
        val modelData = ModelData(id = "test-model")

        assertEquals("model", modelData.`object`)
        assertEquals(0L, modelData.created)
        assertEquals("local", modelData.owned_by)
    }

    @Test
    fun `ModelData deserializes from JSON string`() {
        val jsonString = """
            {
                "id": "gemma-2-2b-it",
                "object": "model",
                "created": 1700000000,
                "owned_by": "google"
            }
        """.trimIndent()

        val modelData = json.decodeFromString<ModelData>(jsonString)

        assertEquals("gemma-2-2b-it", modelData.id)
        assertEquals(1700000000L, modelData.created)
        assertEquals("google", modelData.owned_by)
    }

    // ---- Completion Request ----

    @Test
    fun `CompletionRequest serialization round-trip`() {
        val request = CompletionRequest(
            model = "gemma-2-2b-it",
            prompt = "Once upon a time",
            temperature = 0.8f,
            max_tokens = 100,
            stream = false
        )

        val encoded = json.encodeToString(request)
        val decoded = json.decodeFromString<CompletionRequest>(encoded)

        assertEquals(request.model, decoded.model)
        assertEquals(request.prompt, decoded.prompt)
        assertEquals(request.temperature, decoded.temperature)
        assertEquals(request.max_tokens, decoded.max_tokens)
    }

    @Test
    fun `CompletionRequest defaults are correct`() {
        val request = CompletionRequest(
            model = "test",
            prompt = "Hello"
        )

        assertNull(request.temperature)
        assertNull(request.top_p)
        assertNull(request.top_k)
        assertNull(request.max_tokens)
        assertEquals(false, request.stream)
    }

    // ---- Completion Response ----

    @Test
    fun `CompletionResponse serialization round-trip`() {
        val response = CompletionResponse(
            id = "cmpl-123",
            created = 1234567890L,
            model = "gemma-2-2b-it",
            choices = listOf(
                CompletionChoice(
                    index = 0,
                    text = "there was a brave knight",
                    finish_reason = "stop"
                )
            )
        )

        val encoded = json.encodeToString(response)
        val decoded = json.decodeFromString<CompletionResponse>(encoded)

        assertEquals(response.id, decoded.id)
        assertEquals(response.model, decoded.model)
        assertEquals("text_completion", decoded.`object`)
        assertEquals(response.choices[0].text, decoded.choices[0].text)
        assertEquals(response.choices[0].finish_reason, decoded.choices[0].finish_reason)
    }

    // ---- Completion Streaming Chunks ----

    @Test
    fun `CompletionChunk serialization round-trip`() {
        val chunk = CompletionChunk(
            id = "cmpl-456",
            created = 1234567890L,
            model = "test-model",
            choices = listOf(
                CompletionChunkChoice(
                    index = 0,
                    text = "continuing"
                )
            )
        )

        val encoded = json.encodeToString(chunk)
        val decoded = json.decodeFromString<CompletionChunk>(encoded)

        assertEquals(chunk.id, decoded.id)
        assertEquals("text_completion", decoded.`object`)
        assertEquals("continuing", decoded.choices[0].text)
    }

    // ---- Tool definitions ----

    @Test
    fun `OpenAiTool serialization round-trip`() {
        val tool = OpenAiTool(
            type = "function",
            function = OpenAiToolFunction(
                name = "get_time",
                description = "Get current time"
            )
        )

        val encoded = json.encodeToString(tool)
        val decoded = json.decodeFromString<OpenAiTool>(encoded)

        assertEquals(tool.type, decoded.type)
        assertEquals("get_time", decoded.function.name)
        assertEquals("Get current time", decoded.function.description)
    }

    @Test
    fun `OpenAiTool with null description and null parameters`() {
        val tool = OpenAiTool(
            function = OpenAiToolFunction(name = "test")
        )

        assertNull(tool.function.description)
        assertNull(tool.function.parameters)
    }

    // ---- Edge cases ----

    @Test
    fun `empty messages list is valid`() {
        val request = ChatCompletionRequest(
            model = "test",
            messages = emptyList()
        )

        val encoded = json.encodeToString(request)
        val decoded = json.decodeFromString<ChatCompletionRequest>(encoded)

        assertTrue(decoded.messages.isEmpty())
    }

    @Test
    fun `empty choices list is valid`() {
        val response = ChatCompletionResponse(
            id = "id",
            created = 0L,
            model = "m",
            choices = emptyList()
        )

        val encoded = json.encodeToString(response)
        val decoded = json.decodeFromString<ChatCompletionResponse>(encoded)

        assertTrue(decoded.choices.isEmpty())
    }

    @Test
    fun `unknown fields are ignored`() {
        val jsonString = """
            {
                "model": "test",
                "messages": [{"role": "user", "content": "Hi"}],
                "unknown_field": "should be ignored"
            }
        """.trimIndent()

        val request = json.decodeFromString<ChatCompletionRequest>(jsonString)

        assertEquals("test", request.model)
        assertEquals(1, request.messages.size)
    }
}
