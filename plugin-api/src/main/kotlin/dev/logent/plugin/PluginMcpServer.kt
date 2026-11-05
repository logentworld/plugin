package dev.logent.plugin

/**
 * 플러그인이 기여하는 Streamable HTTP MCP 서버예요.
 * 이름은 플러그인 안에서만 고유하고, 코어가 네임스페이스를 붙여 사용자 설정과 합쳐요.
 */
data class PluginMcpServer(
    val name: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
) {
    init {
        require(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,63}").matches(name)) { "MCP 서버 이름이 올바르지 않아요." }
        require(url.length <= 2048) { "MCP 서버 주소가 너무 길어요." }
        require(headers.keys.all { Regex("[A-Za-z0-9-]{1,128}").matches(it) }) { "MCP 헤더 이름이 올바르지 않아요." }
        require(headers.values.all { it.length <= 4096 && '\n' !in it && '\r' !in it }) { "MCP 헤더 값이 올바르지 않아요." }
    }
}
