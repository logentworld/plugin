package com.sleepysoong.logent.plugin

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
    @Test
    fun `슬라이더와 선택 설정은 범위와 항목을 검증해요`() {
        val slider = PluginSetting(
            key = "limit", title = "한도", kind = PluginSettingKind.SLIDER,
            defaultValue = "5", minValue = 1, maxValue = 10, step = 2,
        )
        assertEquals(10, slider.maxValue)
        val select = PluginSetting(
            key = "mode", title = "모드", kind = PluginSettingKind.SELECT,
            defaultValue = "fast", options = listOf("fast", "deep"),
        )
        assertEquals(listOf("fast", "deep"), select.options)
        // 3.0 계약의 다섯 인자 생성자는 그대로 동작해야 해요.
        val legacy = PluginSetting("old", "예전", "설명", PluginSettingKind.TEXT, "값")
        assertEquals("값", legacy.defaultValue)
        assertTrue(runCatching {
            PluginSetting("bad", "나쁨", kind = PluginSettingKind.SLIDER, minValue = 5, maxValue = 1)
        }.isFailure)
        assertTrue(runCatching {
            PluginSetting("bad", "나쁨", kind = PluginSettingKind.SELECT, options = listOf("a", "a"))
        }.isFailure)
    }

    @Test
    fun `hooks mcp skill contract`() {
        val server = PluginMcpServer("demo", "https://example.com/mcp", mapOf("X-Test" to "1"))
        assertEquals("demo", server.name)
        val skill = PluginSkill("demo", "설명", read = { Result.success("본문") })
        assertEquals("본문", kotlinx.coroutines.runBlocking { skill.read!!(PluginSkillReadContext(FakeHost())) }.getOrThrow())
        val command = PluginSlashCommand("demo", "", "설명", buildPrompt = { "프롬프트" })
        val result = kotlinx.coroutines.runBlocking { command.execute("인자", object : PluginSlashContext {
            override val sessionId = "s"
            override val workspace = "~"
            override val host = FakeHost()
            override suspend fun reply(text: String, isError: Boolean) = Unit
            override suspend fun runTool(name: String, argumentsJson: String) = ToolResult("")
        }) }
        assertEquals("프롬프트", result.prompt)
    }

    private class FakeHost : PluginHost {
        override val appVersion = "test"
        override val apkPath = "/tmp/app.apk"
        override suspend fun shell(command: String, timeoutSeconds: Int) = Result.success(ProcessResult(0, "", false))
        override suspend fun ensureDesktop() = Result.success(Unit)
        override suspend fun readAsset(path: String): Result<ByteArray> = Result.failure(IllegalStateException("없음"))
        override fun progress(message: String) = Unit
    }

}
