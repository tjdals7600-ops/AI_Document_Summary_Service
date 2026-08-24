# AI Document Summary API

문서를 업로드하면 AI가 핵심 내용을 요약해 주는 ** AI 백엔드 프로젝트**입니다.

이 프로젝트의 핵심 목표는 복잡한 AI 기술을 많이 사용하는 것이 아니라,

> **Spring Boot에서 파일을 받고 → 문서의 글자를 추출하고 → OpenAI API에 전달하고 → 요약 결과를 반환하는 흐름을 직접 구현하는 것**

입니다.

---

## 1. 프로젝트 소개

사용자가 PDF 또는 TXT 문서를 업로드하면 서버가 문서의 텍스트를 읽고, OpenAI API를 이용해 내용을 요약한 뒤 JSON으로 반환합니다.

```text
사용자
  ↓
PDF / TXT 업로드
  ↓
Spring Boot
  ↓
텍스트 추출
  ↓
OpenAI API
  ↓
요약 결과 반환
```

## 2. 프로젝트 목표

이 프로젝트를 통해 다음 내용을 연습합니다.

- Spring Boot REST API 만들기
- `MultipartFile` 파일 업로드 처리
- PDF/TXT에서 텍스트 읽기
- 외부 AI API 호출
- DTO로 JSON 응답 만들기
- 기본적인 예외 처리
- 환경 변수로 API Key 관리
- 간단한 테스트 작성

AI 모델을 직접 학습하는 프로젝트가 아니라, **이미 만들어진 LLM을 백엔드 서비스에 연결하는 방법을 배우는 프로젝트**입니다.

---

## 3. MVP 핵심 기능

### 3.1 PDF / TXT 업로드

```http
POST /api/documents/summarize
```

지원 형식:

```text
.pdf
.txt
```

처음에는 DOCX, HWP, PPTX 등은 지원하지 않습니다.

### 3.2 문서 텍스트 추출

- PDF: Apache PDFBox 사용
- TXT: 파일 내용을 문자열로 읽기

```text
파일
 ↓
텍스트 추출
 ↓
String
```

### 3.3 AI 요약

추출한 문자열을 OpenAI API에 전달합니다.

예시 Prompt:

```text
아래 문서를 한국어로 이해하기 쉽게 요약해 줘.

조건:
1. 핵심 내용 위주로 요약
2. 중요한 내용은 bullet point로 정리
3. 너무 길게 작성하지 않기

문서:
{documentText}
```

### 3.4 JSON 응답

```json
{
  "fileName": "spring.pdf",
  "summary": "이 문서는 Spring의 핵심 개념을 설명합니다.",
  "keyPoints": [
    "Spring은 Java 기반 프레임워크이다.",
    "IoC와 DI가 핵심 개념이다.",
    "Spring Boot를 사용하면 초기 설정을 줄일 수 있다."
  ]
}
```

---

## 4. 기술 스택


| 구분 | 기술 | 이유 |
|---|---|---|
| Language | Java 21 | 익숙한 Java 사용 |
| Framework | Spring Boot | REST API 개발 |
| Build Tool | Gradle | 의존성 관리 |
| Web | Spring Web | Controller 및 HTTP API |
| PDF | Apache PDFBox | PDF 텍스트 추출 |
| AI | OpenAI API + Spring AI | AI 호출을 단순화 |
| Test | JUnit 5 | 기본 테스트 |

### 1차 MVP에서 사용하지 않는 기술

- Redis
- Docker
- AWS / S3
- Kafka / RabbitMQ
- Vector DB / pgvector
- Elasticsearch
- LangChain
- RAG
- JWT / Spring Security
- MySQL / JPA
- Kubernetes
- Microservice

필요한 기능이 생겼을 때 하나씩 추가합니다.

---

## 5. 왜 DB를 빼나요?

첫 번째 버전에서는 아래 흐름만 있으면 됩니다.

```text
파일 업로드
 → 텍스트 추출
 → AI 호출
 → 결과 반환
```

요약 기록을 저장하지 않기 때문에 DB가 없어도 됩니다.

나중에 아래 기능을 만들 때 MySQL + JPA를 추가합니다.

- 요약 기록 저장
- 최근 요약 목록 조회
- 특정 요약 다시 보기
- 요약 삭제

---

## 6. 프로젝트 구조

```text
src
└─ main
   └─ java
      └─ com.example.documentsummary
         ├─ DocumentSummaryApplication.java
         ├─ controller
         │  └─ DocumentController.java
         ├─ service
         │  ├─ DocumentService.java
         │  └─ AiSummaryService.java
         ├─ dto
         │  └─ SummaryResponse.java
         └─ exception
            └─ GlobalExceptionHandler.java
```

### 클래스 역할

**DocumentController**
- 파일 요청 받기
- Service 호출
- JSON 응답 반환

