// 모든 페이지 공통: 헤더·푸터를 그리고 로그인 상태를 확인한다.
// 사용: <body data-page="tour"> + <div id="site-header"></div> ... <div id="site-footer"></div>
//       const user = await initPage({ requireAuth: true });
import { API_BASE_URL } from "./api.js";
import { currentUser, isAdmin, logout, requireLogin, restoreSession } from "./auth.js";
import { $, consumeFlash, h, showNotice } from "./ui.js";

const NAV = [
    { page: "tour", href: "tour.html", label: "관광지 찾기" },
    { page: "hotplace", href: "hotplaces.html", label: "핫플레이스" },
    { page: "plan", href: "plans.html", label: "여행 계획" },
    { page: "board", href: "board.html?type=NOTICE", label: "커뮤니티" },
    { page: "members", href: "members.html", label: "멤버 목록", adminOnly: true }
];

const brandMark = () => {
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("class", "brand-mark");
    svg.setAttribute("viewBox", "0 0 32 32");
    svg.setAttribute("aria-hidden", "true");
    ["M3 13c3.5-3 6.5-3 10 0s6.5 3 10 0 4.5-2.5 6-1.5", "M3 21c3.5-3 6.5-3 10 0s6.5 3 10 0 4.5-2.5 6-1.5"]
        .forEach((d) => {
            const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
            path.setAttribute("d", d);
            svg.append(path);
        });
    return svg;
};

const brandWord = () => h("span", { class: "brand-word" }, "enjoy", h("span", { text: "trip" }));

function renderHeader() {
    const activePage = document.body.dataset.page;
    const nav = h("nav", { class: "main-nav", "aria-label": "주요 메뉴" },
        NAV.filter((item) => !item.adminOnly || isAdmin())
            .map((item) => h("a", { href: item.href, class: item.page === activePage ? "active" : "", text: item.label })));

    const themeToggle = h("button", { id: "theme-toggle", class: "theme-toggle", type: "button" },
        h("span", { class: "theme-toggle-track", "aria-hidden": "true" },
            h("span", { class: "theme-sun", text: "☀" }),
            h("span", { class: "theme-moon", text: "☾" }),
            h("span", { class: "theme-toggle-thumb" })));

    const user = currentUser();
    const account = user
        ? [
            h("a", { class: "account-button", href: "member.html", text: "내 정보 · " + user.name }),
            h("button", {
                class: "account-button account-button-primary", type: "button", text: "로그아웃",
                onclick: async () => {
                    await logout();
                    location.href = "index.html";
                }
            })
        ]
        : [
            h("a", { class: "account-button", href: "signup.html", text: "회원가입" }),
            h("a", { class: "account-button account-button-primary", href: "login.html", text: "로그인" })
        ];

    const header = h("header", { class: "site-header" },
        h("div", { class: "header-inner" },
            h("a", { class: "brand", href: "index.html", "aria-label": "EnjoyTrip 홈" }, brandMark(), brandWord()),
            nav,
            h("div", { class: "account-actions" }, themeToggle, ...account)));

    const container = $("site-header");
    container.replaceWith(
        h("a", { class: "skip-link", href: "#main", text: "본문 바로가기" }),
        header,
        h("div", { id: "app-notice", class: "app-notice", role: "status", "aria-live": "assertive", hidden: true }));

    setupTheme(themeToggle);
    placeNavUnderline();
    window.addEventListener("load", placeNavUnderline);
}

function renderFooter() {
    const container = $("site-footer");
    if (!container) return;
    container.replaceWith(h("footer", { class: "site-footer" },
        h("div", { class: "footer-inner footer-top" },
            h("div", { class: "footer-brand" },
                h("span", { class: "footer-logo" }, brandMark(), brandWord()),
                h("p", { text: "지역과 테마로 국내 관광지를 찾고, 다녀온 여행을 기록하고 나누는 여행 탐색 서비스" })),
            h("nav", { class: "footer-nav", "aria-label": "하단 메뉴" },
                NAV.filter((item) => !item.adminOnly).map((item) => h("a", { href: item.href, text: item.label })))),
        h("div", { class: "footer-inner footer-bottom" },
            h("span", { text: "관광 정보: 한국관광공사 TourAPI · 지도: Kakao Maps" }),
            h("span", { text: "© 2026 enjoytrip" }))));
}

// 현재 메뉴 아래 밑줄 위치
function placeNavUnderline() {
    const nav = document.querySelector(".main-nav");
    const active = nav && nav.querySelector("a.active");
    if (!active) return;
    nav.style.setProperty("--nav-underline-left", active.offsetLeft + "px");
    nav.style.setProperty("--nav-underline-width", active.offsetWidth + "px");
}

// 라이트/다크 테마 전환 (선택은 브라우저에 저장)
function setupTheme(toggle) {
    const apply = (isDark) => {
        document.documentElement.dataset.theme = isDark ? "dark" : "light";
        toggle.setAttribute("aria-pressed", String(isDark));
        toggle.setAttribute("aria-label", isDark ? "라이트 모드로 전환" : "다크 모드로 전환");
    };
    apply(document.documentElement.dataset.theme === "dark");
    toggle.addEventListener("click", () => {
        const isDark = document.documentElement.dataset.theme !== "dark";
        apply(isDark);
        try {
            localStorage.setItem("enjoyTripTheme", isDark ? "dark" : "light");
        } catch (e) { /* 무시 */ }
    });
}

/**
 * 로그인 상태를 확인하고 헤더·푸터를 그린다.
 * @param {{requireAuth?: boolean, requireAdmin?: boolean}} options
 * @returns 로그인한 회원 또는 null (로그인이 필요한데 안 했으면 로그인 화면으로 이동하고 null)
 */
export async function initPage({ requireAuth = false, requireAdmin = false } = {}) {
    const user = API_BASE_URL ? await restoreSession() : null;
    renderHeader();
    renderFooter();
    if (!API_BASE_URL) {
        showNotice("js/config.js가 없습니다. config.example.js를 복사해 API_BASE_URL을 설정하세요.", "error");
    }
    const flash = consumeFlash();
    if (flash) showNotice(flash);
    if ((requireAuth || requireAdmin) && !requireLogin()) return null;
    if (requireAdmin && !isAdmin()) {
        document.querySelector("main").replaceChildren(
            h("p", { class: "data-status", "data-state": "error", text: "관리자만 볼 수 있는 화면입니다." }));
        return null;
    }
    return user;
}
