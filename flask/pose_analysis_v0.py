# pose_analysis.py 가장 처음에 올린 파일입니다

import cv2
import mediapipe as mp
import numpy as np
from fastdtw import fastdtw
from scipy.spatial.distance import euclidean
import time
from s3_helper import download_temp_from_s3

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

def process_and_compare_videos(*, song_title):
    expert_video_path, silhouette_path = download_temp_from_s3(song_title)

    cap_expert = cv2.VideoCapture(expert_video_path)
    cap_webcam = cv2.VideoCapture(0)  # 사용자: 실시간 웹캠 입력
    cap_silhouette = cv2.VideoCapture(silhouette_path)  # 실루엣 영상 (사용자 쪽에 오버레이)

    if not cap_expert.isOpened() or not cap_webcam.isOpened() or not cap_silhouette.isOpened():
        print(" 영상 파일 또는 웹캠을 열 수 없습니다.")
        return
    
    frame_idx = 0  # frame_idx 초기화

    # 점수 계산 간격 설정 (1.5초 간격)
    score_interval = 1.5  # 초 단위
    last_score_time = time.time()
    last_score = None      # 마지막 점수 저장
    last_feedback = None   # 마지막 피드백 저장
    display_duration = 1.5 # 점수 표시 유지 시간 (초)
    score_display_time = time.time()  # 점수 표시 시작 시간


    with mp_pose.Pose(static_image_mode=False, min_detection_confidence=0.5, min_tracking_confidence=0.5) as pose:
        while cap_expert.isOpened() and cap_webcam.isOpened():
            ret_expert, frame_expert = cap_expert.read()
            ret_cam, frame_cam = cap_webcam.read()
            ret_sil, frame_sil = cap_silhouette.read()

            frame_cam = cv2.flip(frame_cam, 1)  # [수정] 여기에 flip 추가

            if not ret_expert or not ret_cam:
                break

            # 실루엣 영상 루프 재생
            if not ret_sil:
                cap_silhouette.set(cv2.CAP_PROP_POS_FRAMES, 0)
                ret_sil, frame_sil = cap_silhouette.read()
                if not ret_sil:
                    continue

            # 프레임 비율 유지하면서 리사이즈
            frame_expert_resized = resize_with_aspect_ratio(frame_expert, 640, 480)
            frame_cam_resized = resize_with_aspect_ratio(frame_cam, 640, 480)
            frame_silhouette_resized = resize_with_aspect_ratio(frame_sil, 640, 480)

            # 사용자 쪽에 실루엣 오버레이
            blended_user = cv2.addWeighted(frame_cam_resized, 0.5, frame_silhouette_resized, 0.5, 0)

            frame_expert_rgb = cv2.cvtColor(frame_expert_resized, cv2.COLOR_BGR2RGB)
            frame_user_rgb = cv2.cvtColor(blended_user, cv2.COLOR_BGR2RGB)

            result_expert = pose.process(frame_expert_rgb)
            result_amateur = pose.process(frame_user_rgb)

            # 현재 시간 확인
            current_time = time.time()

            # 1.5초 간격으로 점수 계산
            if current_time - last_score_time >= score_interval:
                if result_expert.pose_landmarks and result_amateur.pose_landmarks:
                    # 전문가와 사용자 키포인트 추출
                    expert_keypoints = np.array([[lmk.x, lmk.y, lmk.z] for lmk in result_expert.pose_landmarks.landmark])
                    amateur_keypoints = np.array([[lmk.x, lmk.y, lmk.z] for lmk in result_amateur.pose_landmarks.landmark])
                
                    # 1차원 벡터로 변환
                    expert_keypoints_flat = expert_keypoints.flatten()
                    amateur_keypoints_flat = amateur_keypoints.flatten()
                
                    # DTW 거리 계산
                    distance, _ = fastdtw(expert_keypoints_flat[:, np.newaxis], amateur_keypoints_flat[:, np.newaxis], dist=euclidean)

                    # 점수 계산
                    max_distance = 5.5
                    score = max(100 - (distance / max_distance) * 100, 0)

                    # 점수에 따른 피드백
                    if score >= 90:
                        feedback = "Perfect"    # 멘트 정리(한단어로)
                    elif score >= 80:
                        feedback = "Good"
                    elif score >= 75:
                        feedback = "Normal"
                    elif score >= 60:
                        feedback = "Bad"
                    else:
                        feedback = "Worst"

                    # 점수와 피드백 출력
                    cv2.putText(blended_user, f"Score: {score:.2f}", (50, 50),
                                cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
                    cv2.putText(blended_user, feedback, (50, 100),
                                cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
                    
                    last_score = score
                    last_feedback = feedback
                    # 마지막 점수 계산 시간 업데이트
                    last_score_time = current_time
                    score_display_time = current_time

            if last_score is not None and last_feedback is not None and current_time - score_display_time <= display_duration:
                # 마지막 점수와 피드백을 일정 시간 동안 표시
                cv2.putText(blended_user, f"Score: {last_score:.2f}", (50, 50),
                            cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
                cv2.putText(blended_user, last_feedback, (50, 100),
                            cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)

            # 결합된 프레임 출력
            cv2.imshow("Accuracy Mode", blended_user)

            # 종료 조건 (q 키를 누르면 종료)
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break

            frame_idx += 1

    cap_expert.release()
    cap_webcam.release()
    cap_silhouette.release()
    cv2.destroyAllWindows()

    return None  # 실시간 점수 표시용이므로 값 반환 없음
