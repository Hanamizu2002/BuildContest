package work.alsace.buildContest.ktor.controller

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import work.alsace.buildContest.BuildContest
import work.alsace.buildContest.utils.FieldValidator

/**
 * TeamController 类负责处理与团队相关的 HTTP 请求。
 *
 * @param plugin 插件实例，用于获取业务逻辑服务。
 */
fun Route.team(plugin: BuildContest) {

    val contestService = plugin.contestService
    val configManager = plugin.configManager

    // 定义正则表达式验证规则
    val teamIdRegex = Regex("^[a-zA-Z0-9_]{3,10}\$")
    val teamNameRegex = Regex("^.{3,10}\$")
    val memberNameRegex = Regex("^[a-zA-Z0-9_]{3,16}\$")

    post("/team/add") {
        // 解析请求体，获取 teamId, teamName, members
        val params = call.receive<Map<String, Any>>()
        val teamId = params["teamId"] as? String
        val teamName = params["teamName"] as? String
        val members = params["members"] as? List<String>

        // 验证输入是否为空
        if (teamId == null || teamName == null || members == null) {
            call.respond(HttpStatusCode.BadRequest, "输入数据无效，请检查 teamId, teamName 和 members 是否正确。")
            return@post
        }

        // 使用 FieldValidator 验证字段内容
        if (!FieldValidator.validate("teamId", teamId, teamIdRegex)) {
            call.respond(HttpStatusCode.BadRequest, "teamId 格式不正确。只能包含字母、数字和下划线，长度为 3-10 个字符。")
            return@post
        }

        if (!FieldValidator.validate("teamName", teamName, teamNameRegex)) {
            call.respond(HttpStatusCode.BadRequest, "teamName 格式不正确。长度必须为 3-10 个字符。")
            return@post
        }

        // 验证每个成员名是否符合格式
        for (member in members) {
            if (!FieldValidator.validate("member", member, memberNameRegex)) {
                call.respond(HttpStatusCode.BadRequest, "成员名 $member 格式不正确。只能包含字母、数字和下划线，长度为 3-16 个字符。")
                return@post
            }

            // 检查成员是否已存在于其他队伍中
            if (configManager.isMemberInAnyTeam(member)) {
                call.respond(HttpStatusCode.BadRequest, "成员 $member 已存在于其他队伍中！")
                return@post
            }
        }

        // 调用业务逻辑服务，添加新队伍
        if (contestService.createAndRegisterServer(teamId, teamName, members)) {
            call.respond(HttpStatusCode.OK, "队伍添加成功。")
        } else {
            call.respond(HttpStatusCode.InternalServerError, "添加队伍失败，请稍后重试。")
        }
    }
}
