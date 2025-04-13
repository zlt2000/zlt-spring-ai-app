package org.zlt.alibaba.chat;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author: zlt
 * @date: 2025/4/12
 * <p>
 * Blog: https://my.oschina.net/zlt2000
 * Github: https://github.com/zlt2000
 */
@RestController
@RequestMapping("/tool")
public class ToolController {
    private final ChatClient chatClient;
    private ChatMemory chatMemory;
    private MessageChatMemoryAdvisor messageChatMemoryAdvisor;

    public ToolController(ChatClient.Builder builder) {
        chatMemory = new InMemoryChatMemory();
        messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemory);
        this.chatClient = builder
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .withModel("qwen-max")
                                //.withModel("deepseek-v3")
                                .build()
                )
                .build();
    }

    @GetMapping(value = "/chat")
    public String chat(@RequestParam String input, String sessionId, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");

        return chatClient.prompt().user(input)
                .tools(new TestTools())
                .advisors(messageChatMemoryAdvisor)
                .advisors(spec -> spec
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId))
                .call()
                .content();
    }

    public static class TestTools {
        @Tool(description = "获取今天日期")
        String getCurrentDate() {
            System.out.println("======getCurrentDate");
            return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
        }

        @Tool(description = "获取当前温度")
        String getCurrentTemperature(MyToolReques  toolReques) {
            System.out.println("======getCurrentTemperature: " + toolReques.localName + "__" + toolReques.date);
            return toolReques.date + toolReques.localName + "温度为20摄氏度";
        }

        public record MyToolReques(String localName, String date) {}
    }
}
