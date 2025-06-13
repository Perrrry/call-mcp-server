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
// 导入日志相关
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// 导入SSE相关
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;

// 导入集合类
import java.util.List;
import java.io.IOException;
import java.time.Duration;

/**
 * 聊天控制器 - 提供AI聊天相关API
 * @author jianzhang
 * 2025/03/18/下午8:00
 */
@RestController // 标识这是一个Spring MVC控制器，返回数据直接写入响应体
@RequestMapping("/dashscope/chat-client") // 定义控制器的基础路径
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

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
     * 流式生成聊天响应 - 使用SseEmitter实现更稳定的SSE
     * @param response HTTP响应对象
     * @param id 会话ID
     * @param prompt 用户输入提示
     * @return SSE发射器
     */
    @RequestMapping(value = "/generate_stream", method = RequestMethod.GET)
    public SseEmitter generateStreamSse(
            HttpServletResponse response,
            @RequestParam("id") String id,
            @RequestParam("prompt") String prompt) {

        // 设置响应头
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Access-Control-Allow-Origin", "*");

        // 创建SSE发射器，设置超时时间为5分钟
        SseEmitter emitter = new SseEmitter(300000L);

        // 创建消息记忆顾问（保留最近10条消息）
        var messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemory, id, 10);

        try {
            // 发送连接成功消息
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("连接成功"));

            // 获取聊天响应流
            Flux<ChatResponse> chatResponseFlux = this.chatClient.prompt(prompt)
                    .advisors(messageChatMemoryAdvisor)
                    .stream()
                    .chatResponse();

            // 订阅流并发送数据
            chatResponseFlux
                    .doOnNext(chatResponse -> {
                        try {
                            // 发送聊天响应数据
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(chatResponse));
                        } catch (IOException e) {
                            logger.debug("客户端连接已断开: {}", e.getMessage());
                            emitter.completeWithError(e);
                        }
                    })
                    .doOnComplete(() -> {
                        try {
                            // 发送完成信号
                            emitter.send(SseEmitter.event()
                                    .name("complete")
                                    .data("流式响应完成"));
                            emitter.complete();
                        } catch (IOException e) {
                            logger.debug("发送完成信号时客户端已断开: {}", e.getMessage());
                        }
                    })
                    .doOnError(throwable -> {
                        logger.error("聊天流处理错误 - 会话ID: {}", id, throwable);
                        emitter.completeWithError(throwable);
                    })
                    .subscribe();

        } catch (IOException e) {
            logger.error("初始化SSE连接失败: {}", e.getMessage());
            emitter.completeWithError(e);
        }

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            logger.info("SSE连接超时 - 会话ID: {}", id);
            emitter.complete();
        });

        emitter.onCompletion(() -> {
            logger.debug("SSE连接正常关闭 - 会话ID: {}", id);
        });

        emitter.onError(throwable -> {
            logger.debug("SSE连接异常关闭 - 会话ID: {}, 错误: {}", id, throwable.getMessage());
        });

        return emitter;
    }

    /**
     * 带记忆顾问的聊天接口 - 保持原有的Flux实现作为备选
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
        // 设置响应编码和头部
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Access-Control-Allow-Origin", "*");

        // 创建消息记忆顾问（保留最近10条消息）
        var messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemory, id, 10);

        // 使用聊天客户端处理提示，返回纯内容流
        return this.chatClient.prompt(prompt)
                .advisors(messageChatMemoryAdvisor)
                .stream()
                .content()
                .timeout(Duration.ofMinutes(5)) // 设置超时
                .doOnNext(content -> logger.debug("发送内容: {}", content))
                .doOnComplete(() -> logger.debug("流式响应完成 - 会话ID: {}", id))
                .doOnError(throwable -> logger.error("聊天流处理错误 - 会话ID: {}", id, throwable));
    }
}