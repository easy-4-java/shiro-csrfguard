# shiro-csrfguard

Apache Shiro CSRF Guard extensions

## 项目简介

本仓库提供 Apache Shiro **csrfguard** 扩展组件，拆分为两个模块：

- `shiro-csrfguard-core` — 与框架无关的令牌、Realm、异常、工具类（不依赖 Spring Boot）。
- `shiro-csrfguard-spring` — 基于 core 模块之上的 Spring Web 过滤器、处理器、仓库与配置装配。

可直接被任何使用 Apache Shiro 的应用使用，无需依赖 Spring Boot 自动装配机制。

## 构建

```bash
./mvnw clean install
```

## Maven 坐标

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>shiro-csrfguard-core</artifactId>
    <version>3.0.x.20260630-SNAPSHOT</version>
</dependency>
```

## 许可证

Apache License 2.0
