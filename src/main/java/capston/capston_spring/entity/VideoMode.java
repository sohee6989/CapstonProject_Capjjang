package capston.capston_spring.entity;

public enum VideoMode {
    PRACTICE("Practice mode"),    // 연습 모드
    CHALLENGE("Challenge mode"),  // 챌린지 모드
    ACCURACY("Accuracy mode");    // 정확도 모드 추가됨

    private final String description;

    VideoMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
    // PRACTICE,  // 연습 모드
    // CHALLENGE  // 챌린지 모드
}