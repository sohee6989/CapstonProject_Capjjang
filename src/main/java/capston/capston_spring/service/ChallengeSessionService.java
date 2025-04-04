package capston.capston_spring.service;

import capston.capston_spring.dto.ChallengeSessionDto;
import capston.capston_spring.dto.ChallengeSessionResponse;
import capston.capston_spring.entity.AppUser;
import capston.capston_spring.entity.ChallengeSession;
import capston.capston_spring.entity.Song;
import capston.capston_spring.repository.ChallengeSessionRepository;
import capston.capston_spring.repository.SongRepository;
import capston.capston_spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeSessionService {

    private final ChallengeSessionRepository challengeSessionRepository;
    private final UserRepository userRepository;
    private final SongRepository songRepository;

    /** username 기반 사용자 조회 메서드 추가 **/
    private AppUser getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    /** ID 기반 노래 조회 (수정 : 메소드 추가) **/
    private Song getSongById(Long songId) {
        return songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("Song not found with ID: " + songId));
    }

    /** (수정 : 유저 및 곡 정보 불러오기 코드 간결화) **/
    private ChallengeSession convertToEntity(ChallengeSessionDto dto, String username) {
        if (dto == null) {
            throw new IllegalArgumentException("ChallengeSessionDto cannot be null");
        }

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username)); // 수정된 부분

        Song song = getSongById(dto.getSongId());

        ChallengeSession session = new ChallengeSession();
        session.setUser(user);
        session.setSong(song);
        session.setStartTime(song.getHighlightStartTime()); // 수정: int
        session.setEndTime(song.getHighlightEndTime());     // int

        return session;
    }

    /** 챌린지 세션 저장 (점수 분석 없이 저장) **/
    public ChallengeSession saveChallengeSession(ChallengeSessionDto dto, String username) {
        ChallengeSession session = convertToEntity(dto, username);
        return challengeSessionRepository.save(session);
    }

    /** username 기반 챌린지 세션 전체 조회 **/
    public List<ChallengeSession> getByUsername(String username) {
        AppUser user = getUserByUsername(username);
        return challengeSessionRepository.findByUserId(user.getId());
    }

    /** username + songId 기반 챌린지 세션 조회 **/
    public List<ChallengeSession> getByUsernameAndSongId(String username, Long songId) {
        AppUser user = getUserByUsername(username);
        Song song = getSongById(songId);
        return challengeSessionRepository.findByUserIdAndSongId(user.getId(), song.getId());
    }

    // 추가: ChallengeSessionResponse DTO를 반환하도록 구조 변경
    // ChallengeSessionResponse 생성 시 LocalDateTime 대신 int 사용
    public List<ChallengeSessionResponse> getChallengeSessionsWithSong(String username, Long songId) {
        AppUser user = getUserByUsername(username);
        List<ChallengeSession> sessions;

        if (songId == null) {
            sessions = challengeSessionRepository.findByUserId(user.getId());
        } else {
            Song song = getSongById(songId);
            sessions = challengeSessionRepository.findByUserIdAndSongId(user.getId(), song.getId());
        }

        return sessions.stream()
                .map(session -> {
                    int startTime = session.getStartTime();  // int로 변경
                    int endTime = session.getEndTime();      // int로 변경
                    String duration = formatDuration(startTime, endTime);

                    // 응답에 필요한 song 정보만 SongInfo로 래핑
                    ChallengeSessionResponse.SongInfo songInfo = new ChallengeSessionResponse.SongInfo(
                            session.getSong().getId(),
                            session.getSong().getTitle()
                    );

                    return new ChallengeSessionResponse(
                            session.getId(),
                            songInfo,
                            startTime,   // int로 변경
                            endTime,     // int로 변경
                            duration
                    );
                })
                .collect(Collectors.toList());
    }

     // 추가: startTime, endTime 기준으로 duration 문자열 계산 (MM:SS 형식)
     private String formatDuration(int start, int end) {
        int durationSec = end - start;
        int minutes = durationSec / 60;
        int seconds = durationSec % 60;
        return String.format("%02d:%02d", minutes, seconds);
     }

    /** 챌린지에서 사용할 배경 (배경 이미지 또는 아바타) **/
    public String getChallengeBackground(Long songId) {
        Song song = getSongById(songId);
        String avatarPath = song.getAvatarVideoWithAudioPath();

        if (avatarPath == null || avatarPath.isEmpty()) {
            String path = song.getCoverImagePath();
            return (path == null || path.isEmpty()) ? "" : path;
        }

        return avatarPath;
    }

    // 0403 챌린지 모드 시간 하이라이트에서 가져오는걸로 수정 + 실패 응답 메시지 수정
    /** 챌린지 하이라이트 시간 문자열 반환 + 세션 저장 **/
    public String getHighlightPart(Long songId, String username) {
        AppUser user = getUserByUsername(username);
        Song song = getSongById(songId);

        int startTime = song.getHighlightStartTime();
        int endTime = song.getHighlightEndTime();

        if (startTime == 0 && endTime == 0) {
            throw new IllegalArgumentException("Highlight part is not defined for this song.");
        }

        ChallengeSession session = new ChallengeSession();
        session.setUser(user);
        session.setSong(song);
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        challengeSessionRepository.save(session);

        return String.format("%02d:%02d-%02d:%02d",
                startTime / 60, startTime % 60,
                endTime / 60, endTime % 60);
    }
}
