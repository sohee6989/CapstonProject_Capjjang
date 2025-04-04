package capston.capston_spring.service;

import capston.capston_spring.dto.SongDto;
import capston.capston_spring.entity.Song;
import capston.capston_spring.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SongService {

    private final SongRepository songRepository;

    /** 모든 노래 조회 **/
    public List<Song> getAllSongs() {
        return songRepository.findAll();
    }

    /** ID로 특정 곡 조회 **/
    public Song getSongById(Long songId) {
        return songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("Song not found with ID: " + songId));
    }

    /** 특정 곡의 댄스 가이드 영상 경로 조회 **/
    public String getDanceGuidePath(Long songId) {
        return songRepository.findDanceGuidePathById(songId)
                .orElseThrow(() -> new RuntimeException("Dance guide video not found for song ID: " + songId));
    }

    // 0404 추가: 특정 곡의 실루엣 영상 경로 조회 메서드
    public String getSilhouetteVideoPath(Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("Song not found with ID: " + songId));
        return song.getSilhouetteVideoPath(); // ← Song 엔티티에 있는 필드 기준!
    }

    /** 특정 곡의 1절/하이라이트 구간 조회 **/
    public Optional<Object[]> getPracticeSections(Long songId) {
        return songRepository.findPracticeSectionsById(songId);
    }

    /** 제목 또는 가수로 노래 검색 (검색용 DTO 반환) **/
    public List<Song> searchSongs(String title, String artist) {
        if (title != null && artist != null) {
            return songRepository.findByTitleContainingIgnoreCaseAndArtistContainingIgnoreCase(title, artist); // 수정됨
        } else if (title != null) {
            return songRepository.findByTitleContainingIgnoreCase(title); // 수정됨
        } else if (artist != null) {
            return songRepository.findByArtistContainingIgnoreCase(artist); // 수정됨
        } else {
            return songRepository.findAll(); // 수정됨
        }
    }

    // 노래 저장
    public Song saveSong(SongDto songDto) {
        Song song = new Song();
        song.setTitle(songDto.getTitle());
        song.setArtist(songDto.getArtist());
        song.setCoverImagePath(songDto.getCoverImagePath());

        // JWT 토큰에서 사용자 정보 추출
        String username = getAuthenticatedUsername();
        song.setCreatedBy(username); // 수정됨

        return songRepository.save(song);
    }


    // 인증된 사용자 이름 가져오기
    private String getAuthenticatedUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getUsername();
        } else {
            throw new RuntimeException("Authentication required to access this service");
        }
    }

}
