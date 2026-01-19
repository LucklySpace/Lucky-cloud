### Change Log

---

### 2026-01-19

- **重构懒加载配置到 Nacos 配置中心**
  - 将所有模块的 `spring.main.lazy-initialization` 配置迁移到 Nacos 的 `im-lazy-init-config.yml` 文件中统一管理
  - 修改 `LazyInitConfig.java` 以支持从 `im.lazy-init.config[index].enabled` 读取懒加载开关
  - 新增 `nacos-config-example/im-lazy-init-config.yml` 配置示例文件
  - 清理各模块本地配置文件中的 `lazy-initialization` 配置（im-server, im-auth, im-database, im-leaf）
  - **注意**: 部署时需要在 Nacos 配置中心创建 `im-lazy-init-config.yml` 配置文件

---

- 添加  **lazy-initialization** 到 `im-auth`模块 和 `im-server`模块。 
- 添加 **async start** 到 `im-connect` 模块中的`WebSocketTemplate` 和 `TCPSocketTemplate`代码。
- 添加 **测试用例** 到`im-server`模块，
- 添加 **graalvm** 到`im-server`模块。 


要求打包`im-server`模块的时候使用命令：
```shell
mvn  -Pnative spring-boot:build-image
```