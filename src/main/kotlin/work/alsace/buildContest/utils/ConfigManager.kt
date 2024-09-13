package work.alsace.buildContest.utils

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import work.alsace.buildContest.BuildContest
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

/**
 * ConfigManager 类用于管理插件的配置和数据文件。
 * 它负责加载、保存和更新配置文件内容。
 *
 * @param plugin 插件实例，用于获取插件路径和日志记录器。
 */
class ConfigManager(private val plugin: BuildContest) {

    private val configFile: File = File(plugin.pluginPath.toFile(), "config.yml")
    private val dataFile: File = File(plugin.pluginPath.toFile(), "data.yml")
    private var data: MutableMap<String, Any> = mutableMapOf()
    private var config: MutableMap<String, Any> = mutableMapOf()

    init {
        loadFiles()
        if (getBearerToken() == null) {
            setBearerToken(TokenManager.generateRandomToken())
        }
    }

    /**
     * 加载配置和数据文件，如果文件不存在则创建并初始化默认配置。
     */
    private fun loadFiles() {
        // 确保插件目录存在
        if (!plugin.pluginPath.exists()) {
            plugin.pluginPath.toFile().mkdirs()
        }

        // 加载配置文件和数据文件
        config = loadOrCreateFile(configFile, "config.yml")
        data = loadOrCreateFile(dataFile, mapOf("teams" to emptyMap<String, Map<String, Any>>()))
    }

    /**
     * 获取开始端口，默认值为 30000。
     *
     * @return 从配置文件中获取的开始端口号。
     */
    fun getStartPort(): Int {
        val ktorConfig = config["ktor"] as? Map<String, Any> ?: return 30000
        return ktorConfig["port-start"] as? Int ?: 30000
    }

    /**
     * 设置开始端口。
     *
     * @param port 要设置的端口号。
     */
    fun setStartPort(port: Int) {
        val ktorConfig = config["ktor"] as? MutableMap<String, Any> ?: return
        ktorConfig["port-start"] = port
        saveConfig(config, configFile)
    }


    /**
     * 获取 HTTP 端口，默认值为 8080。
     *
     * @return 从配置文件中获取的 HTTP 服务端口号。
     */
    fun getPort(): Int {
        val ktorConfig = config["ktor"] as? Map<String, Any> ?: return 8080
        return ktorConfig["http-port"] as? Int ?: 8080
    }

    /**
     * 获取队伍 ID 和其对应端口号的 Map。
     *
     * @return 包含队伍 ID 和端口号的 Map，如果数据不存在则返回空的 Map。
     */
    fun getTeamIdPortMap(): Map<String, Int> {
        // 从数据中获取所有队伍
        val teams = data["teams"] as? Map<String, Map<String, Any>> ?: return emptyMap()

        // 构建一个 Map，将队伍 ID 和端口号对应起来
        val teamPortMap = mutableMapOf<String, Int>()

        // 遍历每个队伍的条目
        teams.forEach { (teamId, teamData) ->
            // 从队伍数据中获取端口号
            val port = teamData["port"] as? Int ?: return@forEach
            // 将队伍 ID 和端口号加入 Map
            teamPortMap[teamId] = port
        }

        return teamPortMap
    }

    /**
     * 读取数据文件中的队伍 ID 列表。
     *
     * @return 队伍 ID 列表，如果没有队伍则返回空列表。
     */
    fun getTeamList(): List<String> {
        val teams = data["teams"] as? Map<String, Any> ?: return emptyList()
        return teams.keys.toList()
    }

    /**
     * 根据队伍 ID 获取队伍成员列表。
     *
     * @param teamId 队伍 ID。
     * @return 队伍成员列表，如果队伍不存在则返回 null。
     */
    fun getTeamMembers(teamId: String): List<String>? {
        val teams = data["teams"] as? Map<String, Map<String, Any>> ?: return null
        return teams[teamId]?.get("members") as? List<String>
    }

    /**
     * 根据队伍 ID 获取队伍名称。
     *
     * @param teamId 队伍 ID。
     * @return 队伍名称，如果队伍不存在则返回 null。
     */
    fun getTeamName(teamId: String): String? {
        val teams = data["teams"] as? Map<String, Map<String, Any>> ?: return null
        return teams[teamId]?.get("name") as? String
    }

    /**
     * 根据队伍 ID 获取队伍服务器端口号。
     * @param teamId 队伍 ID。
     * @return 队伍所在服务器端口号，如果队伍不存在则返回 null。
     */
    fun getTeamPort(teamId: String): Int? {
        val teams = data["teams"] as? Map<String, Map<String, Any>> ?: return null
        return teams[teamId]?.get("port") as? Int
    }

    /**
     * 根据成员名称查找其所属的队伍 ID。
     *
     * @param member 成员名称。
     * @return 队伍 ID，如果成员未找到则返回 null。
     */
    fun getTeamIdByMember(member: String): String? {
        // 从数据中获取所有队伍
        val teams = data["teams"] as? Map<String, Map<String, Any>> ?: return null

        // 查找队伍中包含该成员的队伍 ID
        return teams.entries.find { entry ->
            // 获取队伍中的成员列表并转换为 List<String>
            val members = entry.value["members"] as? List<*> ?: return@find false
            // 检查成员是否在列表中
            members.contains(member)
        }?.key
    }

