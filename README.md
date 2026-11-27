# EnjoyYourTrip

> 한국관광공사 TourAPI 데이터 기반 국내 여행 정보 서비스 풀스택 개인 프로젝트

지역별 관광지를 지도에서 찾고, 다녀온 곳을 핫플레이스로 기록하고, 여행 계획을 세우고, 다른 여행자와 게시판에서 이야기를 나누는 서비스입니다.

이 저장소는 **같은 서비스를 단계적으로 다시 만들어 온 세 번째 버전**입니다. 1, 2단계는 **부트캠프**에서 주어진 요구사항을 구현한 과제였고, 3단계는 그동안 공부한 백엔드 지식(Spring Boot, JPA, Spring Security, OAuth2와 OIDC, 캐시, 테스트)을 바탕으로 그 결과물을 **개인 프로젝트로 재설계**한 것입니다. Servlet/JSP 버전의 한계를 정리한 뒤 Spring Boot REST API와 정적 프론트엔드로 구조를 새로 짰고, 기능과 화면은 이어받으면서 저장 방식, 인증, 구조, 테스트를 바꿨습니다.

---

## 목차

1. [발전 과정](#1-발전-과정)
2. [이전 버전에서 달라진 점](#2-이전-버전에서-달라진-점)
3. [주요 기능](#3-주요-기능)
4. [기술 스택](#4-기술-스택)
5. [전체 구성](#5-전체-구성)
6. [프로젝트 구조](#6-프로젝트-구조)
7. [API](#7-api)

---

## 1. 발전 과정

| 단계 | 진행 | 형태 | 한 일 |
| --- | --- | --- | --- |
| 1 | 부트캠프 과제 | 프론트엔드 (HTML/CSS/JS) | 요구사항에 맞춰 서비스 화면과 기능 흐름을 구현 |
| 2 | 부트캠프 과제 | Servlet/JSP MVC | 관광지 조회, 회원, 핫플레이스, 여행 계획, 게시판 기능을 Tomcat 위의 JSP 화면과 Java 직렬화 파일 저장으로 구현 |
| **3 (현재)** | **개인 프로젝트** | **Spring Boot REST API + 정적 프론트엔드** | 공부한 백엔드 지식을 바탕으로, 2단계의 기능을 그대로 옮기면서 DB, 인증 서버, 캐시, 자동화 테스트를 갖춘 구조로 재설계 |

- 1, 2단계는 부트캠프에서 별도 프로젝트로 진행했고, 과제 코드는 공개하지 않습니다.
- 이 저장소의 커밋은 3단계 재설계를 **기능 단위**(공통 설정 → 회원 → 게시판 → 관광지 → 핫플레이스 → 여행계획 → 화면 → Keycloak 인증)로 나눠 기록한 것입니다.

## 2. 이전 버전에서 달라진 점

2단계(Servlet/JSP)를 마치며 정리한 "알려진 제한 사항"을 하나씩 해결하는 것이 3단계의 목표였습니다.

### 2-1. 한계와 해결

| 2단계의 한계 | 3단계에서 바꾼 것 |
| --- | --- |
| 회원 비밀번호를 평문으로 저장 | 로그인을 **Keycloak**에 맡겨 앱이 비밀번호를 아예 보관하지 않음 |
| Java 직렬화 파일(`.dat`)에 저장해서 서버 한 대만 전제 | **MySQL + JPA**로 저장하고, 테이블은 **Flyway** 마이그레이션(V1~V5)으로 관리 |
| 개발 중 서버가 다시 뜨면 로그인 세션이 사라짐 | 세션 대신 **토큰** 인증. 새로고침해도 `check-sso`로 로그인 유지 |
| 로그인, 권한 확인을 컨트롤러마다 직접 작성 | **Spring Security**가 토큰을 검증하고, `@CurrentMemberId`, `@PreAuthorize`로 한곳에서 처리 |
| `<`, `>`를 입력 단계에서 치환해서 글에 `&lt;`가 보임 | 입력은 원문 그대로 저장하고, 화면에 넣을 때 `textContent`로만 출력 |
| 자동화 테스트 없음 | API, 보안, 예외 처리 등 **70개 테스트** (실제 MySQL, Redis 사용) |

### 2-2. 구조 비교

| 구분 | 2단계 (Servlet/JSP) | 3단계 (현재) |
| --- | --- | --- |
| 빌드 / 실행 | Maven WAR + 외부 Tomcat | Gradle + Spring Boot 내장 서버 |
| 화면 | JSP 서버 렌더링 | 정적 HTML + ES 모듈 JavaScript, REST API(`/api/v1`) 호출 |
| 계층 | Controller → Service → DAO | 도메인별 Controller → Service(인터페이스 + 구현체) → Repository, DTO 변환은 Mapper로 통일 |
| 저장소 | Java 직렬화 파일 | MySQL 8 + JPA, Flyway |
| 인증 | 세션 로그인 (직접 구현) | Keycloak (OIDC Authorization Code + PKCE), JWT 검증 |
| 권한 | 컨트롤러에서 role 비교 | Keycloak realm 역할(USER/ADMIN) → Spring Security 권한 |
| 외부 API | 서버에서 TourAPI 호출 | 같은 방식에 더해 응답을 **Redis**에 캐시 |
| 에러 응답 | 403/404 상태 코드 | `ProblemDetail`(RFC 9457) + 에러 코드(`code`) |
| 비밀값 | `config.properties` | `.env` + 환경변수 (예시 파일에는 키 이름만) |
| API 문서 | 없음 | Swagger UI (springdoc) |

### 2-3. 이어받은 것과 뺀 것

- **이어받은 것:** 기능 범위, 화면 디자인과 다크 모드, 입력 검증 규칙(제목 80자, 내용 2,000자 등), "본인 기록만 조회, 수정" 같은 권한 규칙
- **뺀 것:** 관광지 날씨(Open-Meteo)는 이번 버전 범위에서 제외했습니다.

## 3. 주요 기능

| 기능 | 설명 | 권한 |
| --- | --- | --- |
| 관광지 찾기 | 지역, 테마(8종), 검색어로 관광지를 찾고 지도에 표시. 지도를 움직이면 주변 관광지를 다시 조회 | 누구나 |
| 지금 떠나기 좋은 곳 | 메인 화면에서 사진이 있는 관광지를 카드로 보여줌 | 누구나 |
| 나만의 핫플레이스 | 관광지에 사진, 방문일, 유형, 기록을 남기고 지도에 마커로 표시. 사진은 본인만 열람 | 본인 |
| 여행 계획 | 관광지를 담아 동선을 짜고(드래그로 순서 변경) 일정, 경비, 메모와 함께 저장 | 본인 |
| 게시판 | 공지사항, 공유게시판 목록(검색, 페이징), 조회수, 글쓰기, 수정, 삭제 | 조회: 누구나 / 공지 작성: ADMIN / 수정, 삭제: 작성자 또는 ADMIN |
| 회원 | Keycloak으로 회원가입, 로그인, 로그아웃, 내 정보 수정, 탈퇴 | 본인 |
| 회원 관리 | 회원 목록(검색, 페이징), 권한(USER/ADMIN) 변경 | ADMIN |

- **회원가입 연동:** 처음 로그인하면 백엔드가 회원 정보를 자동으로 만들고, 같은 이메일의 기존 회원이 있으면 그 회원에 연결합니다.
- **탈퇴:** DB에 탈퇴 시각을 기록하고 Keycloak 계정을 비활성화합니다. Keycloak 처리에 실패하면 DB 변경도 되돌립니다.
- **비밀번호, 이메일 변경:** Keycloak 계정 콘솔에서 합니다.

## 4. 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| 언어 / 빌드 | Java 21, Gradle |
| 백엔드 | Spring Boot 4.2, Spring Web MVC, Spring Data JPA, Spring Security (OAuth2 Resource Server), Validation, Cache |
| DB / 캐시 | MySQL 8, Flyway, Redis Stack |
| 인증 | Keycloak 26.8 (Docker), keycloak-js |
| 외부 API 호출 | Spring HTTP Interface (`@HttpExchange`) + RestClient |
| 프론트엔드 | HTML, CSS, Vanilla JavaScript (ES 모듈, 빌드 도구 없음) |
| 문서 / 테스트 | springdoc-openapi (Swagger UI), JUnit 5, MockMvc |

| 외부 서비스 | 용도 |
| --- | --- |
| 한국관광공사 국문 관광정보 서비스 (TourAPI) | 지역별, 키워드, 주변 관광지 조회, 상세 정보 |
| Kakao Maps JavaScript SDK | 지도, 마커, 경로선 |

## 5. 전체 구성

```
 브라우저 (정적 프론트, :5500)
   │  ① 로그인, 회원가입  ───────────────►  Keycloak (:8180)
   │  ② Access Token 받음  ◄───────────────      ▲
   │                                              │ 공개키(JWKS), Admin API
   │  ③ API 요청 + Bearer 토큰                    │ (탈퇴, 권한 변경)
   ▼                                              │
 Spring Boot API (:8080)  ────────────────────────┘
   ├── MySQL     회원, 게시글, 핫플레이스, 여행계획
   ├── Redis     TourAPI 응답 캐시
   ├── 로컬 폴더  핫플레이스 사진
   └── TourAPI   관광지 데이터 (키는 서버에만 보관)
```

- 백엔드는 토큰의 서명, 발급자, 대상(audience)을 Keycloak 공개키로 직접 확인합니다.
- 회원 역할은 Keycloak이 원본이고, DB의 역할은 요청마다 토큰에 맞춰 동기화합니다.

## 6. 프로젝트 구조

```
backend/
├── docker-compose.yml            Redis, Keycloak
├── keycloak/realm-enjoyyourtrip.json   realm, 클라이언트, 역할 자동 구성
└── src/main/java/com/hyeon/enjoyyourtrip/
    ├── domain/                   기능별 패키지
    │   ├── member/               회원, Keycloak 회원 동기화
    │   ├── post/                 공지사항, 공유게시판
    │   ├── tour/                 관광지 조회
    │   ├── hotplace/             핫플레이스
    │   └── plan/                 여행 계획
    │       └── controller, dto, entity, exception, mapper, repository, service
    ├── global/                   공통 설정, 예외 처리(ProblemDetail), 보안
    └── infrastructure/           외부 연동
        ├── tour/                 TourAPI 클라이언트
        ├── keycloak/             Keycloak 토큰, Admin API 클라이언트
        └── storage/              사진 파일 저장
frontend/
├── *.html                        화면
├── css/
└── js/
    ├── common/                   API 호출, 인증(keycloak-js), 공통 레이아웃, 지도
    └── pages/                    화면별 스크립트
```

프론트엔드 구조와 인증 흐름은 [frontend/README.md](frontend/README.md)에 자세히 적어 두었습니다.

## 7. API

모든 API는 `/api/v1` 아래에 있고, 전체 목록과 요청 형식은 Swagger UI(`/swagger-ui.html`)로 제공합니다.

| 리소스 | 주요 엔드포인트 |
| --- | --- |
| 관광지 `/tours` | `GET /regions`, `GET /places`, `GET /places/featured`, `GET /places/nearby`, `GET /places/{contentId}` |
| 게시판 `/posts` | `GET /`, `GET /{id}`, `POST /`, `PATCH /{id}`, `DELETE /{id}` |
| 핫플레이스 `/hotplaces` | `GET /place-types`, `GET /`, `GET /{id}`, `GET /{id}/photo`, `POST /`(multipart), `PATCH /{id}`(multipart), `DELETE /{id}` |
| 여행 계획 `/plans` | `GET /`, `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}` |
| 회원 `/members` | `GET /me`, `GET /`, `GET /{id}`, `PATCH /{id}`, `PATCH /{id}/role`, `DELETE /{id}` |
