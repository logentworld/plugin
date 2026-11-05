package dev.logent.plugin

/** 코어가 공통 설정 화면으로 표시할 필드 종류예요. 비밀값은 기본으로 가려요. */
enum class PluginSettingKind { TEXT, SECRET, BOOLEAN }

/** 설정 이름은 해당 플러그인 안에서만 고유하며 프롬프트에 포함하지 않아요. */
data class PluginSetting(
    val key: String,
    val title: String,
    val description: String = "",
    val kind: PluginSettingKind = PluginSettingKind.TEXT,
    val defaultValue: String = "",
) {
    init {
        require(Regex("[a-z][a-z0-9_.-]{0,63}").matches(key)) { "설정 키가 올바르지 않아요." }
        require(title.isNotBlank()) { "설정 제목이 필요해요." }
        require(defaultValue.length <= 4096) { "설정 기본값이 너무 길어요." }
        require(kind != PluginSettingKind.BOOLEAN || defaultValue in setOf("", "true", "false")) {
            "스위치 기본값은 true 또는 false여야 해요."
        }
    }
}
