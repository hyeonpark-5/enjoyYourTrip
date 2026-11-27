// 회원 정보 (member.html = 내 정보, member.html?id=3 = 관리자가 보는 다른 회원)
import { api } from "../common/api.js";
import { accountManagement, isAdmin, logout } from "../common/auth.js";
import { initPage } from "../common/layout.js";
import { $, formatDate, params, redirectWithNotice, showFormError } from "../common/ui.js";

const user = await initPage({ requireAuth: true });
if (user) {
    const id = params().get("id");
    try {
        const member = await api(id ? "/api/v1/members/" + encodeURIComponent(id) : "/api/v1/members/me");
        const isSelf = member.id === user.id;
        $("member-avatar").textContent = member.name.slice(0, 1);
        $("member-name").textContent = member.name;
        $("member-email-head").textContent = member.email;
        $("member-id").textContent = member.id;
        $("member-email").textContent = member.email;
        $("member-role").textContent = member.role;
        $("member-created").textContent = formatDate(member.createdAt);
        $("member-modify").href = "member-form.html?id=" + member.id;
        // 비밀번호·이메일은 Keycloak 계정 관리 화면에서 바꾼다 (본인만).
        $("member-account").hidden = !isSelf;
        $("member-account").addEventListener("click", () => accountManagement());
        $("member-profile").hidden = false;

        $("member-delete").addEventListener("click", async () => {
            const question = isSelf ? "정말 탈퇴하시겠습니까? 작성한 글은 남습니다." : member.name + " 회원을 탈퇴시킬까요?";
            if (!confirm(question)) return;
            try {
                await api("/api/v1/members/" + member.id, { method: "DELETE" });
                if (isSelf) {
                    await logout(); // Keycloak 로그아웃 후 메인으로 돌아간다.
                } else {
                    redirectWithNotice(isAdmin() ? "members.html" : "index.html", member.name + " 회원을 탈퇴시켰습니다.");
                }
            } catch (error) {
                showFormError($("form-message"), error);
            }
        });
    } catch (error) {
        showFormError($("form-message"), error);
    }
}
