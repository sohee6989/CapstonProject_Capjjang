package capston.capston_spring.controller;

import capston.capston_spring.dto.CustomUserDetails;
import capston.capston_spring.dto.PracticeSessionResponse;
import capston.capston_spring.entity.PracticeSession;
import capston.capston_spring.service.PracticeSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/practice-session")
@RequiredArgsConstructor
public class PracticeSessionController {

    private final PracticeSessionService practiceSessionService;

    /** 전체 연습 세션 조회 - DTO 변환 적용 **/
    @GetMapping("/user/me")
    public ResponseEntity<List<PracticeSessionResponse>> getMyPracticeSessions(@AuthenticationPrincipal CustomUserDetails user) {
        String username = user.getUsername();

        // 기존 PracticeSession 리스트 조회
        List<PracticeSession> sessions = practiceSessionService.getByUsername(username);

        // DTO로 변환
        List<PracticeSessionResponse> response = sessions.stream()
                .map(PracticeSessionResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 곡에 대한 사용자의 연습 세션 조회 - DTO 변환 적용
     **/
    @GetMapping("/song/{songId}/user/me")
    public ResponseEntity<List<PracticeSessionResponse>> getBySongAndUser(@PathVariable Long songId,
                                                                          @AuthenticationPrincipal CustomUserDetails user) {
        String username = user.getUsername();

        // 기존 PracticeSession 리스트 조회
        List<PracticeSession> sessions = practiceSessionService.getBySongAndUsername(songId, username);

        // DTO로 변환
        List<PracticeSessionResponse> response = sessions.stream()
                .map(PracticeSessionResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 1절 연습 시작 - 세션 객체 반환
     **/
    @PostMapping("/full")
    public ResponseEntity<?> startFullPractice(@AuthenticationPrincipal CustomUserDetails user,
                                               @RequestParam Long songId) {
        try {
            String username = user.getUsername();
            PracticeSession session = practiceSessionService.startFullPracticeSessionByUsername(username, songId);
            return ResponseEntity.ok(PracticeSessionResponse.fromEntity(session));
        } catch (IllegalArgumentException e) { // 너가 직접 던진 예외만 받음
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error"));
        }
    }

    /**
     * 하이라이트 연습 시작 - 세션 객체 반환
     **/
    @PostMapping("/highlight")
    public ResponseEntity<?> startHighlightPractice(@AuthenticationPrincipal CustomUserDetails user,
                                                    @RequestParam Long songId) {
        try {
            String username = user.getUsername();
            PracticeSession session = practiceSessionService.startHighlightPracticeSessionByUsername(username, songId);
            return ResponseEntity.ok(PracticeSessionResponse.fromEntity(session));

        } catch (IllegalArgumentException e) {
            // 사용자 또는 곡이 없거나 mode가 잘못된 경우
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // 예기치 못한 서버 에러
            return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error"));
        }
    }
}