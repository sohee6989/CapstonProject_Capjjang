package capston.capston_spring.controller;

import capston.capston_spring.dto.ChallengeSessionDto;
import capston.capston_spring.dto.ChallengeSessionResponse; // 0403 추가: 수정된 응답 DTO 임포트
import capston.capston_spring.dto.CustomUserDetails;
import capston.capston_spring.entity.ChallengeSession;
import capston.capston_spring.service.ChallengeSessionService;
import capston.capston_spring.service.SongService;
import capston.capston_spring.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/challenge-session")
public class ChallengeSessionController {

    // 기존 ChallengeSession 대신 ChallengeSessionResponse로 응답 형식 통일

    private final ChallengeSessionService challengeSessionService;
    private final UserService userService;
    private final SongService songService;

    // 사용자 챌린지 세션 조회- 0403 수정: 기존 ChallengeSession 대신 ChallengeSessionResponse로 응답 형식 통일
    @GetMapping("/user/me")
    public ResponseEntity<List<ChallengeSessionResponse>> getByAuthenticatedUser(@AuthenticationPrincipal CustomUserDetails user) {
        String username = user.getUsername();
        List<ChallengeSession> sessions = challengeSessionService.getByUsername(username);
        List<ChallengeSessionResponse> responseList = sessions.stream()
                .map(ChallengeSessionResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    /** 특정 사용자 + 특정 곡 챌린지 세션 조회- 0403 수정 **/
    @GetMapping("/song/{songId}/user/me")
    public ResponseEntity<List<ChallengeSessionResponse>> getByUserAndSong(
            @PathVariable Long songId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        String username = user.getUsername();
        List<ChallengeSession> sessions = challengeSessionService.getByUsernameAndSongId(username, songId);
        List<ChallengeSessionResponse> responseList = sessions.stream()
                .map(ChallengeSessionResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    /*
    // 0403 수정: /highlight/{songId}에서 자동으로 DB에 저장되도록 함
    // 챌린지 세션 저장 (DTO 기반)
    @PostMapping("/save") // 수정된 부분: URI에 /save 추가
    public ResponseEntity<ChallengeSession> saveChallengeSession(
            @RequestBody ChallengeSessionDto sessionDto,
            @AuthenticationPrincipal User user
    ) {
        String username = user.getUsername();
        return ResponseEntity.ok(challengeSessionService.saveChallengeSession(sessionDto, username));
    }
    */


    /** 챌린지 모드 배경 불러오기 **/
    @GetMapping("/background/{songId}")
    public ResponseEntity<String> getChallengeBackground(@PathVariable Long songId) {
        return ResponseEntity.ok(challengeSessionService.getChallengeBackground(songId));
    }

    /** 0403 수정: 챌린지 모드에서 사용할 노래 하이라이트 부분 가져오기 **/
    @GetMapping("/highlight/{songId}")
    public ResponseEntity<String> getHighlightPart(
            @PathVariable Long songId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        String username = user.getUsername();
        return ResponseEntity.ok(challengeSessionService.getHighlightPart(songId, username));
    }
}
