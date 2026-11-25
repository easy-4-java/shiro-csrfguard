# Shiro 3.0.2 适配契约与验收

本次变更以用户确认的版本矩阵为事实源，沿用现有分支，不安装或初始化新的规格工具。

| 分支 | Shiro | Java | Jackson | Servlet | Spring Framework |
| --- | --- | --- | --- | --- | --- |
| feature/2.0.x | 3.0.2 | 17 | 2 | Jakarta | 6.2 |
| feature/3.0.x | 3.0.2 | 21 | 3 | Jakarta | 7.0 |

当前仓库：`shiro-csrfguard`；当前版本线：`feature/3.0.x`。

## 范围与兼容性

- 所有 Shiro 模块必须解析到 3.0.2，不混用 1.x/2.x 或 Jakarta classifier。
- 同系列 easy4j 公共组件使用当前版本线，禁止 2.0.x 引用 3.0.x。
- Servlet 接口迁移到 jakarta.servlet；不替换 JDK 中仍合法的 javax.crypto、javax.naming 等命名空间。
- 2.0.x 保留 Jackson 2 API；3.0.x 使用 tools.jackson，Jackson 注解仍使用 com.fasterxml.jackson.annotation。
- 保留认证、授权、缓存及会话语义，外部 CAS/LDAP/微信等协议不以桩实现代替。
- 不修改 feature/1.0.x，不提交、推送或发布，不覆盖已有用户修改。

## 执行与验收

- [ ] 核对 Maven 有效模型、依赖树、JDK 和分支。
- [x] 对齐 Shiro、Jackson、Spring、Servlet 和公共组件的依赖声明（传递依赖树尚未验证）。
- [x] 使用目标 JDK 调用 Maven 测试流程，记录实际失败及阻塞。
- [ ] 依据编译及回归证据修正源码和测试中的接口兼容问题。
- [ ] 使用目标 JDK 运行全部现有测试，记录测试数、失败与跳过。
- [x] 静态检查没有残留 javax.servlet 导入或错误 Jackson 主版本声明；旧第三方桥接的反射类名保留。
- [ ] 对没有自动化测试、依赖缺失或外部服务未验证项明确保留未完成状态。

## 证据与状态

状态：适配进行中。编译成功、依赖静态检查或零测试执行均不能单独证明适配完成。

参考：[Shiro 迁移指南](https://shiro.apache.org/migration-guide.html)、[Jackson 3 迁移指南](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md)。

## 本次执行证据（2026-11-17）

声明/导入静态检查：**通过**；分支：`feature/3.0.x`；目标 Java：`21`。

Maven 测试流程退出码：`1`。完整日志：[maven-test.log](evidence/maven-test.log)；结构化结果：[verification.json](evidence/verification.json)。


旧 CSRFGuard 的 Javax 边界已迁移为原生 Jakarta 依赖及直接注册；当前针对性测试覆盖 token、配置和监听生命周期。完整 Maven 构建与官方发布制品依赖图仍未验证。

当前执行环境无法解析 Maven Central 域名，且所需缓存不完整。公共 shiro-extension 的依赖/本地 SNAPSHOT 父 POM 缺失使大量下游模块尚未进入编译；不得把此状态判为适配成功。

## 续接修复：原生 Jakarta CSRFGuard

采用官方 `org.owasp:csrfguard:4.5.0-jakarta` 和同版本 `csrfguard-extension-session`；将 Servlet、过滤器、会话监听直接注册为 Jakarta 类型。新增有效 token 放行、无 token/错误 token 拒绝与显式禁用场景测试。配置输出必须是可由 `Properties.getProperty` 读取的字符串，默认空值不能导致初始化失败。

CSRFGuard 4 要求至少一个拒绝动作；默认启用时使用 Error 动作返回 HTTP 403。显式设置自定义 actions 时继续采用用户配置，非法配置应在启动时失败。

ServletContext 初始化由 Spring 配置维持同一事实源，不再让上游监听器读取外部 Owasp.CsrfGuard.properties 覆盖配置。首次加载前按实际 WebApplicationContext 展开 `%servletContext%`，保留配置打印行为。

## 最新验收状态

Java 21 下核心/配置/Spring 生命周期测试合并执行 **11/11 通过**。覆盖无 token 与错误 token 返回 403、合法 token 放行、显式禁用、配置字符串/空值、非法配置阻止启动、会话 token 生成、ServletContext 不读取外部文件覆盖 Spring 配置、context path 展开，以及先请求监听 Bean 时先创建 csrfGuard。监听 Bean 显式依赖 csrfGuard。

证据：[resume-focused-tests.log](evidence/resume-focused-tests.log)、[执行命令和依赖](evidence/resume-focused-tests.json)。上游源码来自已校验 SHA 的官方 4.5.0-jakarta 标签，共 67 个 Java 文件在本地编译；这不等于测试了 Maven Central 发布 JAR。离线使用 Gson 2.13.2（上游声明 2.12.1）、Spring Boot 4.0.7（项目声明 4.0.8）；完整目标依赖图必须另行复验。

## 2026-11-18 全量矩阵复验

完整 Maven 验收仍停在依赖解析阶段。缺少正式 CSRFGuard 4.5.0-jakarta JAR，两线各有 11 项上游源码构建验证通过。

[Maven 验证日志与依赖树](evidence/maven-verify-shiro-3.0.2.log)。
