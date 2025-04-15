import cv2
import mediapipe as mp
import numpy as np
from fastdtw import fastdtw
from scipy.spatial.distance import euclidean
import time

# Mediapipe Pose 모델 초기화
mp_pose = mp.solutions.pose
mp_drawing = mp.solutions.drawing_utils

# OpenCV 최적화 설정
cv2.setUseOptimized(True)
cv2.setNumThreads(4)


# 비율 유지하면서 프레임 리사이즈 (여백 추가)
def resize_with_aspect_ratio(image, target_width, target_height): 
    h, w = image.shape[:2]
    scale = min(target_width / w, target_height / h)
    new_w = int(w * scale)
    new_h = int(h * scale)
    resized = cv2.resize(image, (new_w, new_h))

    top = (target_height - new_h) // 2
    bottom = target_height - new_h - top
    left = (target_width - new_w) // 2
    right = target_width - new_w - left
    padded = cv2.copyMakeBorder(resized, top, bottom, left, right, cv2.BORDER_CONSTANT, value=[0, 0, 0])
    return padded

# 전문가 포즈 1프레임에서 키포인트 추출
def extract_expert_keypoints(song_title):
    expert_video_path = f"videos1/{song_title}_expert.mp4"
    cap = cv2.VideoCapture(expert_video_path)

    if not cap.isOpened():
        raise ValueError("전문가 영상을 열 수 없습니다.")

    ret, frame = cap.read()
    cap.release()

    if not ret:
        raise ValueError("전문가 영상에서 프레임을 읽을 수 없습니다.")

    frame_resized = resize_with_aspect_ratio(frame, 640, 480)
    frame_rgb = cv2.cvtColor(frame_resized, cv2.COLOR_BGR2RGB)

    with mp_pose.Pose(static_image_mode=True) as pose:
        result = pose.process(frame_rgb)
        if result.pose_landmarks:
            return np.array([[lmk.x, lmk.y, lmk.z] for lmk in result.pose_landmarks.landmark])
        else:
            raise ValueError("전문가 영상에서 포즈를 감지할 수 없습니다.")
            
# 실시간 프레임 1장 분석용 함수 (Flask /frame API에서 사용)
def analyze_frame_image(frame_user, song_title):
    expert_keypoints = extract_expert_keypoints(song_title)

    with mp_pose.Pose(static_image_mode=True) as pose:
        frame_rgb = cv2.cvtColor(frame_user, cv2.COLOR_BGR2RGB)
        result_amateur = pose.process(frame_rgb)

        if result_amateur.pose_landmarks:
            amateur_keypoints = np.array([[lmk.x, lmk.y, lmk.z] for lmk in result_amateur.pose_landmarks.landmark])
            amateur_keypoints_flat = amateur_keypoints.flatten()
            expert_keypoints_flat = expert_keypoints.flatten()

            distance, _ = fastdtw(
                expert_keypoints_flat[:, np.newaxis],
                amateur_keypoints_flat[:, np.newaxis],
                dist=euclidean
            )

            max_distance = 50.0
            score = max(100 - (distance / max_distance) * 100, 0)
            
            if score >= 90:
                feedback = "Perfect"
            elif score >= 80:
                feedback = "Good"
            elif score >= 75:
                feedback = "Normal"
            elif score >= 60:
                feedback = "Bad"
            else:
                feedback = "Worst"

            return score, feedback

        return 0.0, "No pose"


# 기존 웹캠 기반 실시간 분석 및 시각화
def process_and_compare_videos(*, song_title):
    expert_video_path = f"videos1/{song_title}_expert.mp4"
    silhouette_path = f"video_uploads/{song_title}_silhouette.mp4"

    cap_expert = cv2.VideoCapture(expert_video_path)
    cap_webcam = cv2.VideoCapture(0)
    cap_silhouette = cv2.VideoCapture(silhouette_path)

    if not cap_expert.isOpened() or not cap_webcam.isOpened() or not cap_silhouette.isOpened():
        print(" 영상 파일 또는 웹캠을 열 수 없습니다.")
        return

    frame_idx = 0
    score_interval = 1.5
    last_score_time = time.time()
    last_score = None
    last_feedback = None
    display_duration = 1.5
    score_display_time = time.time()

    with mp_pose.Pose(static_image_mode=False, min_detection_confidence=0.5, min_tracking_confidence=0.5) as pose:
        while cap_expert.isOpened() and cap_webcam.isOpened():
            ret_expert, frame_expert = cap_expert.read()
            ret_cam, frame_cam = cap_webcam.read()
            ret_sil, frame_sil = cap_silhouette.read()

            frame_cam = cv2.flip(frame_cam, 1)

            if not ret_expert or not ret_cam:
                break

            if not ret_sil:
                cap_silhouette.set(cv2.CAP_PROP_POS_FRAMES, 0)
                ret_sil, frame_sil = cap_silhouette.read()
                if not ret_sil:
                    continue

            frame_expert_resized = resize_with_aspect_ratio(frame_expert, 640, 480)
            frame_cam_resized = resize_with_aspect_ratio(frame_cam, 640, 480)
            frame_silhouette_resized = resize_with_aspect_ratio(frame_sil, 640, 480)

            blended_user = cv2.addWeighted(frame_cam_resized, 0.5, frame_silhouette_resized, 0.5, 0)

            frame_expert_rgb = cv2.cvtColor(frame_expert_resized, cv2.COLOR_BGR2RGB)
            frame_user_rgb = cv2.cvtColor(blended_user, cv2.COLOR_BGR2RGB)

            result_expert = pose.process(frame_expert_rgb)
            result_amateur = pose.process(frame_user_rgb)

            current_time = time.time()

            if current_time - last_score_time >= score_interval:
                if result_expert.pose_landmarks and result_amateur.pose_landmarks:
                    expert_keypoints = np.array([[lmk.x, lmk.y, lmk.z] for lmk in result_expert.pose_landmarks.landmark])
                    amateur_keypoints = np.array([[lmk.x, lmk.y, lmk.z] for lmk in result_amateur.pose_landmarks.landmark])

                    expert_keypoints_flat = expert_keypoints.flatten()
                    amateur_keypoints_flat = amateur_keypoints.flatten()

                    distance, _ = fastdtw(
                        expert_keypoints_flat[:, np.newaxis],
                        amateur_keypoints_flat[:, np.newaxis],
                        dist=euclidean
                    )

                    max_distance = 5.0
                    score = max(100 - (distance / max_distance) * 100, 0)

                    if score >= 90:
                        feedback = "Perfect"
                    elif score >= 80:
                        feedback = "Good"
                    elif score >= 75:
                        feedback = "Normal"
                    elif score >= 60:
                        feedback = "Bad"
                    else:
                        feedback = "Worst"

                    cv2.putText(blended_user, f"Score: {score:.2f}", (50, 50),
                                cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
                    cv2.putText(blended_user, feedback, (50, 100),
                                cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)

                    last_score = score
                    last_feedback = feedback
                    last_score_time = current_time
                    score_display_time = current_time

            if last_score is not None and last_feedback is not None and current_time - score_display_time <= display_duration:
                cv2.putText(blended_user, f"Score: {last_score:.2f}", (50, 50),
                            cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
                cv2.putText(blended_user, last_feedback, (50, 100),
                            cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)

            cv2.imshow("Accuracy Mode", blended_user)

            if cv2.waitKey(1) & 0xFF == ord('q'):
                break

            frame_idx += 1

    cap_expert.release()
    cap_webcam.release()
    cap_silhouette.release()
    cv2.destroyAllWindows()

    return None
