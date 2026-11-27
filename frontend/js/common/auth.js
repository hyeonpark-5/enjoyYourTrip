// 로그인 상태. Access Token은 탭별 sessionStorage에 두고, 없으면 Refresh Token 쿠키로 조용히 재발급해 본다.
import { api, reissue, token } from "./api.js";

const ME_KEY = "enjoyTrip.me";

let current = readMe();

function readMe() {
    try {
        return JSON.parse(sessionStorage.getItem(ME_KEY));
    } catch (e) {
        return null;
    }
}

function saveMe(me) {
    current = me;
    try {
        if (me) sessionStorage.setItem(ME_KEY, JSON.stringify(me));
        else sessionStorage.removeItem(ME_KEY);
    } catch (e) { /* 저장소 사용 불가 */ }
}

/** 현재 로그인한 회원 { id, email, name, role } 또는 null */
export const currentUser = () => current;

export const isAdmin = () => Boolean(current && current.role === "ADMIN");

/** 페이지를 열 때 한 번 호출한다. 토큰이 없으면 쿠키로 재발급을 시도하고, 있으면 내 정보를 확인한다. */
export async function restoreSession() {
    if (!token.get() && !(await reissue())) {
        saveMe(null);
        return null;
    }
    try {
        saveMe(await api("/api/v1/members/me"));
    } catch (e) {
        token.clear();
        saveMe(null);
    }
    return current;
}

export async function login(email, password) {
    const body = await api("/api/v1/auth/login", { method: "POST", json: { email, password }, noRetry: true });
    token.set(body.accessToken);
    saveMe(await api("/api/v1/members/me"));
    return current;
}

export async function logout() {
    try {
        if (token.get()) await api("/api/v1/auth/logout", { method: "POST" });
    } catch (e) { /* 이미 만료된 토큰이어도 로컬 상태는 지운다 */ }
    clearSession();
}

/** 서버 호출 없이 로컬 로그인 상태만 지운다 (탈퇴 직후 등) */
export function clearSession() {
    token.clear();
    saveMe(null);
}

/** 내 정보가 바뀌었을 때(이름 수정 등) 헤더에 반영되도록 갱신한다. */
export async function refreshMe() {
    saveMe(await api("/api/v1/members/me"));
    return current;
}

/** 로그인하지 않았으면 로그인 화면으로 보내고 false */
export function requireLogin() {
    if (current) return true;
    const next = location.pathname.split("/").pop() + location.search;
    location.replace("login.html?next=" + encodeURIComponent(next));
    return false;
}
