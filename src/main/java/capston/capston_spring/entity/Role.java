package capston.capston_spring.entity;

public enum Role {
    USER, ADMIN;

    // Role을 String으로 변환
    @Override
    public String toString() {
        return "ROLE_" + name();  // "ROLE_USER", "ROLE_ADMIN" 등의 형식으로 반환
    }
}
