// <head>에서 바로 실행: 저장된 테마를 렌더링 전에 적용해 깜빡임을 막는다.
try {
    if (localStorage.getItem("enjoyTripTheme") === "dark") document.documentElement.dataset.theme = "dark";
} catch (e) { /* 저장소 접근 불가 시 라이트 모드 */ }
