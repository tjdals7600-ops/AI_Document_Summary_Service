# AGENT.md

이 문서는 AI 코딩 에이전트(ChatGPT, Codex, Claude Code 등)가 이 프로젝트에서 코드를 작성하거나 수정할 때 따라야 할 규칙입니다.


가장 중요한 원칙은 다음과 같습니다.

> **복잡한 기술보다 이해하기 쉬운 코드와 작은 단계의 구현을 우선한다.**

---

## 1. 프로젝트 목적

Spring Boot와 OpenAI API를 이용한 간단한 문서 요약 API입니다.

```text
PDF / TXT 업로드
 ↓
텍스트 추출
 ↓
OpenAI API 호출
 ↓
문서 요약
 ↓
JSON 응답
```

AI 모델 학습, 복잡한 AI Pipeline, RAG 시스템 구축이 목적이 아닙니다.

---

## 2. 기본 기술 스택

```text
Java 21
Spring Boot
Gradle
Spring Web
Validation
Apache PDFBox
Spring AI OpenAI
JUnit 5
```

1차 MVP에서는 DB를 사용하지 않습니다.

---

## 3. 사용자 요청 없이 추가하지 말 것

다음 기술은 사용자가 직접 요청하지 않는 한 추가하지 않습니다.

```text
Redis
Kafka
RabbitMQ
Docker
Kubernetes
AWS
S3
MySQL
PostgreSQL
MongoDB
JPA
Spring Security
JWT
OAuth
Vector DB
pgvector
Pinecone
Elasticsearch
LangChain
RAG
Microservice
WebFlux
CQRS
Event Sourcing
```

"실무에서는 필요하다"는 이유만으로 기술을 추가하지 않습니다.

현재 목표는 **MVP를 이해하고 끝까지 완성하는 것**입니다.

---

## 4. 코드 작성 원칙

### 쉬운 코드를 우선한다

같은 기능을 여러 방식으로 구현할 수 있다면 더 이해하기 쉬운 방법을 선택합니다.

```text
복잡한 디자인 패턴     X
Controller → Service   O
```

### 과도한 추상화를 하지 않는다

다음처럼 클래스를 불필요하게 늘리지 않습니다.

```text
DocumentFacade
DocumentManager
DocumentFactory
DocumentStrategy
DocumentHandler
```

기본적으로 아래 정도를 유지합니다.

```text
DocumentController
DocumentService
AiSummaryService
SummaryResponse
GlobalExceptionHandler
```

### 이름을 명확하게 짓는다

좋은 예:

```java
extractText()
summarizeDocument()
validateFile()
```

피할 예:

```java
exec()
proc()
doTask()
```

---

## 5. 기본 프로젝트 구조

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

필요하지 않은 패키지는 만들지 않습니다.

---

## 6. 클래스별 역할

### DocumentController

```text
HTTP 요청 받기
Service 호출
HTTP 응답 반환
```

Controller에서 PDF를 읽거나 OpenAI API를 직접 호출하지 않습니다.

### DocumentService

```text
파일 검사
PDF 텍스트 추출
TXT 텍스트 추출
전체 처리 흐름 관리
```

### AiSummaryService

```text
Prompt 작성
OpenAI API 호출
요약 결과 반환
```

### SummaryResponse

클라이언트에 반환할 JSON 구조를 담당합니다.

예:

```java
public record SummaryResponse(
    String fileName,
    String summary,
    List<String> keyPoints
) {}
```

---

## 7. 구현 순서

한 번에 전체 프로젝트를 만들지 않습니다.

```text
STEP 1. Spring Boot 실행
STEP 2. MultipartFile 업로드
STEP 3. TXT 텍스트 추출
STEP 4. PDFBox로 PDF 텍스트 추출
STEP 5. OpenAI API 단독 호출 테스트
STEP 6. 문서 텍스트와 OpenAI 연결
STEP 7. SummaryResponse 반환
STEP 8. 예외 처리
STEP 9. 테스트
```

앞 단계가 동작한 뒤 다음 단계로 넘어갑니다.

---

## 8. API 규칙

MVP에서는 아래 API 하나를 중심으로 구현합니다.

```http
POST /api/documents/summarize
```

Request:

```text
multipart/form-data
file=<PDF 또는 TXT>
```

Response 예시:

```json
{
  "fileName": "spring.pdf",
  "summary": "문서 요약 내용입니다.",
  "keyPoints": [
    "핵심 포인트 1",
    "핵심 포인트 2"
  ]
}
```

사용자가 요청하지 않는 한 API를 여러 개로 나누지 않습니다.

---

## 9. 파일 규칙

처음에는 아래만 지원합니다.

```text
PDF
TXT
```

