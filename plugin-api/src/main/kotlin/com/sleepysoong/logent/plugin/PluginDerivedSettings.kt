package com.sleepysoong.logent.plugin

/**
 * 같은 반복 설정 그룹의 일반 필드에서 유도되는, 사용자가 편집하지 않는 비밀이 아닌 텍스트예요.
 * 의존 키는 같은 그룹의 일반 필드를 가리켜요. 목록을 선언한 뒤에는 수정하지 않아요.
 * 호스트는 그룹과 필드의 실제 존재, 일반 필드와 파생 필드 간 키 중복 및 파생 의존 순환을 검증해요.
 */
data class PluginDerivedSetting(
    val groupKey: String,
    val key: String,
    val title: String,
    val description: String = "",
    val dependencyKeys: List<String>,
) {
    init {
        val keyPattern = Regex("[a-z][a-z0-9_.-]{0,63}")
        require(keyPattern.matches(groupKey)) { "반복 설정 키가 올바르지 않아요." }
        require(keyPattern.matches(key)) { "파생 설정 키가 올바르지 않아요." }
        require(title.isNotBlank()) { "파생 설정 제목이 필요해요." }
        require(dependencyKeys.all { keyPattern.matches(it) }) { "의존 설정 키가 올바르지 않아요." }
        require(dependencyKeys.size == dependencyKeys.toSet().size) { "의존 설정 키가 중복됐어요." }
        require(key !in dependencyKeys) { "자기 자신에 의존할 수 없어요." }
    }
}

/** API 5를 명시한 플러그인이 파생 설정을 선언할 때 구현하는 별도 기능이에요. */
interface PluginDerivedSettings {
    fun derivedSettings(): List<PluginDerivedSetting>
}

/**
 * 한 원자 시점의 항목, 파생 표시값과 각 파생 필드의 revision이에요.
 * [entry]에는 비밀이 들어갈 수 있어요. revision은 호스트가 발급한 불투명 출처 토큰이며
 * 계정 키나 키 해시가 아니에요. 값·키·revision과 비밀은 로그나 문자열 표현에 넣지 않아요.
 */
class PluginDerivedSettingSnapshot(
    val entry: PluginSettingEntry,
    val values: Map<String, String>,
    val revisions: Map<String, String>,
) {
    override fun toString(): String = "PluginDerivedSettingSnapshot"
}

/**
 * 호스트가 지원할 때만 제공하는 파생 설정 기능이에요. 일반 설정 export/import에는
 * 파생값을 포함하지 않아요. revision은 UI·export·로그에 노출하지 않아요.
 */
interface PluginDerivedSettingsHost {
    /** 선언된 그룹의 항목과 파생값, revision을 같은 원자 시점에서 읽어요. */
    fun derivedSettingSnapshots(groupKey: String): List<PluginDerivedSettingSnapshot>

    /**
     * 같은 항목과 필드에 발급된 최신 revision일 때만 원자적으로 표시값을 저장해요.
     * 의존 필드 변경, 항목 삭제·재생성, 패키지 교체·비활성화 이후의 revision은 false예요.
     * 저장하지 못한 오류는 실패로 반환하며, 미지원 기능을 가짜 성공으로 대체하지 않아요.
     * 비밀은 파생 표시값이나 출력·로그에 넣지 않아요.
     */
    fun writeDerivedSettingIfCurrent(
        groupKey: String,
        entryId: String,
        key: String,
        revision: String,
        value: String,
    ): Result<Boolean>
}
