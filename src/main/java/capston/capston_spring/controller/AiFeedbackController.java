package capston.capston_spring.controller;

import capston.capston_spring.service.AccuracySessionService;
import capston.capston_spring.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class AiFeedbackController {

    private final OpenAiService openAiService;

    // 세션 기반 GPT 피드백용 서비스 주입
    private final AccuracySessionService accuracySessionService;

    /**
     * 이미지 기반 GPT 피드백 요청 엔드포인트(테스트용)
     *
     * @param userImagePath   사용자 이미지 경로 (Base64로 변환할 파일 경로)
     * @param expertImagePath 전문가 이미지 경로 (Base64로 변환할 파일 경로)
     */
    @GetMapping("/api/image-feedback")
    public Mono<String> imageFeedback(
            @RequestParam String userImagePath,
            @RequestParam String expertImagePath
    ) {
        return openAiService.getDanceImageFeedback(userImagePath, expertImagePath);
    }

    /**
     * 세션 기반 GPT 피드백 요청 엔드포인트
     *
     * @param sessionId 세션 ID
     * @return 프레임별 피드백 리스트
     */
    @GetMapping("/api/low-score-feedback")
    public ResponseEntity<List<String>> getLowScoreFeedback(@RequestParam Long sessionId) {
        List<String> feedbacks = accuracySessionService.generateLowScoreFeedback(sessionId);
        return ResponseEntity.ok(feedbacks);
    }
}
