from flask import Flask, request, jsonify, send_file, send_from_directory
import torch
from torchvision import transforms
from PIL import Image
import io
import os
import logging
from flask_cors import CORS
import numpy as np
from model import build_unet  # 모델 정의
import time  # 타임스탬프 생성을 위한 모듈

app = Flask(__name__)
CORS(app, resources={r"/segment": {"origins": "*"}})  # 모든 출처에서 /segment로의 요청 허용

# 로그 파일 설정 (절대 경로로 설정)
log_file_path = 'C:\\Users\\AMI-DEEP1\\Desktop\\segmentation\\UNET\\server_errors.log'
logging.basicConfig(filename=log_file_path, level=logging.DEBUG)

@app.errorhandler(Exception)
def handle_exception(e):
    logging.exception("An error occurred during a request.")
    return jsonify({"error": str(e)}), 500

# Threshold and parse the segmentation mask
def parse_output(output):
    output = output.squeeze(0).squeeze(0).cpu().numpy()  # Remove extra dimensions (1, 1, 180, 645) -> (180, 645)
    output = (output > 0.5).astype(np.uint8)  # Apply threshold at 0.4, convert to uint8
    return output

# Image preprocessing for the UNet input
def process_image(image_path):
    transform = transforms.Compose([
        transforms.Resize((180, 645)),  # 모델이 요구하는 크기와 일치하게 리사이즈
        transforms.ToTensor(),
    ])
    image = Image.open(image_path).convert("RGB")  # RGB로 변환
    image = transform(image).unsqueeze(0)  # 배치 차원 추가
    return image

@app.route('/segment', methods=['POST'])
def segment():
    try:
        # 타임스탬프 생성
        timestamp = str(int(time.time()))

        # 이미지와 모델 파일이 요청에 포함되었는지 확인
        if 'image' not in request.files or 'unet_model' not in request.files:
            logging.error('Missing image or model file in the request')
            return jsonify({'error': 'Missing image or model file'}), 400

        # 업로드된 이미지 저장 (타임스탬프를 파일 이름에 추가하여 고유하게 생성)
        image_file = request.files['image']
        original_image_filename = f'original_{timestamp}_{image_file.filename}'
        image_path = os.path.join('C:\\Users\\AMI-DEEP1\\Desktop\\segmentation\\uploads', original_image_filename)
        image_file.save(image_path)
        logging.info(f"Image saved at {image_path}")
        
        # 업로드된 모델 파일 저장 (모델 파일도 타임스탬프를 붙여 고유하게 저장 가능)
        model_file = request.files['unet_model']
        model_path = os.path.join('C:\\Users\\AMI-DEEP1\\Desktop\\segmentation\\uploads', model_file.filename)
        model_file.save(model_path)
        logging.info(f"Model saved at {model_path}")
    except Exception as e:
        logging.error(f"File upload failed: {str(e)}")
        return jsonify({'error': 'File upload failed'}), 500
    
    try:
        # UNet 모델 로드
        device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        model = build_unet()
        model = model.to(device)

        # 모델 가중치 로드
        model.load_state_dict(torch.load(model_path, map_location=device))
        model.eval()  # 평가 모드로 전환
        logging.info("Model loaded and set to eval mode successfully")
    except Exception as e:
        logging.error(f"Failed to load model: {str(e)}")
        return jsonify({'error': 'Model loading failed'}), 500

    try:
        # 이미지 전처리
        image = process_image(image_path).to(device)
        logging.info(f"Image processed: {image.shape}")
    except Exception as e:
        logging.error(f"Image processing failed: {str(e)}")
        return jsonify({'error': 'Image processing failed'}), 500

    try:
        # 이미지 분할 수행
        with torch.no_grad():
            output = model(image)
            output = torch.sigmoid(output)  # Sigmoid로 이진 분할 수행
            logging.info(f"Segmentation output: min={output.min()}, max={output.max()}")
    except Exception as e:
        logging.error(f"Segmentation failed: {str(e)}")
        return jsonify({'error': 'Segmentation failed'}), 500

    try:
        # 결과를 파싱하여 마스크 생성
        segmented_image = parse_output(output)
        logging.info(f"Segmented image generated with shape: {segmented_image.shape}")
        
        # 단일 채널 이미지를 PIL에서 처리할 수 있도록 (180, 645) 형식으로 변환
        segmented_image_pil = Image.fromarray(segmented_image * 255, mode='L')  # 'L'은 8-bit pixels, black and white

        output_io = io.BytesIO()
        segmented_image_pil.save(output_io, format='PNG')  # PNG로 저장
        output_io.seek(0)

        # 분할된 이미지를 저장 (타임스탬프를 사용하여 고유 파일명 생성)
        segmented_image_filename = f'segmented_{timestamp}_{image_file.filename.replace(".jpg", ".png").replace(".jpeg", ".png")}'  # PNG로 저장
        result_path = os.path.join('C:\\Users\\AMI-DEEP1\\Desktop\\segmentation\\uploads', segmented_image_filename)
        segmented_image_pil.save(result_path, format='PNG')  # PNG 형식으로 저장
        logging.info(f"Segmented image saved at {result_path}")

        # 분할된 이미지 파일 이름을 응답으로 반환
        return jsonify({'segmented_image_filename': segmented_image_filename}), 200

    except Exception as e:
        logging.error(f"Failed to generate segmented image: {str(e)}")
        return jsonify({'error': 'Failed to generate segmented image'}), 500

# 업로드된 파일을 제공하기 위한 라우트 설정
@app.route('/uploads/<filename>')
def uploaded_file(filename):
    try:
        return send_from_directory('C:\\Users\\AMI-DEEP1\\Desktop\\segmentation\\uploads', filename)
    except Exception as e:
        logging.error(f"Failed to send file: {str(e)}")
        return jsonify({'error': 'File not found'}), 404

# CORS 헤더를 응답에 추가
@app.after_request
def add_cors_headers(response):
    response.headers.add('Access-Control-Allow-Origin', '*')
    response.headers.add('Access-Control-Allow-Headers', 'Content-Type,Authorization')
    response.headers.add('Access-Control-Allow-Methods', 'GET,POST,OPTIONS')
    return response

if __name__ == '__main__':
    # 업로드된 파일을 저장할 폴더가 있는지 확인하고 없으면 생성
    upload_dir = 'C:\\Users\\AMI-DEEP1\\Desktop\\segmentation\\uploads'
    try:
        if not os.path.exists(upload_dir):
            os.makedirs(upload_dir, exist_ok=True)  # 폴더가 없으면 생성
            logging.info(f"Created directory: {upload_dir}")
        else:
            logging.info(f"Directory already exists: {upload_dir}")
    except Exception as e:
        logging.error(f"Failed to create directory: {str(e)}")
    
    # Flask 서버 실행 (디버그 모드 활성화)
    app.run(host='0.0.0.0', port=5000, debug=True)
