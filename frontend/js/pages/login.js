import { login } from "../common/auth.js";
import { initPage } from "../common/layout.js";
import { $, params, redirectWithNotice, showFormError } from "../common/ui.js";

/** 로그인 후 돌아갈 곳. 같은 사이트 안의 .html 경로만 허용한다(외부 주소로 튕기는 것 방지). */
function nextUrl() {
    const next = params().get("next") || "";
    return /^[a-z-]+\.html(\?.*)?$/.test(next) ? next : "index.html";
}

const user = await initPage();
if (user) location.replace(nextUrl());

$("login-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    $("submit").disabled = true;
    $("form-message").textContent = "";
    try {
        const me = await login($("email").value.trim(), $("password").value);
        redirectWithNotice(nextUrl(), me.name + "님, 반가워요!");
    } catch (error) {
        showFormError($("form-message"), error);
        $("submit").disabled = false;
    }
});
