package org.zlt.alibaba.chat;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

/**
 * @author: zlt
 * @date: 2025/2/21
 * <p>
 * Blog: https://my.oschina.net/zlt2000
 * Github: https://github.com/zlt2000
 */
@RestController
@RequestMapping("/ai")
public class ChatController {
    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        String sysPrompt = """
            你是一个博学的智能聊天助手，请根据用户提问回答。
            请讲中文。
            今天的日期是 {current_date}。
            """;
        this.chatClient = builder
                .defaultSystem(sysPrompt)
                // 设置 ChatClient 中 ChatModel 的 Options 参数
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                /**
                                 * 用于控制随机性和多样性的程度。具体来说，temperature值控制了生成文本时对每个候选词的概率分布进行平滑的程度。较高的temperature值会降低概率分布的峰值，使得更多的低概率词被选择，生成结果更加多样化；而较低的temperature值则会增强概率分布的峰值，使得高概率词更容易被选择，生成结果更加确定。
                                 * 值范围：[0, 2)，系统默认值0.85。不建议取值为0，无意义
                                 */
                                .withTemperature(1.3)
                                .withModel("deepseek-v3")
                                .build()
                )
                .build();
    }

    /**
     * 普通对话，动态切换模型
     * @param input 输入内容
     * @param model 模型名称
     */
    @GetMapping(value = "/chat")
    public String chat(@RequestParam String input, @RequestParam(required = false) String model, HttpServletResponse response) {
        // 设置字符编码，避免乱码
        response.setCharacterEncoding("UTF-8");

        if (StrUtil.isEmpty(model)) {
            model = "deepseek-v3";
        }
        input = URLDecoder.decode(input, StandardCharsets.UTF_8);
        return chatClient.prompt().user(input)
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .options(DashScopeChatOptions.builder().withModel(model).build())
                .call()
                .content();
    }

    /**
     * 使用提示词模板 PromptTemplate
     */
    @GetMapping(value = "/chatTemp")
    public String chatTemp(@RequestParam String input, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        input = URLDecoder.decode(input, StandardCharsets.UTF_8);
        // 使用PromptTemplate定义提示词模板
        PromptTemplate promptTemplate = new PromptTemplate("请逐步解释你的思考过程: {input}");
        Prompt prompt = promptTemplate.create(Map.of("input", input));

        return chatClient.prompt(prompt)
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .call()
                .content();
    }

    /**
     * 流式对话
     */
    @GetMapping(value = "/streamChat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam String input, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        input = URLDecoder.decode(input, StandardCharsets.UTF_8);
        // 使用PromptTemplate定义提示词模板
        PromptTemplate promptTemplate = new PromptTemplate("请逐步解释你的思考过程: {input}");
        Prompt prompt = promptTemplate.create(Map.of("input", input));

        return chatClient.prompt(prompt)
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .stream()
                .content()
                // 添加终止标识和错误处理
                .concatWith(Flux.just("[DONE]"))
                // 异常时也发送终止
                .onErrorResume(e -> Flux.just("ERROR: " + e.getMessage(), "[DONE]"));
    }
}
