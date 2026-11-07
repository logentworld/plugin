package com.sleepysoong.logent.plugin

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** 도구 스키마를 짧게 선언하기 위한 공통 빌더. */
fun toolSpec(
    name: String,
    description: String,
    required: List<String> = emptyList(),
    properties: JsonObjectBuilder.() -> Unit,
): ToolSpec = ToolSpec(
    function = FunctionSpec(
        name = name,
        description = description,
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties", properties)
            if (required.isNotEmpty()) putJsonArray("required") { required.forEach(::add) }
        },
    ),
)

fun JsonObjectBuilder.stringProperty(name: String, description: String, enum: List<String>? = null) {
    putJsonObject(name) {
        put("type", "string")
        put("description", description)
        if (enum != null) putJsonArray("enum") { enum.forEach(::add) }
    }
}

fun JsonObjectBuilder.integerProperty(name: String, description: String, minimum: Int? = null, maximum: Int? = null) {
    putJsonObject(name) {
        put("type", "integer")
        put("description", description)
        if (minimum != null) put("minimum", minimum)
        if (maximum != null) put("maximum", maximum)
    }
}

fun JsonObjectBuilder.booleanProperty(name: String, description: String) {
    putJsonObject(name) {
        put("type", "boolean")
        put("description", description)
    }
}

fun JsonObjectBuilder.stringArrayProperty(name: String, description: String) {
    putJsonObject(name) {
        put("type", "array")
        put("description", description)
        putJsonObject("items") { put("type", "string") }
    }
}

fun JsonObjectBuilder.objectArrayProperty(
    name: String,
    description: String,
    required: List<String> = emptyList(),
    properties: JsonObjectBuilder.() -> Unit,
) {
    putJsonObject(name) {
        put("type", "array")
        put("description", description)
        putJsonObject("items") {
            put("type", "object")
            putJsonObject("properties", properties)
            if (required.isNotEmpty()) putJsonArray("required") { required.forEach(::add) }
        }
    }
}

fun JsonObject.text(name: String): String? =
    (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

fun JsonObject.integer(name: String): Int? =
    (this[name] as? JsonPrimitive)?.content?.toIntOrNull()

fun JsonObject.boolean(name: String): Boolean? =
    (this[name] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

fun JsonObject.array(name: String): JsonArray? = this[name] as? JsonArray

/** 셸에 값을 그대로 전달하기 위한 작은따옴표 인용. */
fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
