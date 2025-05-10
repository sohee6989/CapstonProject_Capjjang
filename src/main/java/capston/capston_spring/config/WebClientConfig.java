package capston.capston_spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient openAiWebClient() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null) {
            // VM 옵션으로 전달된 속성도 같이 확인
            apiKey = System.getProperty("OPENAI_API_KEY");
        }
        if (apiKey == null) {
            throw new IllegalStateException("OPENAI_API_KEY 환경변수가 설정되지 않았습니다.");
        }

        return WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

}