    /**
     * 判断指定成员是否存在于任何队伍中。
     *
     * @param member 成员名称。
     * @return 如果成员存在于其他队伍中，则返回 true，否则返回 false。
     */
    fun isMemberInAnyTeam(member: String): Boolean {
        // 从数据中获取所有队伍
        val teams = data["teams"] as? Map<String, Map<String, Any>> ?: return false

        // 遍历所有队伍，检查成员是否存在
        return teams.any { entry ->
            val members = entry.value["members"] as? List<*> ?: return@any false
            members.contains(member)
        }
    }

    /**
     * 将新队伍添加到数据文件中，并保存更新。
     *
     * @param teamId 队伍 ID。
     * @param teamName 队伍名称。
     * @param port 队伍服务器端口号。
     * @param members 队伍成员列表。
     */
    fun addNewTeam(teamId: String, teamName: String, port: Int, members: List<String>) {
        val teams =
            data.getOrPut("teams") { mutableMapOf<String, Map<String, Any>>() } as MutableMap<String, Map<String, Any>>
        teams[teamId] = mapOf("name" to teamName, "port" to port, "members" to members)

        // 保存更新后的数据
        saveConfig(data, dataFile)
    }

    /**
     * 获取用于身份验证的 bearer token。
     *
     * @return bearer token，如果未找到则返回 null。
     */
    fun getBearerToken(): String? {
        val ktorConfig = config["ktor"] as? Map<String, Any> ?: return null
        return ktorConfig["bearer-token"] as? String
    }

    /**
     * 设置用于身份验证的 bearer token。
     *
     * @param token 要设置的 bearer token。
     */
    private fun setBearerToken(token: String) {
        val ktorConfig = config["ktor"] as? MutableMap<String, Any> ?: return
        ktorConfig["bearer-token"] = token
        saveConfig(config, configFile)
    }


    /**
     * 加载指定文件，如果文件不存在则创建并写入默认内容。
     *
     * @param file 要加载的文件。
     * @param defaultConfig 默认配置内容（当文件不存在时写入）。
     * @return 加载的文件内容，作为键值对 Map。
     */
    private fun loadOrCreateFile(file: File, defaultConfig: Map<String, Any> = emptyMap()): MutableMap<String, Any> {
        if (!file.exists()) {
            try {
                file.createNewFile()
                saveConfig(defaultConfig, file)
            } catch (e: IOException) {
                plugin.logger.error("无法创建文件 ${file.name}", e)
            }
        }
        return try {
            loadConfigFile(file).toMutableMap()
        } catch (e: IOException) {
            plugin.logger.error("无法加载文件 ${file.name}", e)
            mutableMapOf()
        }
    }

    /**
     * 从指定文件中加载配置，如果文件不存在则从 resources 目录复制默认配置。
     *
     * @param file 目标配置文件。
     * @param resourcePath 资源目录中的默认配置文件路径（如 "config.yml"）。
     * @return 加载的配置文件内容。
     */
    private fun loadOrCreateFile(file: File, resourcePath: String): MutableMap<String, Any> {
        if (!file.exists()) {
            try {
                // 从 resources 目录复制默认配置文件
                val inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)
                if (inputStream != null) {
                    Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    plugin.logger.info("从资源文件 $resourcePath 创建了新的配置文件 ${file.name}")
                } else {
                    plugin.logger.error("无法找到资源文件 $resourcePath")
                    // 如果 resources 目录中没有找到，使用空的默认配置
                    saveConfig(emptyMap(), file)
                }
            } catch (e: IOException) {
                plugin.logger.error("无法创建文件 ${file.name}", e)
            }
        }
        return try {
            loadConfigFile(file).toMutableMap()
        } catch (e: IOException) {
            plugin.logger.error("无法加载文件 ${file.name}", e)
            mutableMapOf()
        }
    }

    /**
     * 从文件中加载配置。
     *
     * @param file 配置文件。
     * @return 配置的键值对 Map。
     * @throws IOException 如果读取文件失败。
     */
    @Throws(IOException::class)
    private fun loadConfigFile(file: File): Map<String, Any> {
        FileInputStream(file).use { inputStream ->
            val yaml = Yaml()
            return yaml.load(inputStream) ?: emptyMap()
        }
    }

    /**
     * 保存配置到文件。
     *
     * @param config 配置的键值对 Map。
     * @param file 配置文件。
     * @throws IOException 如果写入文件失败。
     */
    @Throws(IOException::class)
    private fun saveConfig(config: Map<String, Any>, file: File) {
        FileWriter(file).use { writer ->
            val options = DumperOptions().apply {
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            }
            val yaml = Yaml(options)
            yaml.dump(config, writer)
        }
    }
}
