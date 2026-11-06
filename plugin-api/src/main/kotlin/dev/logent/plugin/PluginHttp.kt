package dev.logent.plugin

/**
 * 플러그인이 호스트에 요청하는 HTTPS 호출이에요.
 *
 * 호스트는 공용 HTTPS만 허용하고 리다이렉트마다 같은 검사를 해요. 비밀 헤더는
 * 로그·프롬프트에 남기지 않아요.
 */
data class PluginHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val timeoutSeconds: Int = 30,
    val maxResponseBytes: Int = 1_048_576,
) {
    init {
        require(method in setOf("GET", "POST", "PUT", "DELETE")) { "지원하지 않는 HTTP 메서드예요." }
        require(url.startsWith("https://")) { "플러그인 HTTP는 https://만 허용해요." }
        require(timeoutSeconds in 1..120) { "HTTP 시간 제한은 1~120초여야 해요." }
        require(maxResponseBytes in 1..4_194_304) { "응답 한도는 1바이트~4MiB여야 해요." }
        require((body?.toByteArray()?.size ?: 0) <= 262_144) { "요청 본문은 256KiB 이하여야 해요." }
        require(headers.keys.all { it.matches(HEADER_NAME) }) { "헤더 이름이 올바르지 않아요." }
    }

    private companion object {
        val HEADER_NAME = Regex("[A-Za-z0-9-]{1,64}")
    }
}

data class PluginHttpResponse(
    val status: Int,
    val headers: Map<String, String>,
    val body: String,
)
