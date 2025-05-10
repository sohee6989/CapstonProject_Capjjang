package capston.capston_spring.service;

import capston.capston_spring.dto.ChatCompletionResponse;
import capston.capston_spring.exception.OpenAiApiException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private final WebClient openAiWebClient;

    public OpenAiService(WebClient openAiWebClient) {
        this.openAiWebClient = openAiWebClient;
    }

    //이미지 기반 GPT 피드백 메서드 (GPT-4o Vision)
    public Mono<String> getDanceImageFeedback(String userImagePath, String expertImagePath) {
        try {
            String userImageBase64 = encodeImage(userImagePath);
            String expertImageBase64 = encodeImage(expertImagePath);

            List<Map<String, Object>> messages = List.of(
                    Map.of(
                            "role", "system",
                            "content", "당신은 춤 연습을 돕는 긍정적이고 안전한 피드백 전문가입니다. 자세, 위치, 방향에 대해서만 피드백하며 외모나 민감한 내용은 절대 언급하지 마세요."
                    ),
                    Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of(
                                            "type", "image_url",
                                            "image_url", Map.of("url", "data:image/png;base64," + userImageBase64)
                                    ),
                                    Map.of(
                                            "type", "image_url",
                                            "image_url", Map.of("url", "data:image/png;base64," + expertImageBase64)
                                    ),
                                    Map.of(
                                            "type", "text",
                                            "text", "첫 번째 이미지는 그 동작을 학습한 사람이 따라 한 모습이고, 두 번째 이미지는 전문가가 춤을 추는 모습이야. 동작의 정확도, 팔/상체/하체의 각도 등을 기준으로 비교해줘. 학습자가 어떻게 수정하면 더 비슷해질 수 있는지 \"사용자가 쉽게 알아들을 수 있게\" 구체적으로 알려줘."
                                    )
                            )
                    )
            );

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4o",
                    "messages", messages,
                    "temperature", 0.0
            );

            return openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .map(response -> {
                        if (response.getChoices() == null || response.getChoices().isEmpty()) {
                            throw new OpenAiApiException("OpenAI 응답이 비어 있습니다.", 502);
                        }
                        return response.getChoices().get(0).getMessage().getContent();
                    })
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        String errorBody = ex.getResponseBodyAsString();
                        System.err.println("OpenAI API 오류: " + ex.getStatusCode() + " - " + errorBody);

                        String errorMessage = String.format(
                                "OpenAI API 호출 실패: [%d] %s",
                                ex.getRawStatusCode(),
                                errorBody != null ? errorBody : "응답 본문 없음"
                        );
                        return Mono.error(new OpenAiApiException(errorMessage, ex.getRawStatusCode()));
                    })
                    .onErrorResume(Exception.class, ex -> {
                        System.err.println("OpenAI API 호출 중 일반 오류: " + ex.getMessage());
                        return Mono.error(new OpenAiApiException("OpenAI API 호출 중 알 수 없는 오류가 발생했습니다.", 500));
                    });

        } catch (IOException e) {
            return Mono.error(new RuntimeException("이미지 인코딩 실패: " + e.getMessage()));
        }
    }

    // 이미지 Base64 인코딩 유틸 메서드
    private String encodeImage(String imagePath) throws IOException {
        Path path = Paths.get(imagePath);
        byte[] imageBytes = Files.readAllBytes(path);
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
