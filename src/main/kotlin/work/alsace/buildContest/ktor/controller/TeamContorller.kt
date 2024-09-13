package work.alsace.buildContest.ktor.controller

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import work.alsace.buildContest.BuildContest

/**
 * TeamController 类负责处理与团队相关的 HTTP 请求。
 *
 * @param plugin 插件实例，用于获取业务逻辑服务。
 */
fun Route.team(plugin: BuildContest) {

    val contestService = plugin.contestService
    val configManager = plugin.configManager

    post("/team/add") {
        // 解析请求体，获取 teamId, teamName, members
        val params = call.receive<Map<String, Any>>()
        val teamId = params["teamId"] as? String
        val teamName = params["teamName"] as? String
        val members = params["members"] as? List<String>

        // 验证输入
        if (teamId == null || teamName == null || members == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid input data")
            return@post
        }

        for(member in members) {
            if (!configManager.isMemberInAnyTeam(member)) {
                call.respond(HttpStatusCode.BadRequest, "Member $member is exist!")
                return@post
            }
        }

        // 调用业务逻辑服务，添加新队伍
        if (contestService.createAndRegisterServer(teamId, teamName, members)) {
            call.respond(HttpStatusCode.OK, "Team added successfully")
        } else {
            call.respond(HttpStatusCode.InternalServerError, "Failed to add team")
        }
    }
}
