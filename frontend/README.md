# EnjoyTrip Frontend

원본 JSP 화면을 정적 HTML + ES 모듈 JavaScript로 옮긴 프론트엔드입니다. 빌드 도구 없이 동작하며, 데이터는 모두 백엔드 REST API(`/api/v1/**`)에서 받습니다.

## 실행

1. 설정 파일 만들기 — `js/config.example.js`를 복사해 `js/config.js`로 저장하고 값을 채웁니다. (`config.js`는 깃에 올라가지 않습니다)
   - `API_BASE_URL`: 백엔드 주소
   - `KAKAO_APP_KEY`: Kakao JavaScript 키 (비우면 지도 대신 안내 문구가 나오고 나머지 기능은 동작)
2. 백엔드를 실행합니다.
3. 정적 서버 실행 (프론트 폴더에서):

   ```
   python serve.py
   ```

   → http://localhost:5500 . 백엔드 CORS 허용 주소(`app.cors.allowed-origins`)에 이 주소가 있어야 합니다.
   VS Code Live Server(5500)로 열어도 됩니다. `python -m http.server`는 Windows에서 .js를 text/plain으로 보내 모듈이 로드되지 않으므로 `serve.py`를 씁니다.

## 구조

```
*.html                 화면 (index, tour, hotplaces, hotplace-form, plans, plan-form, board, post, post-form,
                       login, signup, password-reset, member, member-form, members)
css/                   원본 스타일 그대로 (app.css 끝에 REST 전환용 규칙 추가)
assets/                메인 화면 일러스트
js/common/
  api.js               API 호출 · Access Token 첨부 · 만료 시 자동 재발급 · ProblemDetail → ApiError
  auth.js              로그인 상태 (sessionStorage), 로그인/로그아웃, requireLogin
  layout.js            공통 헤더·푸터·테마 전환, initPage()
  kakao.js             Kakao 지도 SDK 로더, 관광지 테마 색·아이콘
  photo.js             본인만 볼 수 있는 핫플레이스 사진을 Blob URL로 받기
  ui.js                DOM 생성(h), 알림, 날짜·금액 표시, 페이지 번호
  theme-init.js        저장된 다크 모드를 렌더링 전에 적용
js/pages/*.js          화면별 스크립트
```

## 인증 흐름

- 로그인하면 Access Token은 응답 본문으로 받아 탭별 `sessionStorage`에 두고, Refresh Token은 HttpOnly 쿠키로 받습니다.
- 페이지를 열 때 토큰이 없으면 쿠키로 재발급을 시도합니다(새 탭에서도 로그인 유지).
- API가 `401 INVALID_TOKEN`을 돌려주면 한 번 재발급한 뒤 같은 요청을 다시 보냅니다.

## 보안 메모

- 사용자 입력(게시글, 설명 등)은 모두 `textContent`로만 화면에 넣습니다. `innerHTML`은 고정된 아이콘 SVG에만 씁니다.
- 로그인 후 이동할 `next` 주소는 같은 사이트의 `.html` 경로만 허용합니다.
