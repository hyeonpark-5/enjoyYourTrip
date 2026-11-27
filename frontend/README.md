# EnjoyTrip Frontend

원본 JSP 화면을 정적 HTML + ES 모듈 JavaScript로 옮긴 프론트엔드입니다. 빌드 도구 없이 동작하며, 데이터는 모두 백엔드 REST API(`/api/v1/**`)에서 받습니다.

## 실행

1. 설정 파일 만들기 — `js/config.example.js`를 복사해 `js/config.js`로 저장하고 값을 채웁니다. (`config.js`는 깃에 올라가지 않습니다)
   - `API_BASE_URL`: 백엔드 주소
   - `KAKAO_APP_KEY`: Kakao JavaScript 키 (비우면 지도 대신 안내 문구가 나오고 나머지 기능은 동작)
   - `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`: 로그인 서버 (realm·클라이언트는 `backend/keycloak/realm-enjoyyourtrip.json`)
2. `backend` 폴더에서 `docker compose up -d`로 Redis·Keycloak을 띄우고 백엔드를 실행합니다.
3. 정적 서버 실행 (프론트 폴더에서):

   ```
   python serve.py
   ```

   → http://localhost:5500 . 백엔드 CORS 허용 주소(`app.cors.allowed-origins`)에 이 주소가 있어야 합니다.
   VS Code Live Server(5500)로 열어도 됩니다. `python -m http.server`는 Windows에서 .js를 text/plain으로 보내 모듈이 로드되지 않으므로 `serve.py`를 씁니다.

## 구조

```
*.html                 화면 (index, tour, hotplaces, hotplace-form, plans, plan-form, board, post, post-form,
                       member, member-form, members)
silent-check-sso.html  Keycloak 로그인 세션을 숨은 iframe으로 확인할 때 쓰는 빈 페이지
css/                   원본 스타일 그대로 (app.css 끝에 REST 전환용 규칙 추가)
js/common/
  api.js               API 호출 · Access Token 첨부 · INVALID_TOKEN이면 갱신 후 재시도 · ProblemDetail → ApiError
  auth.js              Keycloak(keycloak-js) 로그인·회원가입·로그아웃·계정 관리, requireLogin
  layout.js            공통 헤더·푸터·테마 전환, initPage()
  kakao.js             Kakao 지도 SDK 로더, 관광지 테마 색·아이콘
  photo.js             본인만 볼 수 있는 핫플레이스 사진을 Blob URL로 받기
  ui.js                DOM 생성(h), 알림, 날짜·금액 표시, 페이지 번호
  theme-init.js        저장된 다크 모드를 렌더링 전에 적용
js/pages/*.js          화면별 스크립트
```

## 인증 흐름 (Keycloak)

- 로그인·회원가입·비밀번호 변경 화면은 Keycloak이 제공합니다. 프론트는 `keycloak-js`로 Authorization Code + PKCE 흐름을 씁니다.
- Access Token은 `keycloak-js`가 메모리에 들고 있다가 만료 30초 전에 갱신합니다. 브라우저 저장소에는 토큰을 두지 않습니다.
- 페이지를 열 때 `check-sso`로 Keycloak 로그인 세션을 조용히 확인합니다(새로고침·새 탭에서도 로그인 유지).
- 처음 로그인하면 백엔드 `GET /api/v1/members/me`가 회원 정보를 만듭니다. 역할(USER/ADMIN)은 Keycloak realm 역할을 따릅니다.
- 회원 탈퇴 시 백엔드가 Keycloak 계정을 비활성화하고, 프론트는 Keycloak에서 로그아웃합니다.

## 보안 메모

- 사용자 입력(게시글, 설명 등)은 모두 `textContent`로만 화면에 넣습니다. `innerHTML`은 고정된 아이콘 SVG에만 씁니다.
- 로그인 후 돌아올 주소(redirect URI)는 Keycloak 클라이언트 설정에서 `http://localhost:5500/*`만 허용합니다.
