package work.alsace.buildContest.services

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import work.alsace.buildContest.BuildContest
import java.net.InetSocketAddress

/**
 * ContestService 类用于管理与创建服务器相关的业务逻辑。
 *
 * @param server Velocity 服务器实例。
 * @param plugin 插件实例，用于获取配置管理器和日志记录器。
 */
class ContestService(
    private val server: ProxyServer,
    private val plugin: BuildContest
) {
    private var nextAvailablePort = plugin.configManager.getStartPort()

    /**
     * 获取下一个可用端口，端口从 port 开始递增。
     *
     * @return 下一个可用的端口号。
     */
    private fun getNextAvailablePort(): Int {
        nextAvailablePort++
        plugin.configManager.setStartPort(nextAvailablePort)
        return nextAvailablePort
    }

    /**
     * 创建一个子服、启动一个Docker 容器，并将其配置到 Velocity 中。
     *
     *
     * @param teamName 队伍名称（作为服务器名称）。
     * @return 是否成功创建服务器。
     */
    fun createAndRegisterServer(teamId: String, teamName: String, members: List<String>): Boolean {
        val port = getNextAvailablePort()
        val success = plugin.dockerManager.startContainer(teamId, port)

        if (success) {
            addServerToVelocity(teamId, port)
            plugin.configManager.addNewTeam(teamId, teamName, port, members)
            plugin.logger.info("队伍 $teamName 已成功创建在端口 $port 上。")
            return true
        } else {
            plugin.logger.error("创建队伍 $teamName 失败")
            return false
        }
    }

    /**
     * 将新创建的子服务器添加到 Velocity 配置中。
     *
     * @param teamName 队伍名称（作为服务器名称）。
     * @param port 子服务器的端口号。
     */
    private fun addServerToVelocity(teamName: String, port: Int) {
        val serverInfo = ServerInfo(teamName, InetSocketAddress("127.0.0.1", port))
        server.registerServer(serverInfo)
        plugin.logger.info("已将新服务器 $teamName 添加到 Velocity，端口: $port")
    }
}
