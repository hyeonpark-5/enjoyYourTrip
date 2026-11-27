import { api } from "../common/api.js";
import { initPage } from "../common/layout.js";
import { $, redirectWithNotice, showFormError } from "../common/ui.js";

await initPage();

// 이메일을 입력하면 중복 여부를 확인해 is-valid / is-invalid 표시를 바꾼다.
const emailInput = $("email");
let checkSeq = 0;
emailInput.addEventListener("input", async () => {
    const seq = ++checkSeq;
    if (!emailInput.value || !emailInput.checkValidity()) {
        emailInput.classList.remove("is-valid", "is-invalid");
        return;
    }
    try {
        const { available } = await api("/api/v1/auth/email-check", { query: { email: emailInput.value.trim() } });
        if (seq !== checkSeq) return; // 그 사이 더 최근 입력이 있었다면 무시
        emailInput.classList.toggle("is-valid", available);
        emailInput.classList.toggle("is-invalid", !available);
    } catch (e) {
        emailInput.classList.remove("is-valid", "is-invalid");
    }
});

$("signup-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    $("submit").disabled = true;
    try {
        await api("/api/v1/auth/signup", {
            method: "POST",
            json: { name: $("name").value.trim(), email: emailInput.value.trim(), password: $("password").value }
        });
        redirectWithNotice("login.html", "등록되었습니다. 로그인 후 사용해주세요.");
    } catch (error) {
        showFormError($("form-message"), error);
        $("submit").disabled = false;
    }
});
