package capston.capston_spring.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SongDto {
    private String title;
    private String artist;
    private int bpm;
    private int duration;
    private String audioFilePath;
    private String danceGuidePath;
    private String avatarVideoWithAudioPath;
    private String coverImagePath;

    private int highlightStart;
    private int highlightEnd;
    private int fullStart;
    private int fullEnd;
    private int challengeStart;
    private int challengeEnd;
}