package work.alsace.buildContest.utils

import org.slf4j.Logger
import work.alsace.buildContest.BuildContest
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * DockerManager 类使用 Docker SDK 管理 Docker 容器。
 * 该类负责修改 docker-compose 文件并管理容器的启动和停止。
 *
 * @param plugin 传入插件实例，获取日志记录器。
 * @param pluginPath 插件所在的目录，用于查找 docker-compose 文件。
 */
class DockerManager(plugin: BuildContest, private val pluginPath: Path) {

    private val dockerComposePath: Path = pluginPath.resolve("docker-compose.yml")
    private val logger: Logger = plugin.logger

    /**
     * 使用修改后的 docker-compose-default.yml 文件启动 Docker 容器。
     *
     * @param serverName 要启动的服务器名称。
     * @param port 分配给服务器的端口号。
     * @return 如果容器启动成功则返回 true，否则返回 false。
     */
    fun startContainer(serverName: String, port: Int): Boolean {
        return try {
            // 修改 Docker Compose 文件
            generateDockerCompose(serverName, port)

            // 使用 docker-compose 启动容器
            val isStarted = startWithDockerCompose(serverName)
            if (isStarted) {
                logger.info("Docker 容器通过 docker-compose 启动成功，服务器名称: $serverName")
            } else {
                logger.error("无法通过 docker-compose 启动 Docker 容器，服务器名称: $serverName")
            }
            isStarted
        } catch (e: IOException) {
            logger.error("启动 Docker 容器时发生错误: ", e)
            false
        } catch (e: InterruptedException) {
            logger.error("启动 Docker 容器时被中断: ", e)
            false
        }
    }

    /**
     * 修改 docker-compose-default.yml 文件，将占位符替换为实际的值。
     *
     * @param serverName 要替换到 compose 文件中的服务器名称。
     * @param port 要替换到 compose 文件中的端口号。
     * @throws IOException 如果文件读取或写入失败抛出异常。
     */
    @Throws(IOException::class)
    private fun generateDockerCompose(serverName: String, port: Int) {
        // 定义模板文件路径
        val templatePath = pluginPath.resolve("docker-compose-default.yml")

        // 确保模板文件存在
        if (!Files.exists(templatePath)) {
            logger.error("模板文件不存在: ${templatePath.toAbsolutePath()}")
            return
        }

        // 定义目标文件路径，将模板文件复制到这个路径
        val targetPath = pluginPath.resolve("docker-compose.yml")

        if (Files.exists(targetPath)) {
            Files.delete(targetPath)
        }

        // 复制模板文件到目标路径
        Files.copy(templatePath, targetPath, StandardCopyOption.REPLACE_EXISTING)

        // 读取目标文件内容
        var content = Files.readString(targetPath)

        // 替换占位符
        val replacements = mapOf(
            "#SERVER_NAME_PLACEHOLDER" to serverName,
            "#SERVER_PORT_PLACEHOLDER" to port.toString()
        )

        // 执行替换操作
        replacements.forEach { (key, value) ->
            content = content.replace(key, value)
        }

        // 将替换后的内容写入目标文件
        Files.writeString(targetPath, content, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
        logger.info("Docker Compose 文件已生成并修改，服务器名称: {}, 端口: {}", serverName, port)
    }

    /**
     * 使用 docker-compose 启动容器。
     *
     * @return 启动是否成功。
     */
    private fun startWithDockerCompose(serverName: String): Boolean {
        return try {
            val processBuilder = ProcessBuilder(
                "docker-compose",
                "-f", dockerComposePath.toString(),
                "-p", serverName,
                "up",
                "-d"
            )
            logger.warn(pluginPath.toString())
            // 合并标准错误流到标准输出流中，以便捕获所有输出
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            // 捕获并输出执行过程中的日志信息
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            // 输出执行结果的日志
            logger.info("Docker Compose 输出: $output")

            if (exitCode == 0) {
                logger.info("Docker 容器通过 docker-compose 成功启动")
                true
            } else {
                logger.error("Docker Compose 执行失败，退出码: $exitCode，输出: $output")
                false
            }
        } catch (e: Exception) {
            logger.error("通过 docker-compose 启动容器时出错: ", e)
            false
        }
    }

    /**
     * 停止并删除通过 docker-compose 启动的容器。
     *
     * @param serverName 要停止的服务器名称。
     * @return 如果容器停止成功则返回 true，否则返回 false。
     */
    fun stopContainer(serverName: String): Boolean {
        return try {
            val processBuilder = ProcessBuilder(
                "docker-compose",
                "-f", dockerComposePath.toString(),
                "down"
            )
            processBuilder.directory(pluginPath.toFile())
            val process = processBuilder.start()
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                logger.info("Docker 容器通过 docker-compose 停止成功，服务器名称: $serverName")
                true
            } else {
                logger.error("无法通过 docker-compose 停止 Docker 容器，服务器名称: $serverName")
                false
            }
        } catch (e: Exception) {
            logger.error("通过 docker-compose 停止容器时发生错误: ", e)
            false
        }
    }
}
