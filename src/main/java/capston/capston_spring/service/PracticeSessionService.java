package capston.capston_spring.service;

import capston.capston_spring.dto.PracticeSessionDto;
import capston.capston_spring.entity.AppUser;
import capston.capston_spring.entity.PracticeSession;
import capston.capston_spring.entity.Song;
import capston.capston_spring.repository.PracticeSessionRepository;
import capston.capston_spring.repository.SongRepository;
import capston.capston_spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PracticeSessionService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final UserRepository userRepository;
    private final SongRepository songRepository;

    /** username 기반으로 1절/하이라이트 연습 세션 시작 (공통 처리) **/
    public PracticeSession startPracticeSessionByUsername(String username, Long songId, String section) {
        AppUser user = getUserByUsername(username); // 예외 통일
        Song song = getSongById(songId);            // 예외 통일

        int startTime;
        int endTime;

        switch (section.toLowerCase()) {
            case "full":
                startTime = song.getFullStartTime();
                endTime = song.getFullEndTime();
                break;
            case "highlight":
                startTime = song.getHighlightStartTime();
                endTime = song.getHighlightEndTime();
                break;
            default:
                throw new IllegalArgumentException("Invalid section: " + section); // 예외 처리
        }

        PracticeSession session = new PracticeSession();
        session.setUser(user);
        session.setSong(song);
        session.setStartTimeFromSeconds(startTime);
        session.setEndTimeFromSeconds(endTime);
        session.setMode(section);  // mode 설정

        return practiceSessionRepository.save(session);
    }

    /** username 기반 1절 연습 세션 시작 */
    public PracticeSession startFullPracticeSessionByUsername(String username, Long songId) {
        return startPracticeSessionByUsername(username, songId, "full");
    }

    /** username 기반 하이라이트 연습 세션 시작 **/
    public PracticeSession startHighlightPracticeSessionByUsername(String username, Long songId) {
        return startPracticeSessionByUsername(username, songId, "highlight");
    }

    /** DTO 기반 연습 세션 저장 (username은 토큰 기반으로 컨트롤러에서 전달) **/
    public PracticeSession savePracticeSession(PracticeSessionDto dto, String username) {
        PracticeSession session = convertToEntity(dto, username);  // username을 외부에서 전달
        return practiceSessionRepository.save(session);
    }

    /** PracticeSessionDto → Entity 변환 (username 기반) **/
    private PracticeSession convertToEntity(PracticeSessionDto dto, String username) {
        AppUser user = getUserByUsername(username);
        Song song = getSongById(dto.getSongId());

        PracticeSession session = new PracticeSession();
        session.setUser(user);
        session.setSong(song);
        session.setStartTimeFromSeconds((int) dto.getStartTime().toEpochSecond(ZoneOffset.UTC));
        session.setEndTimeFromSeconds((int) dto.getEndTime().toEpochSecond(ZoneOffset.UTC));
        session.setMode(dto.getMode());

        return session;
    }

    /** username으로 사용자 조회 **/
    private AppUser getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    /** songId로 곡 조회 **/
    private Song getSongById(Long songId) {
        return songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Song not found: " + songId));
    }

    /** username 기반 사용자 연습 세션 조회 */
    public List<PracticeSession> getByUsername(String username) {
        AppUser user = getUserByUsername(username);
        return practiceSessionRepository.findByUserId(user.getId());
    }

    /** username 기반 곡 + 사용자 연습 세션 조회 */
    public List<PracticeSession> getBySongAndUsername(Long songId, String username) {
        AppUser user = getUserByUsername(username);
        return practiceSessionRepository.findBySongIdAndUserId(songId, user.getId());
    }


    /** Song 엔티티에서 1절 끝나는 시간 조회 (초 단위) **/
    public int getFullEndTime(Long songId) {
        Song song = getSongById(songId);
        return song.getFullEndTime();
    }

    /** Song 엔티티에서 하이라이트 구간 조회 **/
    public int[] getHighlightRange(Long songId) {
        Song song = getSongById(songId);
        return new int[]{song.getHighlightStartTime(), song.getHighlightEndTime()};
    }

    /** 곡 제목으로 연습용 비디오 경로 조회 (실루엣 + 가이드) **/
    public Map<String, String> getPracticeModeVideoPathsBySongTitle(String songTitle) {
        Song song = songRepository.findByTitleIgnoreCase(songTitle)
                .orElseThrow(() -> new IllegalArgumentException("Song not found: " + songTitle));

        Map<String, String> paths = new HashMap<>();
        paths.put("danceGuideUrl", song.getDanceGuidePath());
        paths.put("silhouetteVideoUrl", song.getSilhouetteVideoPath());
        return paths;
    }
}
