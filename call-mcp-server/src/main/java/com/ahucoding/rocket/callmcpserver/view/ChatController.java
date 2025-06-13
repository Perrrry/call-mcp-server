// 声明当前控制器类所在的包路径
package com.ahucoding.rocket.callmcpserver.view;

// 导入阿里云DashScope聊天选项类
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
// 导入MCP同步客户端接口
import io.modelcontextprotocol.client.McpSyncClient;
// 导入Jakarta Servlet响应类
import jakarta.servlet.http.HttpServletResponse;
// 导入Spring AI相关类
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
// 导入Spring框架相关注解
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
// 导入响应式流类
import reactor.core.publisher.Flux;

// 导入集合类
import java.util.List;

/**
 * 聊天控制器 - 提供AI聊天相关API
 * @author jianzhang
 * 2025/03/18/下午8:00
 */
@RestController // 标识这是一个Spring MVC控制器，返回数据直接写入响应体
@RequestMapping("/dashscope/chat-client") // 定义控制器的基础路径
public class ChatController {

    // 聊天客户端实例
    private final ChatClient chatClient;
    // 聊天记忆（内存实现）
    private final ChatMemory chatMemory = new InMemoryChatMemory();

    /**
     * 构造函数 - 通过依赖注入初始化聊天客户端
     * @param chatClientBuilder 聊天客户端构建器
     * @param mcpSyncClients MCP同步客户端列表
     * @param tools 工具回调提供者
     */
    public ChatController(ChatClient.Builder chatClientBuilder,
                          List<McpSyncClient> mcpSyncClients,
                          ToolCallbackProvider tools) {
        // 构建聊天客户端实例：
        // 1. 设置默认工具回调
        // 2. 设置默认选项（使用DashScope，topP=0.7）
        this.chatClient = chatClientBuilder
                .defaultTools(tools)
                .defaultOptions(DashScopeChatOptions.builder().withTopP(0.7).build())
                .build();
    }

    /**
     * 流式生成聊天响应
     * @param response HTTP响应对象
     * @param id 会话ID
     * @param prompt 用户输入提示
     * @return 聊天响应流
     */
    @RequestMapping(value = "/generate_stream", method = RequestMethod.GET)
    public Flux<ChatResponse> generateStream(
            HttpServletResponse response,
            @RequestParam("id") String id,
            @RequestParam("prompt") String prompt) {
        // 设置响应编码
        response.setCharacterEncoding("UTF-8");
        // 创建消息记忆顾问（保留最近10条消息）
        var messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemory, id, 10);
        // 使用聊天客户端处理提示，并返回流式响应
        return this.chatClient.prompt(prompt)
                .advisors(messageChatMemoryAdvisor)
                .stream()
                .chatResponse();
    }

    /**
     * 带记忆顾问的聊天接口
     * @param response HTTP响应对象
     * @param id 会话ID
     * @param prompt 用户输入提示
     * @return 聊天内容流
     */
    @GetMapping("/advisor/chat/{id}/{prompt}")
    public Flux<String> advisorChat(
            HttpServletResponse response,
            @PathVariable String id,
            @PathVariable String prompt) {
        // 设置响应编码
        response.setCharacterEncoding("UTF-8");
        // 创建消息记忆顾问（保留最近10条消息）
        var messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemory, id, 10);
        // 使用聊天客户端处理提示，返回纯内容流
        return this.chatClient.prompt(prompt)
                .advisors(messageChatMemoryAdvisor)
                .stream()
                .content();
    }
}