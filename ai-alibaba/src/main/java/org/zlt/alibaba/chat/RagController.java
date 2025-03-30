package org.zlt.alibaba.chat;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author: zlt
 * @date: 2025/3/29
 * <p>
 * Blog: https://my.oschina.net/zlt2000
 * Github: https://github.com/zlt2000
 */
@RestController
@RequestMapping("/rag")
public class RagController {
    private final ChatClient chatClient;

    public RagController(ChatClient.Builder builder, VectorStore vectorStore) {
        String sysPrompt = """
                您是一个智能搜索引擎，负责根据用户输入的内容进行精准匹配、模糊匹配和近义词匹配，以搜索相关的数据记录。
                您只能搜索指定的内容，不能回复其他内容或添加解释。
                您可以通过[search_content]标识符来表示需要搜索的具体内容。要求您返回匹配内容的完整记录，以JSON数组格式呈现。
                如果搜索不到内容，请返回[no_data]。
                """;
        this.chatClient = builder
                .defaultSystem(sysPrompt)
                .defaultAdvisors(
                        new QuestionAnswerAdvisor(vectorStore, new SearchRequest())
                )
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .withModel("deepseek-r1")
                                .build()
                )
                .build();
    }

    @GetMapping(value = "/search")
    public List<SearchVo> search(@RequestParam String search, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        PromptTemplate promptTemplate = new PromptTemplate("[search_content]: {search}");
        Prompt prompt = promptTemplate.create(Map.of("search", search));

        return chatClient.prompt(prompt)
                .call()
                .entity(new ParameterizedTypeReference<List<SearchVo>>() {});
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class SearchVo {
        private String type;
        private String name;
        private String topic;
        private String industry;
        private String remark;
    }
}
