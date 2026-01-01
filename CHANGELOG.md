### Change Log

---

- 添加  **lazy-initialization** 到 `im-auth`模块 和 `im-server`模块。 
- 添加 **async start** 到 `im-connect` 模块中的`WebSocketTemplate` 和 `TCPSocketTemplate`代码。
- 添加 **测试用例** 到`im-server`模块，
- 添加 **graalvm** 到`im-server`模块。 


要求打包`im-server`模块的时候使用命令：
```shell
mvn  -Pnative spring-boot:build-image
```