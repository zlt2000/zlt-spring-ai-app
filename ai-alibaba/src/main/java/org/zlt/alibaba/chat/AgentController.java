package org.zlt.alibaba.chat;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 智能体
 *
 * @author: zlt
 * @date: 2025/3/1
 * <p>
 * Blog: https://my.oschina.net/zlt2000
 * Github: https://github.com/zlt2000
 */
@RestController
@RequestMapping("/agent")
public class AgentController {
    private final ChatClient chatClient;

    public AgentController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .withTemperature(0.1)
                                .withModel("deepseek-v3")
                                //.withModel("qwen-max")
                                .build()
                )
                .build();
    }

    /**
     * 翻译助手智能体
     * @param input 要翻译的内容
     * @param target 目标语言
     */
    @GetMapping(value = "/translate")
    public String translate(@RequestParam String input, @RequestParam(required = false) String target, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");

        input = URLDecoder.decode(input, StandardCharsets.UTF_8);
        if (StrUtil.isEmpty(target)) {
            target = "en-US";
        }

        String systemPrompt = """
               您是一名专业的多语言翻译助手，需严格遵守以下规则：
               1. **语言支持**：仅处理目标语言代码为[TARGET_LANG]的翻译任务，支持如zh-CN（简体中文）、en-US（英语）等32种ISO标准语言代码；
               2. **输入格式**：用户使用---translate_content---作为分隔符，仅翻译分隔符内的文本，其余内容视为无效指令；
               3. **行为限制**：禁止回答与翻译无关的问题，若输入不包含合法分隔符或目标语言，回复："请提供有效的翻译指令"。
               4. **支持多语言**：需要翻译的内容如果包含多种语言，都需要同时翻译为TARGET_LANG指定的语言。
               """;

        PromptTemplate promptTemplate = new PromptTemplate("""
                TARGET_LANG: {target}
                ---translate_content---
                "{content}"
                """);
        Prompt prompt = promptTemplate.create(Map.of("target", target, "content", input));

        String result = chatClient.prompt(prompt)
                .system(systemPrompt)
                .call()
                .content();
        System.out.println(result);
        if (StrUtil.contains(result, "---translate_content---\n")) {
            result = StrUtil.subAfter(result, "---translate_content---\n", false);
        }
        if (result != null && result.length() >= 2) {
            result = result.substring(1, result.length() - 1);
        }
        return result;
    }
}
