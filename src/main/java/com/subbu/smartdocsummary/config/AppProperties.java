package com.subbu.smartdocsummary.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Rag rag = new Rag();
    private Llm llm = new Llm();
    private Vectorstore vectorstore = new Vectorstore();

    @Data
    public static class Rag {
        private int chunkSize = 500;
        private int overlap = 50;
    }

    @Data
    public static class Llm {
        private String provider = "ollama";
    }

    @Data
    public static class Vectorstore {
        // Add fields as needed
    }
}
