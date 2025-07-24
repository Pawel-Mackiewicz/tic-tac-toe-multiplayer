package info.mackiewicz.tictactoemultiplayer.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * Configures and provides a customized {@link ObjectMapper} bean.
     *
     * The returned ObjectMapper is configured to fail on unknown properties during deserialization
     * and accept case-insensitive enums.
     *
     * @return a pre-configured {@link ObjectMapper} instance for JSON processing
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build();
    }
}
