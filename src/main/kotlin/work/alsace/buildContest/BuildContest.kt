package work.alsace.buildContest

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import org.slf4j.Logger
import work.alsace.buildContest.ktor.KtorApplication
import work.alsace.buildContest.listeners.PlayerListener
import work.alsace.buildContest.services.ContestService
import work.alsace.buildContest.utils.ConfigManager
import work.alsace.buildContest.utils.DockerManager
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

@Plugin(
    id = "buildcontest",
    name = "BuildContest",
    version = BuildConstants.VERSION
)
class BuildContest @Inject constructor(
    val server: ProxyServer,
    val logger: Logger
) {
    val pluginPath: Path = Path("plugins/BuilderContest/").apply { createDirectories() }
    lateinit var configManager: ConfigManager
    lateinit var contestService: ContestService
    lateinit var dockerManager: DockerManager
    private lateinit var ktorApplication: KtorApplication

    /**
     * 代理初始化时调用。
     *
     * @param event 初始化事件
     */
    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        dockerManager = DockerManager(this, pluginPath)
        configManager = ConfigManager(this)
        contestService = ContestService(server, this)
        //注册监听器
        server.eventManager.register(this, PlayerListener(this))

        //注册子服
        registerServersOnStartup()

        // 启动ktor
        ktorApplication = KtorApplication(this, logger)
        ktorApplication.start()

        logger.info("BuildContest 插件已初始化。")
    }

    /**
     * 根据配置注册子服务器。
     */
    private fun registerServersOnStartup() {
        // 获取队伍 ID 和端口的映射
        val teamPortMap = configManager.getTeamIdPortMap()

        if (teamPortMap.isEmpty()) {
            logger.info("未找到需要注册的子服务器信息。")
            return
        }

        // 遍历映射并注册子服务器
        teamPortMap.forEach { (teamId, port) ->
            contestService.registerServerToVelocity(teamId, port)
        }
    }
}
