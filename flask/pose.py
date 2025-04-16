import cv2
import mediapipe as mp
import math

mp_pose = mp.solutions.pose

# 사용자 포즈 추출
def extract_pose_keypoints(frame):
    with mp_pose.Pose(static_image_mode=True) as pose:
        image_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = pose.process(image_rgb)

        if not results.pose_landmarks:
            raise ValueError("Pose not detected")

        landmarks = results.pose_landmarks.landmark
        indices = {
            "left_shoulder": 11, "right_shoulder": 12, "left_elbow": 13, "right_elbow": 14,
            "left_wrist": 15, "right_wrist": 16, "left_hip": 23, "right_hip": 24,
            "left_knee": 25, "right_knee": 26, "left_ankle": 27, "right_ankle": 28
        }

        extracted = {name: landmarks[idx] for name, idx in indices.items()}

        l_hip = extracted["left_hip"]
        r_hip = extracted["right_hip"]
        base_x = (l_hip.x + r_hip.x) / 2
        base_y = (l_hip.y + r_hip.y) / 2

        norm_sq = sum((v.x - base_x) ** 2 + (v.y - base_y) ** 2 for v in extracted.values())
        l2 = math.sqrt(norm_sq) or 1.0

        normalized = {
            name: {"x": (v.x - base_x) / l2, "y": (v.y - base_y) / l2}
            for name, v in extracted.items()
        }
        normalized["mid_hip"] = {"x": 0.0, "y": 0.0}
        return normalized

# 각 부위 차이 계산 함수
def part_diff(user, ref, keys):
    total = sum(abs(user[k]['x'] - ref[k]['x']) + abs(user[k]['y'] - ref[k]['y']) for k in keys)
    return (total / len(keys)) * 100

# 전체 방향 기반 점수 계산
def compare_pose_directional(user, ref):
    def angle(pose):
        dx = pose['mid_hip']['x'] - pose['left_shoulder']['x']
        dy = pose['mid_hip']['y'] - pose['left_shoulder']['y']
        return math.degrees(math.atan2(dy, dx))

    user_angle = angle(user)
    ref_angle = angle(ref)
    diff_angle = abs(user_angle - ref_angle)

    breakdown = {
        'body_direction': max(0, 100 - diff_angle),
        'left_arm': max(0, 100 - part_diff(user, ref, ["left_shoulder", "left_elbow", "left_wrist"])),
        'right_arm': max(0, 100 - part_diff(user, ref, ["right_shoulder", "right_elbow", "right_wrist"])),
        'left_leg': max(0, 100 - part_diff(user, ref, ["left_hip", "left_knee", "left_ankle"])),
        'right_leg': max(0, 100 - part_diff(user, ref, ["right_hip", "right_knee", "right_ankle"]))
    }

    weights = {
        'body_direction': 5.0,
        'left_arm': 3.0,
        'right_arm': 3.0,
        'left_leg': 1.0,
        'right_leg': 1.0
    }

    total_weight = sum(weights.values())
    total_score = round(sum(breakdown[k] * weights[k] for k in breakdown) / total_weight, 2)


    return total_score
