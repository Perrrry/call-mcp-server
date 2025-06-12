package com.ahucoding.rocket.callmcpserver.cfg;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * @author jianzhang
 * 2025/03/18/下午8:02
 */
@Configuration
public class McpClientCfg implements McpSyncClientCustomizer {

    @Bean
    public ToolCallbackProvider toolCallbackProvider(List<McpSyncClient> mcpSyncClients) {
        return new SyncMcpToolCallbackProvider(mcpSyncClients);
    }

    @Override
    public void customize(String name, McpClient.SyncSpec spec) {
        // do nothing
        spec.requestTimeout(Duration.ofSeconds(30));
    }
}