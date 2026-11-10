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

/** 공통 필드 양식으로 관리하는 반복 설정 그룹이에요. */
data class PluginSettingGroup(
    val key: String,
    val title: String,
    val fields: List<PluginSetting>,
    val description: String = "",
    val itemTitleKey: String = "name",
    val addLabel: String = "추가",
) {
    init {
        require(SETTING_ENTRY_KEY.matches(key)) { "반복 설정 키가 올바르지 않아요." }
        require(title.isNotBlank() && addLabel.isNotBlank()) { "반복 설정 제목과 추가 버튼 이름이 필요해요." }
        require(fields.size in 1..32) { "반복 설정 필드는 1개 이상 32개 이하여야 해요." }
        val keys = HashSet<String>()
        require(fields.all { keys.add(it.key) }) { "반복 설정 필드 키가 중복됐어요." }
        require(fields.any { it.key == itemTitleKey && it.kind == PluginSettingKind.TEXT }) {
            "항목 이름으로 표시할 텍스트 필드가 필요해요."
        }
    }
}

/** 안정적인 항목 ID와 선언된 필드 값이에요. 비밀값이 문자열 표현에 노출되지 않아요. */
class PluginSettingEntry(
    val id: String,
    val values: Map<String, String>,
) {
    init {
        require(SETTING_ENTRY_ID.matches(id)) { "반복 설정 항목 ID가 올바르지 않아요." }
    }
}

private val SETTING_ENTRY_KEY = Regex("[a-z][a-z0-9_.-]{0,63}")
private val SETTING_ENTRY_ID = Regex("[a-z0-9][a-z0-9._-]{1,63}")
