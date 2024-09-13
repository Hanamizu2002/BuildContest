package work.alsace.buildContest.listeners

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import net.kyori.adventure.text.Component
import work.alsace.buildContest.BuildContest

/**
 * PlayerListener 类负责监听玩家相关的事件。
 *
 * @param plugin 插件实例，用于访问相关服务和配置。
 */
class PlayerListener(private val plugin: BuildContest) {

    /**
     * 监听玩家登录后事件，将玩家传送到对应的子服务器。
     *
     * @param event 玩家登录事件。
     */
    @Subscribe
    fun onPlayerJoin(event: PostLoginEvent) {
        val player = event.player
        val playerName = player.username

        // 使用 getTeamIdByMember() 获取玩家所属的队伍 ID
        val teamId = plugin.configManager.getTeamIdByMember(playerName)

        if (teamId != null) {
            // 根据队伍 ID 获取对应的子服务器
            val server = plugin.server.getServer(teamId)

            if (server.isPresent) {
                // 将玩家传送到对应的子服务器
                sendPlayerToServer(player, server.get())
            } else {
                plugin.logger.error("无法找到子服务器: $teamId")
                player.sendMessage(Component.text("无法找到您的子服务器，请联系管理员。"))
            }
        } else {
            plugin.logger.info("玩家 $playerName 未找到对应的队伍，未进行传送。")
            player.sendMessage(Component.text("您未加入任何队伍，请联系管理员。"))
        }
    }

    /**
     * 将玩家传送到指定的子服务器。
     *
     * @param player 要传送的玩家。
     * @param server 目标子服务器。
     */
    private fun sendPlayerToServer(player: Player, server: RegisteredServer) {
        player.createConnectionRequest(server).connectWithIndication().thenAccept { result ->
            if (result) {
                plugin.logger.info("玩家 ${player.username} 成功传送到子服务器 ${server.serverInfo.name}")
            } else {
                plugin.logger.error("无法传送玩家 ${player.username} 到子服务器 ${server.serverInfo.name}")
                player.sendMessage(Component.text("传送失败，请稍后重试。"))
            }
        }
    }
}
