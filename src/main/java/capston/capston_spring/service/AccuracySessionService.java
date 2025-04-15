package capston.capston_spring.service;

import capston.capston_spring.dto.AccuracySessionDto;
import capston.capston_spring.entity.AccuracyFrameEvaluation;
import capston.capston_spring.entity.AccuracySession;
import capston.capston_spring.entity.AppUser;
import capston.capston_spring.entity.Song;
import capston.capston_spring.exception.SongNotFoundException;
import capston.capston_spring.exception.UserNotFoundException;
import capston.capston_spring.repository.AccuracyFrameEvaluationRepository;
import capston.capston_spring.repository.AccuracySessionRepository;
import capston.capston_spring.repository.SongRepository;
import capston.capston_spring.repository.UserRepository;
import capston.capston_spring.utils.MultipartInputStreamFileResource;
import lombok.RequiredArgsConstructor;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class AccuracySessionService {
    private final AccuracySessionRepository accuracySessionRepository;
    private final AccuracyFrameEvaluationRepository frameEvaluationRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;

    @Value("${flask.api.analyze}")
    private String flaskAnalyzeUrl;

    /** ID 기반 곡 조회 **/
    private Song getSongById(Long songId) {
        return songRepository.findById(songId)
                .orElseThrow(() -> new SongNotFoundException("Song not found: " + songId));
    }

    public List<AccuracySession> getByUserId(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        return accuracySessionRepository.findByUserId(user.getId());
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

    /** AccuracySessionDto 기반 세션 저장 **/
    public AccuracySession saveSessionFromDto(String username, AccuracySessionDto dto) {
        AppUser user = getUserByUsername(username);
        Song song = getSongById(dto.getSongId());

        AccuracySession session = new AccuracySession();
        session.setUser(user);
        session.setSong(song);
        session.setScore(dto.getScore());
        session.setFeedback(dto.getFeedback());
        session.setAccuracyDetails(dto.getAccuracyDetails());
        session.setMode(dto.getMode());
        session.setStartTime(dto.getStartTime());
        session.setEndTime(dto.getEndTime());

        return accuracySessionRepository.save(session);
    }

    /** 정확도 분석 후 결과 저장 (Flask 연동 유지) **/
    public AccuracyFrameEvaluation analyzeAndStoreFrameStep(String username, Long songId, Long sessionId, Integer frameIndex, MultipartFile image) throws IOException {
        AppUser user = getUserByUsername(username);
        Song song = getSongById(songId);

        AccuracySession session = accuracySessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new MultipartInputStreamFileResource(image.getInputStream(), image.getOriginalFilename()));
        body.add("song_title", song.getTitle());
        body.add("session_id", sessionId);
        body.add("frame_index", frameIndex);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();

        ResponseEntity<Map> response = rt.postForEntity(flaskAnalyzeUrl, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Flask 분석 실패");
        }

        double accuracyScore = ((Number) response.getBody().get("accuracy_score")).doubleValue();
        String feedback = (String) response.getBody().get("feedback");
        String accuracyDetails = (String) response.getBody().get("accuracy_details");
        String label = (String) response.getBody().get("label");

        AccuracyFrameEvaluation frame = new AccuracyFrameEvaluation();
        frame.setSession(session);
        frame.setFrameIndex(frameIndex);
        frame.setScore(accuracyScore);
        frame.setLabel(label);
        frame.setAccuracyDetails(accuracyDetails);

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

    /** 정확도 세션 시작 - full 모드 (리스트 형태로 반환하도록 래퍼 메서드 추가됨) 0414 **/
    public List<AccuracySession> startFullAccuracySessionList(String username, Long songId, Long sessionId) {
        AccuracySession session = startAccuracySessionByUsername(username, songId, "full", sessionId);
        return List.of(session);
    }

    /** 정확도 세션 시작 - highlight 모드 (리스트 형태로 반환하도록 래퍼 메서드 추가됨) 0414 **/
    public List<AccuracySession> startHighlightAccuracySessionList(String username, Long songId, Long sessionId) {
        AccuracySession session = startAccuracySessionByUsername(username, songId, "highlight", sessionId);
        return List.of(session);
    }

    //0403 수정: 챌린지 세션 시간은 하이라이트 그대로 받아오기
    /** 정확도 세션 시작 - mode (full/highlight) 에 따라 자동 시간 설정 후 저장 **/
    public AccuracySession startAccuracySessionByUsername(String username, Long songId, String mode, Long sessionId) {
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
        session.setScore(0.0); // 초기 점수
        session.setFeedback(null); // 초기 피드백 없음
        session.setAccuracyDetails(null); // 초기 상세 없음
        session.setSessionId(sessionId); // sessionId 설정

        return accuracySessionRepository.save(session);
    }

}
    /** 사용자 연습 기록 조회 getUserAccuracyHistory (수정 : 메서드 삭제) **/
