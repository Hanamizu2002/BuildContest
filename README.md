# BuildContest 插件

BuildContest 是一个 Velocity 插件，解决建筑比赛单队单服配置繁琐的问题。插件通过 Docker-compose 启动子服务器，并将玩家传送到对应的子服务器进行竞赛。团队的配置和子服务器的信息都会持久化保存。

## 功能简介

- **自动创建子服**：通过调用 HTTP 接口，实现基于 Docker Compose 创建队伍服务器，并注册到 Velocity。
- **玩家自动传送**：玩家进入服务器时自动传送到其所属的子服务器。
- **权限控制**：通过 Bearer Token 进行 HTTP 请求的鉴权。
- **防止重复参赛**：添加队伍时，若玩家已存在于其他队伍，则不能再加入其他队伍。

## 安装步骤

1. **准备环境**
    - 需要安装 [Velocity](https://velocitypowered.com/) 代理服务器。
    - 确保已安装 Docker 和 Docker Compose，且配置正确。

2. **制作一个 Minecraft 比赛服务端镜像（子服）**
    - 推荐使用[小扳手](https://www.minebbs.com/resources/1-20-4.8372/)作为服务端
    - 端口配置为25565，需保证此端可以正常通过代理端访问，例如配置forwarding-mode等。
    - 配置 Dockerfile，用于构建镜像。
    - 打包镜像，如：`docker build -t minecraft-contest:latest .`
    - 创建docker-compose.yml文件，用于启动子服务器。

3. **构建插件**
    - 通过 Gradle 构建本插件。

   ```bash
   ./gradlew shadowjar
   ```

4. **配置插件**
    - 将插件 jar 文件放入 `plugins/` 目录中。
    - 启动 Velocity 服务器一次以生成配置文件，然后停止服务器进行配置。

5. **配置文件**
    - 在 `plugins/BuilderContest/` 目录下修改 `config.yml` 文件，配置端口等信息。
    - 将 `docker-compose-default.yml` 放置在插件目录下，用于作为启动子服务器的模板。

6. **启动服务器**
    - 启动服务器，获取 `config.yml` 中生成的 `bearer-token`。

## 配置文件说明

**config.yml**
```yaml
ktor:
  http-port: 8080 # HTTP 服务端口。
  port-start: 30000 # 子服务器端口起始值。
  bearer-token: # Bearer Token，初次使用，请不要配置此项（留空）。
  allow-host: "*" # 允许的 Host 地址。
```

**docker-compose-default.yml示例**
- 请使用`#SERVER_NAME_PLACEHOLDE`来作为docker容器名称的占位符
- 使用`#SERVER_PORT_PLACEHOLDER`来作为端口的占位符
```yaml
version: '3.8'
services:
  minecraft_server:
    image: minecraft-contest:latest
    container_name: "#SERVER_NAME_PLACEHOLDER"
    ports:
      - #SERVER_PORT_PLACEHOLDER:25565
    volumes:
      # 将本地地图目录挂载到容器内指定的路径
      - ./worlds/#SERVER_NAME_PLACEHOLDER/world:/app/world
    environment:
      EULA: "TRUE"
    restart: unless-stopped
```

## 使用说明

### HTTP 接口

- 插件启动后，将会启动一个 HTTP 服务，默认端口为 `8080`。
- 主要接口包括：
    - **添加团队**：`POST /team/add`，需要 Bearer Token 鉴权，传入 `teamId`、`teamName` 和 `members` 字段。

### 示例请求

```bash
curl --location --request POST 'http://localhost:8080/team/add' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer sSCj4piDoEfV2zIo_C8lD7S4ul0YlVOyBr2BkKpdsTI（请从config.yml中获取）' \
--data-raw '{
    "teamId": "team_1",
    "teamName": "队伍1",
    "members": ["Hanamizu_", "user1"]
}'
```
