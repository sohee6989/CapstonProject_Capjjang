package capston.capston_spring.entity;

import jakarta.persistence.*;
import lombok.*;


/** 곡 정보 관리 **/
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    @Column(nullable = false)
    private int duration = 0;

    @Column
    private String genre;


    /** 연습 모드 관련 필드 **/
    @Column
    private Integer  fullStartTime; // 1절 시작 시간 (초 단위)

    @Column
    private Integer  fullEndTime; // 1절 끝나는 시간 (초 단위)

    @Column
    private Integer  highlightStartTime; // 하이라이트 시작 시간

    @Column
    private Integer  highlightEndTime; // 하이라이트 끝나는 시간

    
    /** 파일 경로 관련 필드 **/
    @Column
    private String audioFilePath; // 오디오 파일 경로

    @Column(name = "silhouette_video_path")
    private String silhouetteVideoPath;     // 안무 실루엣 영상 경로

    @Column
    private String danceGuidePath; // 댄스 가이드 비디오 경로

    @Column
    private String avatarVideoWithAudioPath; // 아바타 비디오 + 오디오 경로

    @Column
    private String coverImagePath; // 곡 커버 이미지 경로

    /** 사용자 정보 (곡 생성자) **/
    @Column(nullable = false)
    private String createdBy; // 곡을 만든 사용자 정보

}
