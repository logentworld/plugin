package com.sleepysoong.logent.plugin

/** 코어가 공통 설정 화면으로 표시할 필드 종류예요. 비밀값은 기본으로 가려요. */
enum class PluginSettingKind { TEXT, SECRET, BOOLEAN, SLIDER, SELECT }

/**
 * 플러그인이 선언하는 설정 필드예요.
 *
 * SLIDER는 [minValue]..[maxValue] 범위와 [step] 간격을, SELECT는 [options] 중 하나를 써요.
 * 기존 플러그인이 3.0 계약의 생성자를 그대로 호출할 수 있도록 기본 인자를 붙였어요.
 */
data class PluginSetting @JvmOverloads constructor(
    val key: String,
    val title: String,
    val description: String = "",
    val kind: PluginSettingKind = PluginSettingKind.TEXT,
    val defaultValue: String = "",
    val minValue: Int? = null,
    val maxValue: Int? = null,
    val step: Int = 1,
    val options: List<String> = emptyList(),
) {
    init {
        require(Regex("[a-z][a-z0-9_.-]{0,63}").matches(key)) { "설정 키가 올바르지 않아요." }
        require(title.isNotBlank()) { "설정 제목이 필요해요." }
        require(defaultValue.length <= 4096) { "설정 기본값이 너무 길어요." }
        require(kind != PluginSettingKind.BOOLEAN || defaultValue in setOf("", "true", "false")) {
            "스위치 기본값은 true 또는 false여야 해요."
        }
        if (kind == PluginSettingKind.SLIDER) {
            val min = requireNotNull(minValue) { "슬라이더 최솟값이 필요해요." }
            val max = requireNotNull(maxValue) { "슬라이더 최댓값이 필요해요." }
            require(min < max) { "슬라이더 범위가 올바르지 않아요." }
            require(step > 0) { "슬라이더 간격은 1 이상이어야 해요." }
            require(defaultValue.isEmpty() || defaultValue.toIntOrNull()?.let { it in min..max } == true) {
                "슬라이더 기본값이 범위를 벗어났어요."
            }
        }
        if (kind == PluginSettingKind.SELECT) {
            require(options.size in 2..64) { "선택 항목은 2개 이상 64개 이하여야 해요." }
            require(options.all { it.isNotBlank() && it.length <= 100 }) { "선택 항목이 올바르지 않아요." }
            require(options.distinct().size == options.size) { "선택 항목이 중복됐어요." }
            require(defaultValue.isEmpty() || defaultValue in options) { "선택 기본값이 목록에 없어요." }
        }
    }
}
