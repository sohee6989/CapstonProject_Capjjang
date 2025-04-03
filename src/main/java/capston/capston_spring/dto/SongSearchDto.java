package capston.capston_spring.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SongSearchDto {
    private Long id;
    private String title;
    private String artist;
    private String coverImagePath;
}