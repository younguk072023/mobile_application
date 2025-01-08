# 손톱 하부 모세혈관경 검출 시스템
손톱 하부의 모세혈관경 검사는 비침습적인 방법으로, 레이노 현상, 전신 경화증, 당뇨병성 망막증 등의 질병을 진단하는 데 유용한 지표로 널리 사용되고 있습니다. 그러나 기존 검사 방법은 고가의 전문 장비와 숙련된 의료 인력을 필요로 하며, 대규모 검진이나 의료 접근성이 낮은 지역에서 활용하기 어렵다는 한계가 있습니다. 또한, 지정된 장소에서만 사용이 가능하고 의료인의 주관적인 판단에 의존하여 결과의 일관성과 객관성을 확보하기 어려운 문제가 있습니다. 이러한 문제를 해결하기 위해, 딥러닝 기술을 기반으로 한 스마트폰 애플리케이션을 제안하여 보다 경제적이고 접근성 높은 대안을 제공하고자 합니다.

## 사용 언어
<p align="left">
  <img src="https://img.shields.io/badge/Kotlin-%237F52FF?style=flat&logo=Kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Python-%233776AB?style=flat&logo=Python&logoColor=white" alt="Python"/>
  <img src="https://img.shields.io/badge/OpenAI-%234EA94B?style=flat&logo=OpenAI&logoColor=white" alt="OpenAI"/>
</p>


## 개발 환경
<p align="left">
  <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/9/9a/Visual_Studio_Code_1.35_icon.svg/512px-Visual_Studio_Code_1.35_icon.svg.png" alt="VSCode Logo" width="180">
  <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/Android_Studio_Trademark.svg/512px-Android_Studio_Trademark.svg.png" alt="Android Studio Logo" width="300">
</p>



## 기능
- Camera button -> 스마트폰의 카메라 기능을 하는 역할로 카메라 모드로 변환을 합니다.
- Image Choose button -> 획득한 이미지나 사전에 확보한 이미지를 선택하는 앨범으로 이동을 합니다.
- Preprocessing button -> 선택한 이미지를 645x180 크기의 고정된 크기로 crop기능을합니다. 이후 grayscale로 변환을 하여 전처리 과정을 시행합니다.
- UNet radio button -> 전처리 과정을 모두 마치고 사전에 학습했던 UNet모델을 불러옵니다.
- Result button -> 전처리한 이미지와 UNet모델을 서버측 이미지에 보냅니다.


## 실행 방법
- GPU가 연결된 서버측 컴퓨터의 IP주소를 확인합니다.
- Android측 network코드에서 서버측 IP주소에 맞게 변경을 합니다.
- 서버측 컴퓨터를 실행시킵니다.
- Android Studio를 실행시켜 다운로드받습니다.



  
<p align="center">
  <img src="https://github.com/user-attachments/assets/9f24d535-9487-4b3d-97bf-149f28e7eab0" alt="Process" width="500"/>
</p>




