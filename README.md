# Detection of Nailfold Capillaries Using Smartphone Microscopy and Deep Learning Analysis

손톱 하부의 모세혈관경 검사는 비침습적인 방법으로, 레이노 현상, 전신 경화증, 당뇨병성 망막증 등의 질병을 진단하는 데 유용한 지표로 널리 사용되고 있습니다. 그러나 기존 검사 방법은 고가의 전문 장비와 숙련된 의료 인력을 필요로 하며, 대규모 검진이나 의료 접근성이 낮은 지역에서 활용하기 어렵다는 한계가 있습니다. 

또한, 지정된 장소에서만 사용이 가능하고 의료인의 주관적인 판단에 의존하여 결과의 일관성과 객관성을 확보하기 어려운 문제가 있습니다. 이러한 문제를 해결하기 위해, 딥러닝 기술을 기반으로 한 스마트폰 애플리케이션을 제안하여 보다 경제적이고 접근성 높은 대안을 제공하고자 합니다.

## Publication Paper 
박영욱, 계슬아, & 이언석. (2026). 스마트폰 현미경과 딥러닝 분석을 활용한 손톱 하부 모세혈관 검출. 전기학회논문지, 75(1), 166-173.

[논문 링크 바로가기](http://www.tkiee.org/kiee/XmlViewer/f450064)

## Tech Stack
<p align="left">
  <img src="https://img.shields.io/badge/Kotlin-%237F52FF?style=flat&logo=Kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Python-%233776AB?style=flat&logo=Python&logoColor=white" alt="Python"/>
  <img src="https://img.shields.io/badge/OpenAI-%234EA94B?style=flat&logo=OpenAI&logoColor=white" alt="OpenAI"/>
</p>

## Prerequisites
* **IDE:** <img src="https://img.shields.io/badge/VS_Code-007ACC?style=flat-square&logo=visualstudiocode&logoColor=white" align="center">
  <img src="https://img.shields.io/badge/Android_Studio-3DDC84?style=flat-square&logo=androidstudio&logoColor=white" align="center">
* **Python:** 3.9.0 
* **Android:** SDK API Level XX 이상 

## Features
- **Camera:** 스마트폰 카메라 모드를 활성화하여 실시간으로 이미지를 획득합니다.
- **Image Choose:** 기기 갤러리로 이동하여 사전에 확보한 이미지를 선택할 수 있습니다.
- **Preprocessing:** 선택된 이미지를 645x180 크기로 Crop(자르기) 한 후, Grayscale로 변환하여 딥러닝 분석을 위한 전처리 과정을 수행합니다.
- **UNet:** 전처리가 완료된 후, 사전에 학습된 U-Net 모델을 로드하여 분석을 준비합니다.
- **Result:** 전처리된 이미지를 서버로 전송하고, 서버 측 모델의 추론 결과를 반환받습니다.

<img width="600" height="400" alt="image" src="https://github.com/user-attachments/assets/ffb2170f-549c-45ff-9e93-1b11233446c6" />

## Results
<img width="579" height="340" alt="image" src="https://github.com/user-attachments/assets/60746690-f724-42a1-bd3c-c13e0eae977a" />


## How to Run

### 1. Server (Python)
- GPU가 연결된 서버 PC의 IP 주소를 확인합니다.
- 서버 코드를 실행하여 추론 서버를 대기 상태로 만듭니다.
```bash
git clone https://github.com/younguk072023/mobile_application.git
