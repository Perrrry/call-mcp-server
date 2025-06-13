// 声明当前配置类所在的包路径
package com.ahucoding.rocket.callmcpserver.cfg;

// 导入MCP客户端相关接口和类
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
// 导入Spring AI MCP相关的定制器和工具类
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
// 导入Spring配置相关注解
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 导入Java时间相关类
import java.time.Duration;
// 导入集合类
import java.util.List;

/**
 * MCP客户端配置类
 * @author jianzhang
 * 2025/03/18/下午8:02
 */
@Configuration // 标识这是一个Spring配置类，Spring会扫描并加载其中的Bean定义
public class McpClientCfg implements McpSyncClientCustomizer { // 实现MCP同步客户端定制器接口

    /**
     * 定义ToolCallbackProvider Bean
     * @param mcpSyncClients 自动注入的所有McpSyncClient实例
     * @return 同步MCP工具回调提供者
     */
    @Bean // 声明这是一个Spring Bean，会被IoC容器管理
    public ToolCallbackProvider toolCallbackProvider(List<McpSyncClient> mcpSyncClients) {
        // 创建并返回一个同步MCP工具回调提供者，传入所有MCP同步客户端实例
        return new SyncMcpToolCallbackProvider(mcpSyncClients);
    }

    /**
     * 实现McpSyncClientCustomizer接口的定制方法
     * @param name 客户端名称
     * @param spec 客户端配置规范
     */
    @Override
    public void customize(String name, McpClient.SyncSpec spec) {
        // 这里可以添加对特定客户端的定制逻辑
        // 当前实现只是设置了请求超时时间为30秒
        spec.requestTimeout(Duration.ofSeconds(30));
    }
}