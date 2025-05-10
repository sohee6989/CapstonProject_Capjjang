package capston.capston_spring.service;

import capston.capston_spring.entity.AccuracyFrameEvaluation;
import capston.capston_spring.entity.AccuracySession;
import capston.capston_spring.entity.AppUser;
import capston.capston_spring.entity.Song;
import capston.capston_spring.exception.SessionNotFoundException;
import capston.capston_spring.exception.SongNotFoundException;
import capston.capston_spring.repository.AccuracyFrameEvaluationRepository;
import capston.capston_spring.repository.AccuracySessionRepository;
import capston.capston_spring.repository.SongRepository;
import capston.capston_spring.repository.UserRepository;
import capston.capston_spring.utils.MultipartInputStreamFileResource;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class AccuracySessionService {
    private final Logger log = LoggerFactory.getLogger(AccuracySessionService.class);

    private final AccuracySessionRepository accuracySessionRepository;
    private final AccuracyFrameEvaluationRepository frameEvaluationRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;

    // GPT 호출용 OpenAiService 주입
    private final OpenAiService openAiService;

    @Value("${flask.api.analyze}")
    private String flaskAnalyzeUrl;

    /** ID 기반 곡 조회 **/
    private Song getSongById(Long songId) {
        return songRepository.findById(songId)
                .orElseThrow(() -> new SongNotFoundException("Song not found: " + songId));
    }

    /** username 기반 사용자 조회 (기존 + 유지) **/
    private AppUser getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    /** 특정 사용자(username)의 정확도 세션 조회 **/
    public List<AccuracySession> getByUsername(String username) {
        AppUser user = getUserByUsername(username);
        return accuracySessionRepository.findByUserId(user.getId());
    }

    /** 특정 사용자(username) + 곡의 정확도 세션 조회 **/
    public List<AccuracySession> getBySongAndUsername(Long songId, String username) {
        AppUser user = getUserByUsername(username);
        Song song = getSongById(songId);
        return accuracySessionRepository.findByUserIdAndSongId(user.getId(), song.getId());
    }

    /** 특정 세션 ID로 정확도 세션 조회 **/
    public Optional<AccuracySession> getSessionById(Long sessionId) {
        return accuracySessionRepository.findById(sessionId);
    }

    /** 사용자가 플레이한 게임에 대한 결과(session info) 저장 **/
    public Object saveSession(Long sessionId) {
        AccuracySession session = accuracySessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("해당 세션이 존재하지 않습니다."));


        // 1. 해당 세션의 모든 프레임 평가 가져오기
        List<AccuracyFrameEvaluation> evaluations = frameEvaluationRepository.findBySession(session);

        // 2. 평균 점수 계산
        double avg = evaluations.stream()
                .mapToDouble(AccuracyFrameEvaluation::getScore)
                .average()
                .orElse(0.0);  // 점수가 없을 경우 0.0

        session.setEndTime(LocalDateTime.now());
        session.setAvg_score(avg);

        accuracySessionRepository.save(session);

        return ResponseEntity.ok().build();
    }

    /** 정확도 분석 후 결과 저장 (Flask 연동 유지) **/
    public AccuracyFrameEvaluation analyzeAndStoreFrameStep(String username, Long songId, Long sessionId, Integer frameIndex, MultipartFile image) throws IOException {
        AppUser user = getUserByUsername(username);
        Song song = getSongById(songId);

        AccuracySession session = accuracySessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("frame", new MultipartInputStreamFileResource(image.getInputStream(), image.getOriginalFilename()));// 0415 "image" → "frame"
        body.add("song_title", song.getTitle());
        body.add("session_id", sessionId);
        body.add("frame_index", frameIndex);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();

        ResponseEntity<Map> response = rt.postForEntity(flaskAnalyzeUrl, request, Map.class);

        // Flask 응답 전체 로그 출력
        log.info(" Flask 응답 상태: {}", response.getStatusCode());
        log.info(" Flask 응답 헤더: {}", response.getHeaders());
        log.info(" Flask 응답 본문: {}", response.getBody());

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Flask 분석 실패");
        }

        double accuracyScore = ((Number) response.getBody().get("score")).doubleValue();   // accuracy_score -> score
        String feedback = (String) response.getBody().get("feedback");

        AccuracyFrameEvaluation frame = new AccuracyFrameEvaluation();
        frame.setSession(session);
        frame.setFrameIndex(frameIndex);
        frame.setScore(accuracyScore);
        frame.setFeedback(feedback);

        return frameEvaluationRepository.save(frame);
    }


    /** 곡 제목으로 실루엣 + 가이드 영상 경로 반환 **/
    public Map<String, String> getVideoPathsBySongTitle(String songTitle) {
        Song song = songRepository.findByTitleIgnoreCase(songTitle)
                .orElseThrow(() -> new SongNotFoundException("Song not found with title: " + songTitle));

        Map<String, String> paths = new HashMap<>();
        paths.put("silhouetteVideoUrl", song.getSilhouetteVideoPath());
        return paths;
    }

    /** 정확도 세션 시작 - mode (full/highlight) 에 따라 자동 시간 설정 후 저장 **/
    public AccuracySession createAccuracySession(String username, Long songId, String mode) {
        AppUser user = getUserByUsername(username);
        Song song = getSongById(songId);

        int startSec, endSec;
        if (mode.equalsIgnoreCase("full")) {
            startSec = song.getFullStartTime();
            endSec = song.getFullEndTime();
        } else if (mode.equalsIgnoreCase("highlight")) {
            startSec = song.getHighlightStartTime();
            endSec = song.getHighlightEndTime();
        } else {
            throw new IllegalArgumentException("Invalid accuracy mode: " + mode);
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        AccuracySession session = new AccuracySession();
        session.setUser(user);
        session.setSong(song);
        session.setMode(mode);
        session.setStartTime(now);
        session.setEndTime(now.plusSeconds(endSec - startSec));
        session.setAvg_score(0.0); // 초기 점수

        return accuracySessionRepository.save(session);
    }

    // GPT 피드백 생성 메서드
    /**
     * 점수 하위 프레임을 조회하고 GPT로 피드백 생성
     *
     * @param sessionId 세션 ID
     * @return 프레임별 피드백 리스트
     */
    public List<String> generateLowScoreFeedback(Long sessionId) {
        AccuracySession session = accuracySessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));

        List<AccuracyFrameEvaluation> lowScoreFrames =
                frameEvaluationRepository.findTop5BySessionOrderByScoreAsc(session);

        List<String> feedbackList = new ArrayList<>();

        log.info("[LowScore] 세션 ID {} → 하위 프레임 {}개 조회됨", sessionId, lowScoreFrames.size());

        for (AccuracyFrameEvaluation frame : lowScoreFrames) {
            Integer frameIndex = frame.getFrameIndex();
            Double score = frame.getScore();
            log.info("프레임 번호: {}, 점수: {}", frameIndex, score);

            // 이미지 경로 규칙 (경로는 환경에 맞게 조정 필요)
            String userImagePath = "./saved_frames/user_" + sessionId + "_frame_" + frameIndex + ".png";
            String expertImagePath = "./saved_frames/expert_" + sessionId + "_frame_" + frameIndex + ".png";

            try {
                // GPT 호출
                String feedback = openAiService.getDanceImageFeedback(userImagePath, expertImagePath)
                        .block();

                // DB에 피드백 업데이트
                frame.setFeedback(feedback);
                frameEvaluationRepository.save(frame);

                feedbackList.add("Frame " + frameIndex + ": " + feedback);

            } catch (Exception e) {
                log.error("GPT 피드백 생성 실패 (Frame {}): {}", frameIndex, e.getMessage());
                feedbackList.add("Frame " + frameIndex + ": GPT feedback failed - " + e.getMessage());
            }
        }

        // 피드백 완료 상태 업데이트
        session.setFeedbackCompleted(true);
        accuracySessionRepository.save(session);

        return feedbackList;
    }

}
