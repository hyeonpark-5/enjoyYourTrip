// 화면 공통 도우미 (알림, 날짜·금액 표시, 쿼리 파라미터, 페이지 이동)

export const $ = (id) => document.getElementById(id);

export const params = () => new URLSearchParams(location.search);

/** 원하는 태그를 만들고 속성·자식을 붙인다. 문자열 자식은 textContent로 들어가므로 HTML로 해석되지 않는다. */
export function h(tag, attrs = {}, ...children) {
    const node = document.createElement(tag);
    Object.entries(attrs).forEach(([key, value]) => {
        if (value === undefined || value === null || value === false) return;
        if (key === "class") node.className = value;
        else if (key === "text") node.textContent = value;
        else if (key === "style") Object.assign(node.style, value);
        else if (key.startsWith("on")) node.addEventListener(key.slice(2), value);
        else if (key in node && key !== "list") node[key] = value;
        else node.setAttribute(key, value === true ? "" : value);
    });
    children.flat().forEach((child) => {
        if (child === undefined || child === null || child === false) return;
        node.append(child instanceof Node ? child : document.createTextNode(String(child)));
    });
    return node;
}

// ---- 상단 알림 ----
let noticeTimer = null;

export function showNotice(message, state) {
    const notice = $("app-notice");
    if (!notice || !message) return;
    window.clearTimeout(noticeTimer);
    notice.textContent = message;
    if (state) notice.dataset.state = state;
    else delete notice.dataset.state;
    notice.hidden = false;
    noticeTimer = window.setTimeout(() => { notice.hidden = true; }, 3600);
}

/** 다음 페이지에서 보여줄 알림을 남기고 이동한다. */
export function redirectWithNotice(url, message) {
    try {
        sessionStorage.setItem("enjoyTrip.flash", message);
    } catch (e) { /* 무시 */ }
    location.href = url;
}

export function consumeFlash() {
    try {
        const message = sessionStorage.getItem("enjoyTrip.flash");
        sessionStorage.removeItem("enjoyTrip.flash");
        return message;
    } catch (e) {
        return null;
    }
}

// ---- 표시 형식 ----
const pad = (n) => String(n).padStart(2, "0");

/** "2026-11-27T12:34:56.789" → "2026-11-27" */
export const formatDate = (value) => (value ? String(value).slice(0, 10) : "");

/** "2026-11-27T12:34:56.789" → "2026-11-27 12:34" */
export function formatDateTime(value) {
    if (!value) return "";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);
    return date.getFullYear() + "-" + pad(date.getMonth() + 1) + "-" + pad(date.getDate())
        + " " + pad(date.getHours()) + ":" + pad(date.getMinutes());
}

/** "2026-10-30" → "2026. 10. 30." */
export function formatLongDate(value) {
    const date = new Date(value + "T00:00:00");
    return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium" }).format(date);
}

export const formatBudget = (won) => (won > 0 ? Number(won).toLocaleString("ko-KR") + "원" : "경비 미정");

export function formatDistance(km) {
    if (!km) return "0 km";
    return km < 10 ? km.toFixed(1) + " km" : Math.round(km) + " km";
}

/** 오늘 날짜 "yyyy-MM-dd" (브라우저 시간대) */
export function today() {
    const now = new Date();
    return now.getFullYear() + "-" + pad(now.getMonth() + 1) + "-" + pad(now.getDate());
}

// ---- 페이지 번호 (10개 단위 묶음, page는 0부터) ----
export function renderPager(container, page, totalPages, onSelect) {
    if (!totalPages) {
        container.replaceChildren();
        return;
    }
    const groupStart = Math.floor(page / 10) * 10;
    const groupEnd = Math.min(groupStart + 9, totalPages - 1);
    const item = (label, target, active) => h("li", { class: active ? "active" : "" },
        h("a", { href: "#", text: label, onclick: (event) => { event.preventDefault(); onSelect(target); } }));
    const items = [];
    if (groupStart > 0) items.push(item("이전", groupStart - 1));
    for (let i = groupStart; i <= groupEnd; i++) items.push(item(String(i + 1), i, i === page));
    if (groupEnd < totalPages - 1) items.push(item("다음", groupEnd + 1));
    container.replaceChildren(...items);
}

/** 서버 오류를 폼 메시지 영역에 보여준다. */
export function showFormError(target, error) {
    target.textContent = error && error.firstMessage ? error.firstMessage : (error && error.message) || "요청을 처리하지 못했습니다.";
}
