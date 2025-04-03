package capston.capston_spring.repository;

import capston.capston_spring.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long> {
    /** 제목에 특정 키워드가 포함된 곡 조회 (대소문자 구분 없이) **/
    List<Song> findByTitleContainingIgnoreCase(String title);

    /** 아티스트 이름에 특정 키워드가 포함된 곡 조회 **/
    List<Song> findByArtistContainingIgnoreCase(String artist);

    /** 제목과 아티스트 둘 다 포함하는 곡 조회 **/
    List<Song> findByTitleContainingIgnoreCaseAndArtistContainingIgnoreCase(String title, String artist);

    //    List<Song> findByMode(String mode);
    /** 곡의 danceGuidePath(안무 가이드 영상 URL) 조회 **/
    @Query("SELECT s.danceGuidePath FROM Song s WHERE s.id = :id")
    Optional<String> findDanceGuidePathById(@Param("id") Long id);

    /** 특정 곡의 1절/하이라이트 구간 조회 **/
    @Query("""
           SELECT s.fullStartTime, s.fullEndTime,
                  s.highlightStartTime, s.highlightEndTime
           FROM Song s
           WHERE s.id = :id
           """)
    Optional<Object[]> findPracticeSectionsById(@Param("id") Long id);

    /** 곡 제목으로 곡 전체 정보 조회 (정확한 이름 기준) **/
    // 실루엣+가이드 같이 쓸 때 필요
    Optional<Song> findByTitleIgnoreCase(String title);
}
