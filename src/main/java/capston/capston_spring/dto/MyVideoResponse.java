package capston.capston_spring.dto;

import capston.capston_spring.entity.VideoMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MyVideoResponse {
    private Long videoId;
    private String songTitle;
    private String artist;
    private VideoMode mode;
    private String modeDescription; // 추가
    private String songImgPath;
    private String videoPath;

    public MyVideoResponse(Long videoId, String songTitle, String artist, VideoMode mode, String songImgPath, String videoPath) {
        this.videoId = videoId;
        this.songTitle = songTitle;
        this.artist = artist;
        this.mode = mode;
        this.modeDescription = mode.getDescription(); // enum에서 가져옴
        this.songImgPath = songImgPath;
        this.videoPath = videoPath;
    }
}