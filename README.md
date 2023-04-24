# NexusComponentDeployer
<img width="626" alt="outline" src="https://user-images.githubusercontent.com/52403430/233814648-66beb5a7-0a34-4a16-9e7e-d8669fa5b6fd.png">
NexusComponentDeployer는 편리하게 Maven dependency를 다운로드하고, 폐쇄망의 Nexus Repository로 옮겨주는 자바 어플리케이션입니다.

## 기능

1. Maven Central Repository에서 필요한 dependency를 다운로드
2. 다운로드한 dependency를 로컬 또는 원격 폐쇄망의 Nexus Repository로 업로드

## 시작하기

이 앱을 사용하려면 Nexus Repository의 REST API 기능이 작동하며, 디폴트 계정과 비밀번호로 접근 가능해야 합니다.

### 요구사항

- (컴파일) Java 11 이상, (런타임) Java 8 이상
- Nexus Repository Manager 3 이상

### 설치

1. 인터넷이 사용 가능한 PC에서 이 저장소를 클론하거나 다운로드하세요.
2. 압축을 해제한 후, 저장소의 루트 디렉토리로 이동하세요.
3. pom.xml을 수정하여 원하는 디펜던시를 정의하고 저장하세요. Maven wrapper가 포함돼 있어 별도로 설치하지 않아도 괜찮습니다.
4. 최초 빌드한 파일은 Nexus 업로드용 파일을 빌드합니다. 다음 명령을 실행하여 어플리케이션을 빌드하고 실행하세요:
```bash
java -jar NexusComponentDeployer.jar
```
5. 결과물인 myjar.jar 파일에는 필요한 디펜던시와 경량화된 HTTP Client가 포함돼 있습니다. 폐쇄망으로 옮겨 다음과 같이 실행하세요:
```bash
java -jar myjar.jar
```

## 구현이 필요한 것들

다음은 사용자 편의상 필요해 보이거나, 제한사항으로 구현/수정 예정인 기능들입니다.

- (현행) 컴파일과 런타임 버전 호환성이 달라 오해의 소지가 있음  
(개선) 구형 시스템을 지원할 수 있도록 모두 Java 1.8 버전으로 변경
- (현행) Nexus 사용자 계정, 비밀번호, REST API 주소, 빌드 결과 파일명을 지정하려면 사용자가 직접 수정해 빌드해야 함  
(개선) CLI argument로 지정할 수 있게 분리
- (현행) myjar.jar의 HTTP POST 기능 구현부가 텍스트 파일로 구현돼 있음  
(개선) 테스트, 빌드 용이성을 위해 TXT -> Java 파일로 변경
