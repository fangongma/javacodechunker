package jp.co.jri.codechunker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register JavaTimeModule for Java 8 Date/Time API support
        mapper.registerModule(new JavaTimeModule());

        // Pretty print JSON
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Write dates as ISO-8601 strings
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Ignore null values
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        return mapper;
    }
}
