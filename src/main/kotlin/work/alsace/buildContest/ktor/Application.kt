package work.alsace.buildContest.ktor

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import org.slf4j.Logger
import work.alsace.buildContest.BuildContest
import work.alsace.buildContest.ktor.controller.team

/**
 * KtorApplication 类用于启动 Ktor HTTP 服务器。
 *
 * @param plugin 插件主类，用于获取配置信息和服务实例。
 * @param logger 日志记录器。
 */
class KtorApplication(private val plugin: BuildContest, private val logger: Logger) {

    private lateinit var server: NettyApplicationEngine

    /**
     * 启动 Ktor 服务器。
     */
    fun start() {
        val port = plugin.configManager.getPort()
        server = embeddedServer(Netty, port, module = {
            configureModule()
        }).start(wait = false)

        logger.info("Ktor HTTP 服务器已启动，监听端口 $port")
    }

    /**
     * 停止 Ktor 服务器。
     */
    fun stop() {
        server.stop(1000, 2000)
        logger.info("Ktor HTTP 服务器已停止")
    }

    /**
     * 配置 Ktor 应用程序模块。
     */
    private fun Application.configureModule() {
        // 安装 ContentNegotiation 插件，支持 JSON 序列化
        install(ContentNegotiation) {
            jackson()
        }

        // 安装 CORS 插件，允许跨域请求
        install(CORS) {
            plugin.configManager.getOriginHost()?.let { allowHost(it) }
            allowHeader(HttpHeaders.Authorization)
        }

        // 安装鉴权插件，定义 Bearer 鉴权方案
        install(Authentication) {
            bearer("auth-bearer") {
                authenticate { tokenCredential ->
                    // 验证 Bearer Token 的值
                    if (tokenCredential.token == plugin.configManager.getBearerToken()
                        && tokenCredential.token.isNotEmpty()
                    ) {
                        UserIdPrincipal("user")
                    } else {
                        null
                    }
                }
            }
        }

        // 路由配置
        routing {
            authenticate("auth-bearer") {
                team(plugin)
            }
        }
    }
}
