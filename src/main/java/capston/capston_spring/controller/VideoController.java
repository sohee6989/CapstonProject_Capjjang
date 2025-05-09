package capston.capston_spring.controller;

import capston.capston_spring.dto.CustomUserDetails;
import capston.capston_spring.dto.MyVideoResponse;
import capston.capston_spring.dto.RecordedVideoDto;
import capston.capston_spring.entity.RecordedVideo;
import capston.capston_spring.entity.VideoMode;
import capston.capston_spring.exception.VideoNotFoundException;
import capston.capston_spring.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/recorded-video")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;


    /** 사용자의 녹화 영상 조회 (모드별 조회 가능) **/
    @GetMapping("/user/me") // URI 수정
    public ResponseEntity<List<MyVideoResponse>> getVideosByAuthenticatedUser(
            @AuthenticationPrincipal CustomUserDetails user, // 토큰에서 username 추출
            @RequestParam(name = "mode", required = false) VideoMode mode
    ) {
        String username = user.getUsername(); // username 추출
        return ResponseEntity.ok(
                mode == null
                        ? videoService.getAllUserVideosByUsername(username) // 새로운 서비스 메서드
                        : videoService.getVideosByModeByUsername(username, mode) // 새로운 서비스 메서드
        );
    }

    /** 특정 비디오 조회 **/
    @GetMapping("/{videoId}")
    public ResponseEntity<MyVideoResponse> getMyVideo(@PathVariable("videoId") Long videoId) {
        return videoService.getVideoById(videoId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new VideoNotFoundException("Video not found with ID: " + videoId)); // 메시지 영어로 변경
    }

    /**
     * 특정 세션의 녹화 영상 조회 (연습, 챌린지, 정확도 포함)
     **/
    @GetMapping("/session") // 수정된 부분: {sessionId} -> /session?sessionId= 쿼리 파라미터로 수정
    public ResponseEntity<List<MyVideoResponse>> getVideosBySession(
            @RequestParam Long sessionId, // 수정된 부분: sessionId를 쿼리 파라미터로 받음
            @RequestParam VideoMode mode
    ) {
        return ResponseEntity.ok(videoService.getVideosBySession(sessionId, mode));
    }

    /** 녹화된 영상 저장 (DTO 기반) **/
    @PostMapping(value= "/saveVideo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecordedVideo> saveRecordedVideo(
            @RequestPart("file") MultipartFile file,
            @RequestPart("data") RecordedVideoDto recordedVideoDto,
            @AuthenticationPrincipal CustomUserDetails user // 추가: 토큰에서 username 추출
    ) {
        String username = user.getUsername(); // username 기반 저장
        return ResponseEntity.ok(videoService.saveRecordedVideo(recordedVideoDto, file, username));
    }

    /** 특정 비디오 수정 (재업로드) **/
    @PostMapping(value = "/{videoId}/edit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @PathVariable("videoId") Long videoId
    ) {
        if (file.isEmpty() || file.getSize() == 0) {
            return ResponseEntity.badRequest().body("Uploaded file is empty.");
        }
        return videoService.editVideo(videoId, file);
    }
}
