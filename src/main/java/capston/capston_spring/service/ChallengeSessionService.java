package capston.capston_spring.service;

import capston.capston_spring.dto.ChallengeSessionDto;
import capston.capston_spring.entity.AppUser;
import capston.capston_spring.entity.ChallengeSession;
import capston.capston_spring.entity.Song;
import capston.capston_spring.repository.ChallengeSessionRepository;
import capston.capston_spring.repository.SongRepository;
import capston.capston_spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
        session.setStartTime(dto.getStartTime());
        session.setEndTime(dto.getEndTime());

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

    /** 챌린지에서 사용할 노래의 하이라이트 부분 반환 (DB에서 동적 가져오기) **/
    public String getHighlightPart(Long songId) {
        Song song = getSongById(songId);
        if (song.getHighlightStartTime() == 0 || song.getHighlightEndTime() == 0) {
            return "No highlight part available"; // 메시지도 영어로 변경
        }
        return String.format("%02d:%02d-%02d:%02d",
                song.getHighlightStartTime() / 60, song.getHighlightStartTime() % 60,
                song.getHighlightEndTime() / 60, song.getHighlightEndTime() % 60);
    }
}