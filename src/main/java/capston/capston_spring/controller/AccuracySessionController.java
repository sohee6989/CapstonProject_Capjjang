package capston.capston_spring.controller;

import capston.capston_spring.dto.AccuracySessionDto;
import capston.capston_spring.dto.AccuracySessionResponse;
import capston.capston_spring.dto.CustomUserDetails;
import capston.capston_spring.entity.AccuracySession;
import capston.capston_spring.service.AccuracySessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeAndSaveSession(
            @AuthenticationPrincipal CustomUserDetails user, // userId 대신 토큰에서 유저 정보
            @RequestParam Long songId,
            @RequestParam String videoPath
    ) {
        try {
            String username = user.getUsername(); // username 추출
            return ResponseEntity.ok(
                    accuracySessionService.analyzeAndSaveSessionByUsername(username, songId, videoPath) // 수정된 서비스 메서드 호출
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
    @GetMapping("/{sessionId}/result")
    public ResponseEntity<?> getSessionResult(@PathVariable Long sessionId) {
        try {
            return accuracySessionService.getSessionById(sessionId)
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

    /** 1절 정확도 연습 시작 - 세션 객체 반환 **/
    @PostMapping("/full")
    public ResponseEntity<?> startFullAccuracySession(@AuthenticationPrincipal CustomUserDetails user,
                                                      @RequestParam Long songId) {
        try {
            String username = user.getUsername();
            AccuracySession session = accuracySessionService.startAccuracySessionByUsername(username, songId, "full");
            return ResponseEntity.ok(AccuracySessionResponse.fromEntity(session));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error"));
        }
    }

    /** 하이라이트 정확도 연습 시작 - 세션 객체 반환 **/
    @PostMapping("/highlight")
    public ResponseEntity<?> startHighlightAccuracySession(@AuthenticationPrincipal CustomUserDetails user,
                                                           @RequestParam Long songId) {
        try {
            String username = user.getUsername();
            AccuracySession session = accuracySessionService.startAccuracySessionByUsername(username, songId, "highlight");
            return ResponseEntity.ok(AccuracySessionResponse.fromEntity(session));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error"));
        }
    }
}