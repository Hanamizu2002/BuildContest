package work.alsace.buildContest.utils

import java.security.SecureRandom
import java.util.*

object TokenManager {
    /**
     * 生成一个指定长度的随机 Token。
     *
     * @param length Token 的字节长度。
     * @return 生成的随机 Token。
     */
    fun generateRandomToken(length: Int = 32): String {
        val secureRandom = SecureRandom()
        val randomBytes = ByteArray(length)
        secureRandom.nextBytes(randomBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
    }
}
