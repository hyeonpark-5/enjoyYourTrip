// 로그인 상태 (Keycloak). 로그인·회원가입·비밀번호 변경 화면은 Keycloak이 제공한다.
// Access Token은 keycloak-js가 메모리에 들고 있다가 만료 전에 갱신한다. 새로고침하면 check-sso로 조용히 다시 확인한다.
import Keycloak from "https://cdn.jsdelivr.net/npm/keycloak-js@26.2.5/lib/keycloak.js";
import { api, setTokenSource } from "./api.js";

const config = window.APP_CONFIG || {};
const keycloak = config.KEYCLOAK_URL && config.KEYCLOAK_REALM && config.KEYCLOAK_CLIENT_ID
    ? new Keycloak({ url: config.KEYCLOAK_URL, realm: config.KEYCLOAK_REALM, clientId: config.KEYCLOAK_CLIENT_ID })
    : null;

let current = null;

export const isConfigured = () => Boolean(keycloak);

/** 현재 로그인한 회원 { id, email, name, role } 또는 null */
export const currentUser = () => current;

export const isAdmin = () => Boolean(current && current.role === "ADMIN");

setTokenSource({
    get: async () => {
        if (!keycloak || !keycloak.authenticated) return null;
        try {
            await keycloak.updateToken(30); // 30초 안에 만료되면 미리 갱신
        } catch (e) {
            return null; // Keycloak 세션이 끝남 → 비로그인으로 요청
        }
        return keycloak.token;
    },
    forceRefresh: async () => {
        if (!keycloak || !keycloak.authenticated) return false;
        try {
            return await keycloak.updateToken(-1);
        } catch (e) {
            return false;
        }
    }
});

/**
 * 페이지를 열 때 한 번 호출한다. Keycloak 로그인 세션이 있으면 조용히 토큰을 받아오고 내 정보를 확인한다.
 * 처음 로그인한 사용자는 이때(/members/me) 회원으로 등록된다.
 */
export async function restoreSession() {
    if (!keycloak) return null;
    try {
        const authenticated = await keycloak.init({
            onLoad: "check-sso",
            pkceMethod: "S256",
            checkLoginIframe: false,
            silentCheckSsoRedirectUri: location.origin + "/silent-check-sso.html"
        });
        current = authenticated ? await api("/api/v1/members/me") : null;
    } catch (error) {
        current = null;
        if (error && error.code === "MEMBER_WITHDRAWN") {
            // 탈퇴한 계정으로 남아 있는 Keycloak 세션은 정리한다.
            await keycloak.logout({ redirectUri: location.origin + "/index.html" });
        }
    }
    return current;
}

const here = () => location.href.split("#")[0];

/** Keycloak 로그인 화면으로 이동했다가 지금 페이지로 돌아온다. */
export function login(redirectUri = here()) {
    return keycloak.login({ redirectUri });
}

export function register(redirectUri = location.origin + "/index.html") {
    return keycloak.register({ redirectUri });
}

export function logout() {
    current = null;
    return keycloak.logout({ redirectUri: location.origin + "/index.html" });
}

/** Keycloak 계정 관리 화면 (비밀번호·이메일 변경) */
export function accountManagement() {
    return keycloak.accountManagement();
}

/** 내 정보가 바뀌었을 때(이름 수정 등) 헤더에 반영되도록 갱신한다. */
export async function refreshMe() {
    current = await api("/api/v1/members/me");
    return current;
}

/** 로그인하지 않았으면 Keycloak 로그인 화면으로 보내고 false */
export function requireLogin() {
    if (current) return true;
    if (keycloak) login();
    return false;
}