사용자가 요청하지 않는 한 DOCX, HWP, PPTX, XLSX 파서를 추가하지 않습니다.

---

## 10. OpenAI 규칙

OpenAI 호출 코드는 `AiSummaryService`에 둡니다.

Prompt는 단순하게 유지합니다.

```text
아래 문서를 한국어로 이해하기 쉽게 요약해 줘.

조건:
- 핵심 내용을 먼저 설명
- 중요한 내용은 bullet point로 정리
- 불필요하게 길게 작성하지 않기

문서:
{documentText}
```

복잡한 Prompt Template 시스템을 만들지 않습니다.

---

## 11. API Key 보안

금지:

```java
String apiKey = "sk-xxxx";
```

권장:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

실제 Key가 들어간 파일은 commit하지 않습니다.

---

## 12. DB 규칙

1차 MVP에는 DB가 없습니다.

따라서 아래도 만들지 않습니다.

```text
Entity
Repository
JPA 설정
DB Migration
```

사용자가 요약 기록 저장 기능을 요청했을 때만 DB를 추가합니다.

추가할 경우 **MySQL + JPA**를 우선합니다.

---

## 13. 예외 처리 규칙

처음에는 꼭 필요한 것만 처리합니다.

```text
빈 파일
지원하지 않는 파일 형식
PDF 읽기 실패
AI API 호출 실패
```

`GlobalExceptionHandler` 하나로 단순하게 처리합니다.

복잡한 에러 코드 체계를 만들지 않습니다.

---

## 14. 테스트 규칙

처음부터 높은 테스트 커버리지를 목표로 하지 않습니다.

우선 테스트:

```text
PDF 처리
TXT 처리
빈 파일
지원하지 않는 확장자
AI 요약 Service
```

테스트 때문에 구조를 복잡하게 만들지 않습니다.

---

## 15. 코딩 스타일

Controller는 얇게 유지합니다.

예:

```java
@PostMapping("/summarize")
public SummaryResponse summarize(
        @RequestParam("file") MultipartFile file
) {
    return documentService.summarize(file);
}
```

처리 흐름은 코드에서 눈에 보이게 작성합니다.

```java
validateFile(file);
String text = extractText(file);
String summary = aiSummaryService.summarize(text);
```

---

## 16. 주석 규칙

코드만 봐도 알 수 있는 내용에는 주석을 달지 않습니다.

초보자가 이해하기 어려운 이유나 동작만 간단히 설명합니다.

예:

```java
// PDFBox 문서는 사용 후 닫아야 하므로 try-with-resources를 사용한다.
```

---

## 17. 새로운 기술 추가 전 체크

새로운 라이브러리나 기술을 추가하기 전에 다음을 확인합니다.

```text
1. 지금 기능에 정말 필요한가?
2. 기존 Spring 기능으로 해결할 수 없는가?
3. 초보자가 이해하기 어려워지지 않는가?
4. README의 MVP 범위를 벗어나지 않는가?
```

애매하면 추가하지 않습니다.

---

## 18. MVP 완료 기준

```text
[ ] Spring Boot 실행
[ ] PDF 업로드
[ ] TXT 업로드
[ ] PDF 텍스트 추출
[ ] TXT 텍스트 추출
[ ] OpenAI API 호출
[ ] 문서 요약
[ ] JSON 응답
[ ] 기본 예외 처리
[ ] API Key 보안
```

이 체크리스트가 끝나면 1차 개발은 완료입니다.

---

## 19. 확장 순서

MVP 이후에도 한 단계씩 추가합니다.

### 1단계

```text
요약 길이 선택
요약 형식 선택
```

### 2단계

```text
MySQL
JPA
요약 기록 저장
```

### 3단계

```text
Spring Security
JWT
회원 기능
```

### 4단계

프로젝트와 AI 기술에 익숙해진 뒤:

```text
긴 문서 분할 처리
Embedding
Vector DB
RAG
Docker
AWS
```

앞 단계가 끝나기 전에 뒤 단계 기술을 미리 추가하지 않습니다.

---

## 20. AI Agent 응답 방식

사용자에게 코드를 설명할 때 가능하면 아래 순서를 지킵니다.

```text
1. 지금 무엇을 만들 것인지
2. 왜 필요한지
3. 어느 파일을 수정하는지
4. 코드
5. 코드 동작 설명
6. 실행 및 확인 방법
```

전문 용어만 나열하지 않습니다.

새로운 개념이 나오면 초보자가 이해할 수 있도록 짧게 설명합니다.

---

## 핵심 원칙

> **복잡한 기술을 많이 쓰는 것보다 Spring Boot에서 파일 업로드 → 텍스트 추출 → OpenAI API 호출 → 결과 반환 흐름을 이해하고 직접 완성하는 것을 우선한다.**
