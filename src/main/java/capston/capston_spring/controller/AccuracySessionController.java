package capston.capston_spring.controller;

import capston.capston_spring.dto.AccuracySessionDto;
import capston.capston_spring.dto.AccuracySessionResponse;
import capston.capston_spring.dto.CustomUserDetails;
import capston.capston_spring.entity.AccuracySession;
import capston.capston_spring.service.AccuracySessionService;
import capston.capston_spring.utils.FrameIndexCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/accuracy-session")
@RequiredArgsConstructor
public class AccuracySessionController {

    private final AccuracySessionService accuracySessionService;

    /** 인증된 사용자 정확도 세션 전체 조회 **/
    @GetMapping("/user/me")
    public ResponseEntity<?> getByUsername(@AuthenticationPrincipal CustomUserDetails user) {
        try {
            String username = user.getUsername();
            return ResponseEntity.ok(accuracySessionService.getByUsername(username));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error"));
        }
    }

    /** 인증된 사용자 + 특정 곡 정확도 세션 조회 **/
    @GetMapping("/song/{songId}/user/me")
    public ResponseEntity<?> getBySongAndAuthenticatedUser(@PathVariable Long songId,
                                                           @AuthenticationPrincipal CustomUserDetails user) {
        try {
            String username = user.getUsername();
            return ResponseEntity.ok(accuracySessionService.getBySongAndUsername(songId, username));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error"));
        }
    }

    /** Mediapipe 기반 점수 평가 실행 후 결과 저장 (Flask 연동) **/
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzeAndSaveSession(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long songId,
            @RequestParam Long sessionId,
            @RequestPart MultipartFile image
    ) {
        try {
            AccuracySession session = accuracySessionService.getSessionByCustomSessionId(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid session ID"));

            int frameIndex = FrameIndexCalculator.calculateFrameIndex(session.getStartTime());

            return ResponseEntity.ok(
                    accuracySessionService.analyzeAndStoreFrameStep(user.getUsername(), songId, sessionId, frameIndex, image) // 수정된 서비스 메서드 호출
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to analyze and save session"));
        }
    }

    /** AccuracySessionDto 기반 세션 저장 **/
    @PostMapping("/save")
    public ResponseEntity<?> saveSessionFromDto(@AuthenticationPrincipal CustomUserDetails user,
                                                @RequestBody AccuracySessionDto dto) {
        try {
            String username = user.getUsername();
            AccuracySession session = accuracySessionService.saveSessionFromDto(username, dto);
            return ResponseEntity.ok(AccuracySessionResponse.fromEntity(session));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to save accuracy session"));
        }
    }

    /** (특정 세션 ID로) 정확도 세션 상세 조회 **/
    // 수정된 부분: @PathVariable -> @RequestParam
    @GetMapping("/result")  // 변경된 부분: 경로에서 /{sessionId}/result -> /result로 변경
    public ResponseEntity<?> getSessionResult(@RequestParam Long sessionId) {  // 변경된 부분: sessionId를 쿼리 파라미터로 받기
        try {
            return accuracySessionService.getSessionByCustomSessionId(sessionId)
                    .map(session -> ResponseEntity.ok(
                            AccuracySessionResponse.fromEntity(session)
                    ))
                    .orElseGet(() ->
                            ResponseEntity.status(404).<AccuracySessionResponse>body(null)
                    );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error"));
        }
    }

    /** 곡 제목으로 실루엣 영상 경로 반환 **/
    @GetMapping("/video-paths")
    public ResponseEntity<?> getVideoPathsBySongTitle(@RequestParam("songName") String songName) {
        try {
            return ResponseEntity.ok(accuracySessionService.getVideoPathsBySongTitle(songName));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error"));
        }
    }

    /** 1절 정확도 연습 시작 - 세션 객체 리스트 반환 (0414 수정됨) **/
    @PostMapping("/full")
    public ResponseEntity<?> startFullAccuracySession(@AuthenticationPrincipal CustomUserDetails user,
                                                      @RequestParam Long songId,
                                                      @RequestParam Long sessionId) {
        try {
            String username = user.getUsername();
            AccuracySession session = accuracySessionService.startAccuracySession(username, songId, "full",sessionId); // 서비스 메서드 변경됨
            return ResponseEntity.ok(AccuracySessionResponse.fromEntity(session));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error"));
        }
    }

    /** 하이라이트 정확도 연습 시작 - 세션 객체 리스트 반환 (0414 수정됨) **/
    @PostMapping("/highlight")
    public ResponseEntity<?> startHighlightAccuracySession(@AuthenticationPrincipal CustomUserDetails user,
                                                           @RequestParam  Long songId,
                                                           @RequestParam Long sessionId) {
        try {
            String username = user.getUsername();
            AccuracySession session = accuracySessionService.startAccuracySession(username, songId, "highlight",sessionId); // 서비스 메서드 변경됨
            return ResponseEntity.ok(AccuracySessionResponse.fromEntity(session));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error"));
        }
    }
}
