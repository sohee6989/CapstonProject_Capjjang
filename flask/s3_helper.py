import os
import boto3
import tempfile

def download_temp_from_s3(song_title: str):
    bucket_name = os.getenv("S3_BUCKET_NAME")
    if not bucket_name:
        raise EnvironmentError("S3_BUCKET_NAME 환경변수가 설정되어 있지 않습니다.")

    s3 = boto3.client("s3")

    expert_key = f"songs/{song_title}/{song_title}_expert.mp4"
    silhouette_key = f"songs/{song_title}/{song_title}_silhouette.mp4"

    temp_expert = tempfile.NamedTemporaryFile(delete=False, suffix=".mp4")
    temp_silhouette = tempfile.NamedTemporaryFile(delete=False, suffix=".mp4")

    s3.download_file(bucket_name, expert_key, temp_expert.name)
    s3.download_file(bucket_name, silhouette_key, temp_silhouette.name)

    return temp_expert.name, temp_silhouette.name
