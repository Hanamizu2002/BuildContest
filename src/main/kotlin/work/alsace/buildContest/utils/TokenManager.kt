package work.alsace.buildContest.utils

import java.security.SecureRandom
import java.util.Base64

object TokenManager {
    /**
     * 生成一个指定长度的随机 Token。
     *
     * @param length Token 的字节长度。
     * @return 生成的随机 Token。
     */
    fun generateRandomToken(length: Int = 32): String {
        val secureRandom = SecureRandom() // 使用 SecureRandom 生成安全的随机数
        val randomBytes = ByteArray(length)
        secureRandom.nextBytes(randomBytes) // 填充随机字节数组
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes) // 使用 URL 安全的 Base64 编码
    }
}
