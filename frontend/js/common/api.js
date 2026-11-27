// 백엔드 REST API 호출. Keycloak Access Token을 붙이고, 401 INVALID_TOKEN이면 토큰을 강제로 갱신해 한 번 더 시도한다.
// 오류 응답(ProblemDetail)은 ApiError로 던진다: { status, code, message, errors[{field, message}] }

const config = window.APP_CONFIG || {};
export const API_BASE_URL = (config.API_BASE_URL || "").replace(/\/$/, "");

export class ApiError extends Error {
    constructor(status, problem) {
        super((problem && problem.detail) || "요청을 처리하지 못했습니다. (HTTP " + status + ")");
        this.status = status;
        this.code = problem && problem.code;
        this.errors = (problem && problem.errors) || [];
    }

    /** 필드 오류가 있으면 첫 번째 메시지, 없으면 detail */
    get firstMessage() {
        return this.errors.length ? this.errors[0].message : this.message;
    }
}

// auth.js가 Keycloak 초기화 후 연결한다(순환 import 방지).
let tokenSource = { get: async () => null, forceRefresh: async () => false };

/** @param {{get: () => Promise<string|null>, forceRefresh: () => Promise<boolean>}} source */
export function setTokenSource(source) {
    tokenSource = source;
}

/**
 * @param {string} path   /api/v1/... 경로 (쿼리 포함 가능)
 * @param {object} options method, json(객체 → JSON 본문), form(FormData), query(객체 → 쿼리스트링), raw(true면 Response 반환)
 */
export async function api(path, options = {}) {
    const accessToken = await tokenSource.get();
    const response = await send(path, options, accessToken);
    if (response.status === 401 && accessToken) {
        const problem = await readProblem(response.clone());
        if (problem && problem.code === "INVALID_TOKEN" && await tokenSource.forceRefresh()) {
            return handle(await send(path, options, await tokenSource.get()), options);
        }
    }
    return handle(response, options);
}

function send(path, { method = "GET", json, form, query } = {}, accessToken) {
    const headers = {};
    if (accessToken) headers.Authorization = "Bearer " + accessToken;
    let body;
    if (json !== undefined) {
        headers["Content-Type"] = "application/json";
        body = JSON.stringify(json);
    } else if (form) {
        body = form; // 브라우저가 multipart 경계(boundary)를 붙인다.
    }
    return fetch(API_BASE_URL + path + toQuery(query), { method, headers, body });
}

async function handle(response, { raw } = {}) {
    if (!response.ok) {
        throw new ApiError(response.status, await readProblem(response));
    }
    if (raw) return response;
    if (response.status === 204) return null;
    const type = response.headers.get("Content-Type") || "";
    return type.includes("json") ? response.json() : response.text();
}

async function readProblem(response) {
    try {
        return await response.json();
    } catch (e) {
        return null;
    }
}

function toQuery(query) {
    if (!query) return "";
    const params = new URLSearchParams();
    Object.entries(query).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== "") params.append(key, value);
    });
    const text = params.toString();
    return text ? "?" + text : "";
}
