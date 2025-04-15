from flask import Flask, request, jsonify
import cv2
import numpy as np
import math
import json
import boto3
import os
from pose import extract_pose_keypoints, compare_pose_directional
import logging
logging.basicConfig(level=logging.DEBUG)

app = Flask(__name__)

# ────────────────────────
# S3에서 안무가 JSON 불러오기 (캐시식 구조로 반복 요청 대비)
pose_cache = {}

def get_ref_pose_from_s3(song_title: str, frame_index: int):
    global pose_cache
    if song_title not in pose_cache:
        s3 = boto3.client("s3")
        bucket = os.environ.get("S3_BUCKET_NAME")
        key = f"songs/{song_title}/{song_title}_ref_pose_filtered_1sec_normalized.json"
        try:
            obj = s3.get_object(Bucket=bucket, Key=key)
        except Exception as e:
            app.logger.error(f"S3에서 오브젝트 불러오기 실패: {e}")
            return jsonify({"error": f"S3 error: {str(e)}"}), 500

        pose_cache[song_title] = json.loads(obj['Body'].read().decode('utf-8'))

    ref_data = pose_cache[song_title]
    ref_pose_by_frame = {entry['frame']: entry['keypoints'] for entry in ref_data}
    return ref_pose_by_frame.get(frame_index)

# ────────────────────────
# 실시간 포즈 평가 API
@app.route("/analyze", methods=["POST"])
def pose_eval():
    image = request.files.get("image")
    song_title = request.form.get("song_title")
    session_id = request.form.get("session_id")
    frame_index = int(request.form.get("frame_index", 0))

    app.logger.info(f"image={image}, song_title={song_title}, session_id={session_id}, frame_index={frame_index}")

    if not image or not song_title:
        app.logger.warning("Missing parameters")
        return jsonify({"error": "Missing parameters"}), 400

    npimg = np.frombuffer(image.read(), np.uint8)
    frame = cv2.imdecode(npimg, cv2.IMREAD_COLOR)
    
    try:
        user_pose = extract_pose_keypoints(frame)
        ref_pose = get_ref_pose_from_s3(song_title, frame_index)

        if not ref_pose:
            app.logger.warning(f"No reference pose for frame {frame_index}")
            return jsonify({"error": f"No reference pose for frame {frame_index}"}), 404

        score = compare_pose_directional(user_pose, ref_pose)
        label = "best" if score >= 90 else "good" if score >= 75 else "bad"

        result = {
            "accuracy_score": score,
            "feedback": "Great!" if score > 85 else "Keep practicing!",
            "accuracy_details": json.dumps({"frame": frame_index, "score": score}),
            "label": label,
            "frame_index": frame_index,
            "session_id": session_id
        }

        app.logger.info(f"분석 결과: {result}")
        return jsonify(result)

    except Exception as e:
        app.logger.error(f"분석 중 오류 발생: {e}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000)
