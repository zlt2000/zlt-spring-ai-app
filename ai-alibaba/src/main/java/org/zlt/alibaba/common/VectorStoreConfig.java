package org.zlt.alibaba.common;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * @author: zlt
 * @date: 2025/3/29
 * <p>
 * Blog: https://my.oschina.net/zlt2000
 * Github: https://github.com/zlt2000
 */
@Configuration
public class VectorStoreConfig {
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel
            , @Value("classpath:rag/data-resources.txt") Resource docs) {
        VectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        vectorStore.write(new TokenTextSplitter().transform(new TextReader(docs).read()));
        return vectorStore;
    }
}