**DocumentService**
- 파일 형식 검사
- PDF/TXT 텍스트 추출
- 전체 처리 흐름 담당

**AiSummaryService**
- OpenAI API 호출
- Prompt 생성
- AI 응답 받기

**SummaryResponse**
- 클라이언트에 반환할 JSON 구조 정의

---

## 7. API 설계

### 문서 요약

```http
POST /api/documents/summarize
```

Request:

```text
Content-Type: multipart/form-data
file: PDF 또는 TXT 파일
```

Response:

```json
{
  "fileName": "example.pdf",
  "summary": "문서 전체 요약 내용입니다.",
  "keyPoints": [
    "핵심 내용 1",
    "핵심 내용 2",
    "핵심 내용 3"
  ]
}
```

MVP에서는 이 API 하나만 먼저 구현합니다.

---

## 8. 전체 처리 순서

```text
1. 사용자가 파일 업로드
2. Controller가 MultipartFile 받기
3. DocumentService가 파일 형식 검사
4. PDF/TXT에서 텍스트 추출
5. AiSummaryService에 텍스트 전달
6. OpenAI API 호출
7. AI 요약 결과 받기
8. SummaryResponse 생성
9. JSON 반환
```

---

## 9. 필요한 의존성

처음에는 아래 정도만 사용합니다.

```text
Spring Web
Validation
Apache PDFBox
Spring AI OpenAI
JUnit 5
```

OpenAI 관련 라이브러리는 프로젝트의 Spring Boot 버전과 호환되는 버전을 사용합니다.

---

## 10. API Key 관리

API Key를 코드에 직접 작성하지 않습니다.

잘못된 예:

```java
String apiKey = "sk-xxxx";
```

`application.yml`에서는 환경 변수를 사용합니다.

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

실행 환경에는 다음 값을 설정합니다.

```text
OPENAI_API_KEY=본인의_API_KEY
```

실제 Key가 들어간 파일은 GitHub에 올리지 않습니다.

---

## 11. 기본 예외 처리

처음에는 아래 정도만 처리합니다.

- 파일이 비어 있음
- PDF/TXT가 아닌 파일
- 문서 내용을 읽지 못함
- AI API 호출 실패

예시:

```json
{
  "message": "PDF 또는 TXT 파일만 업로드할 수 있습니다."
}
```

---

## 12. 개발 순서

### STEP 1. Spring Boot 프로젝트 생성

서버가 정상 실행되는지 확인합니다.

### STEP 2. 파일 업로드 API

AI 없이 `MultipartFile`만 받아 봅니다.

### STEP 3. TXT 읽기

TXT 파일을 문자열로 읽습니다.

### STEP 4. PDF 읽기

PDFBox로 PDF 텍스트를 추출합니다.

### STEP 5. OpenAI 연결

간단한 문장을 AI에 보내 응답을 받아 봅니다.

### STEP 6. 문서와 AI 연결

```text
파일 → 텍스트 → OpenAI → 요약
```

### STEP 7. DTO 응답

`SummaryResponse`로 JSON을 반환합니다.

### STEP 8. 예외 처리

잘못된 파일 요청을 처리합니다.

### STEP 9. 테스트

최소 테스트:

```text
PDF 업로드 성공
TXT 업로드 성공
빈 파일
지원하지 않는 확장자
AI API 오류
```

---

## 13. MVP 완료 기준

- [ ] Spring Boot 서버가 실행된다.
- [ ] PDF를 업로드할 수 있다.
- [ ] TXT를 업로드할 수 있다.
- [ ] PDF에서 텍스트를 추출할 수 있다.
- [ ] TXT에서 텍스트를 읽을 수 있다.
- [ ] OpenAI API를 호출할 수 있다.
- [ ] AI가 문서를 요약한다.
- [ ] 요약 결과를 JSON으로 반환한다.
- [ ] 잘못된 파일 요청을 처리한다.
- [ ] API Key가 Git에 노출되지 않는다.


---

## 14. MVP 이후 확장

### Level 2

- 짧게 요약 / 자세히 요약 선택
- 핵심 포인트만 추출
- 문서 글자 수 표시

### Level 3

이때 MySQL + JPA를 추가합니다.

- 요약 결과 저장
- 요약 목록 조회
- 요약 상세 조회
- 요약 삭제

### Level 4

프로젝트에 익숙해진 뒤 공부합니다.

- Spring Security / JWT
- 긴 문서 분할 처리
- Embedding
- Vector DB
- RAG
- Docker
- AWS

---

## 15. 이 프로젝트에서 중요한 점

1. Controller와 Service 역할을 나눈다.
2. API Key를 안전하게 관리한다.
3. 잘못된 요청을 예외 처리한다.
4. 외부 AI API 호출 흐름을 이해한다.
5. 복잡한 기술보다 작은 기능을 끝까지 완성한다.

---
