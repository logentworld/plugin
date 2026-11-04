package dev.logent.plugin

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginApiTest {
    @Test
    fun `도구 스키마는 required와 속성을 보존해요`() {
        val spec = toolSpec("demo", "설명", required = listOf("text")) {
            stringProperty("text", "본문")
            integerProperty("limit", "개수", 1, 5)
            booleanProperty("raw", "원본")
        }
        val parameters = spec.function.parameters
        assertEquals("object", parameters["type"]?.jsonPrimitive?.content)
        assertTrue(parameters["required"].toString().contains("text"))
        val properties = parameters["properties"]!!.jsonObject
        assertEquals(1, properties["limit"]!!.jsonObject["minimum"]!!.jsonPrimitive.int)
        assertEquals(true, properties["raw"]!!.jsonObject["type"]!!.jsonPrimitive.content == "boolean")
    }

    @Test
    fun `셸 인용은 작은따옴표를 안전하게 닫아요`() {
        assertEquals("'a'\\''b'", shellQuote("a'b"))
        assertEquals("'a b'", shellQuote("a b"))
    }

    @Test
    fun `도구 결과의 오류와 이미지를 보존해요`() {
        val image = ToolImage("AAAA", "image/png")
        val result = ToolResult("본문", listOf(image), isError = true)
        assertTrue(result.isError)
        assertEquals(image, result.images.single())
    }

    @Test
    fun `json 도우미는 타입이 다른 값을 거부해요`() {
        val obj = kotlinx.serialization.json.buildJsonObject {
            put("text", kotlinx.serialization.json.JsonPrimitive("값"))
            put("count", kotlinx.serialization.json.JsonPrimitive(3))
            put("flag", kotlinx.serialization.json.JsonPrimitive(true))
        }
        assertEquals("값", obj.text("text"))
        assertEquals(3, obj.integer("count"))
        assertEquals(true, obj.boolean("flag"))
        assertEquals(null, obj.text("count"))
        assertTrue(obj.boolean("flag") == true)
    }
}
