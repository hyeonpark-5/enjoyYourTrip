import { api } from "../common/api.js";
import { initPage } from "../common/layout.js";
import { $, redirectWithNotice, showFormError } from "../common/ui.js";

await initPage();

$("reset-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const message = $("form-message");
    if ($("password").value !== $("password-confirm").value) {
        message.textContent = "비밀번호 확인이 일치하지 않습니다.";
        return;
    }
    $("submit").disabled = true;
    try {
        await api("/api/v1/auth/password-reset", {
            method: "POST",
            json: { email: $("email").value.trim(), name: $("name").value.trim(), newPassword: $("password").value }
        });
        redirectWithNotice("login.html", "비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해주세요.");
    } catch (error) {
        showFormError(message, error);
        $("submit").disabled = false;
    }
});
