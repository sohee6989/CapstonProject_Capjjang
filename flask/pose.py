import cv2
import mediapipe as mp
import math
import numpy as np

mp_pose = mp.solutions.pose

# 관절 각도 계산
def compute_angle(a, b, c):
    a = np.array([a['x'], a['y']])
    b = np.array([b['x'], b['y']])
    c = np.array([c['x'], c['y']])

    ba = a - b
    bc = c - b

    cosine_angle = np.dot(ba, bc) / (np.linalg.norm(ba) * np.linalg.norm(bc))
    return np.degrees(np.arccos(np.clip(cosine_angle, -1.0, 1.0)))


# 사용자 포즈 추출
def extract_pose_keypoints(frame):
    with mp_pose.Pose(static_image_mode=True) as pose:
        image_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = pose.process(image_rgb)

        if not results.pose_landmarks:
            return None  # 예외 대신 None 반환

        landmarks = results.pose_landmarks.landmark
        indices = {
            "left_shoulder": 11, "right_shoulder": 12, "left_elbow": 13, "right_elbow": 14,
            "left_wrist": 15, "right_wrist": 16, "left_hip": 23, "right_hip": 24,
            "left_knee": 25, "right_knee": 26, "left_ankle": 27, "right_ankle": 28
        }

        extracted = {name: landmarks[idx] for name, idx in indices.items()}


        base_x = (extracted["left_hip"].x + extracted["right_hip"].x) / 2
        base_y = (extracted["left_hip"].y + extracted["right_hip"].y) / 2


        norm_sq = sum((v.x - base_x) ** 2 + (v.y - base_y) ** 2 + v.z**2 for v in extracted.values())
        l2 = math.sqrt(norm_sq) or 1.0

        normalized = {
            name: {"x": (v.x - base_x) / l2, "y": (v.y - base_y) / l2, "z": v.z / l2}
            for name, v in extracted.items()
        }
        normalized["mid_hip"] = {"x": 0.0, "y": 0.0, "z": 0.0}
        return normalized


# 각 부위 차이 계산 함수
def part_diff(user, ref, keys):
    total = sum(abs(user[k]['x'] - ref[k]['x']) + abs(user[k]['y'] - ref[k]['y']) + abs(user[k]['z'] - ref[k]['z']) for k in keys)
    return (total / len(keys)) * 100


# 전체 방향 기반 점수 계산
def compare_pose_directional(user, ref):
    if user is None or ref is None:
        return 0.0

    def angle(pose, joint_set):
        try:
            return compute_angle(pose[joint_set[0]], pose[joint_set[1]], pose[joint_set[2]])
        except:
            return 0.0

    # 방향 각도
    dx = user['mid_hip']['x'] - user['left_shoulder']['x']
    dy = user['mid_hip']['y'] - user['left_shoulder']['y']
    user_angle = math.degrees(math.atan2(dy, dx))

    dx = ref['mid_hip']['x'] - ref['left_shoulder']['x']
    dy = ref['mid_hip']['y'] - ref['left_shoulder']['y']
    ref_angle = math.degrees(math.atan2(dy, dx))

    diff_angle = ((user_angle - ref_angle + 180) % 360) - 180
    direction_score = max(0, 100 - abs(diff_angle))

    # 부위별 거리 기반 점수
    body_parts = {
        'left_arm': ["left_shoulder", "left_elbow", "left_wrist"],
        'right_arm': ["right_shoulder", "right_elbow", "right_wrist"],
        'left_leg': ["left_hip", "left_knee", "left_ankle"],
        'right_leg': ["right_hip", "right_knee", "right_ankle"]
    }

    breakdown = {}
    for part, joints in body_parts.items():
        breakdown[part] = max(0, 100 - part_diff(user, ref, joints))

    # 각도 기반 점수
    angle_scores = {}
    for part, joints in body_parts.items():
        user_a = angle(user, joints)
        ref_a = angle(ref, joints)
        diff = abs(user_a - ref_a)
        angle_scores[part] = max(0, 100 - diff)

    # 가중치 합산
    weights = {
        'direction': 4.0,
        'left_arm': 2.0,
        'right_arm': 2.0,
        'left_leg': 1.0,
        'right_leg': 1.0,
        'angle_left_arm': 1.5,
        'angle_right_arm': 1.5,
        'angle_left_leg': 1.0,
        'angle_right_leg': 1.0
    }

    total_score = (
        direction_score * weights['direction'] +
        breakdown['left_arm'] * weights['left_arm'] +
        breakdown['right_arm'] * weights['right_arm'] +
        breakdown['left_leg'] * weights['left_leg'] +
        breakdown['right_leg'] * weights['right_leg'] +
        angle_scores['left_arm'] * weights['angle_left_arm'] +
        angle_scores['right_arm'] * weights['angle_right_arm'] +
        angle_scores['left_leg'] * weights['angle_left_leg'] +
        angle_scores['right_leg'] * weights['angle_right_leg']
    ) / sum(weights.values())

    return round(total_score, 2)
