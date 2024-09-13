package work.alsace.buildContest.commands

import com.velocitypowered.api.command.SimpleCommand
import net.kyori.adventure.text.Component
import work.alsace.buildContest.services.ContestService

/**
 * StartServerCommand 类用于处理 /contest create <teamname> 命令。
 *
 * @param contestService 业务逻辑服务类实例。
 */
class StartServerCommand(private val contestService: ContestService) : SimpleCommand {

    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()
        val args = invocation.arguments()

        if (args.size != 4 || args[0] != "create") {
            source.sendMessage(Component.text("Usage: /contest create <teamId> <teamName> <members>"))
            return
        }

        val teamId = args[1]
        val teamName = args[2]
        val members = args[3].split(",")

        // 创建 Docker 容器并更新配置
        val success = contestService.createAndRegisterServer(teamId, teamName, members)

        if (success) {
            source.sendMessage(Component.text("Server for team '$teamName' created successfully."))
        } else {
            source.sendMessage(Component.text("Failed to create server for team '$teamName'."))
        }
    }

    override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
        return true // 可以在这里设置权限检查逻辑
    }
}
