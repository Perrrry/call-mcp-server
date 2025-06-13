// 声明当前类所在的包路径（包名需与pom.xml中的groupId+artifactId对应）
package com.ahucoding.rocket.callmcpserver;

// 导入Spring Boot的核心启动类
import org.springframework.boot.SpringApplication;
// 导入Spring Boot的自动配置注解
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 主应用类 - Spring Boot应用的启动入口
 * 使用@SpringBootApplication注解标记这是Spring Boot的主配置类
 */
@SpringBootApplication // 复合注解，包含：
// 1. @Configuration（标记为配置类）
// 2. @EnableAutoConfiguration（启用自动配置）
// 3. @ComponentScan（自动扫描当前包及子包的组件）
public class CallMcpServerApplication {

    /**
     * 主方法 - 应用启动入口
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 启动Spring Boot应用：
        // 1. 创建Spring应用上下文
        // 2. 加载所有自动配置的Bean
        // 3. 启动内嵌Tomcat/Jetty服务器（因为依赖了spring-boot-starter-web）
        SpringApplication.run(CallMcpServerApplication.class, args);
    }
}