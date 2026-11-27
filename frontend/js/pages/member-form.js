import { api } from "../common/api.js";
import { isAdmin, refreshMe } from "../common/auth.js";
import { initPage } from "../common/layout.js";
import { $, params, redirectWithNotice, showFormError } from "../common/ui.js";

const user = await initPage({ requireAuth: true });
if (user) {
    const id = params().get("id") || String(user.id);
    const detailUrl = id === String(user.id) ? "member.html" : "member.html?id=" + id;
    $("cancel").href = detailUrl;
    try {
        const member = await api("/api/v1/members/" + encodeURIComponent(id));
        $("name").value = member.name;
        $("email").value = member.email;
        $("role").value = member.role;
        $("role-field").hidden = !isAdmin();
        $("member-form").hidden = false;

        $("member-form").addEventListener("submit", async (event) => {
            event.preventDefault();
            $("submit").disabled = true;
            try {
                const password = $("password").value;
                await api("/api/v1/members/" + member.id, {
                    method: "PATCH",
                    json: { name: $("name").value.trim(), password: password || null }
                });
                if (isAdmin() && $("role").value !== member.role) {
                    await api("/api/v1/members/" + member.id + "/role", { method: "PATCH", json: { role: $("role").value } });
                }
                if (member.id === user.id) await refreshMe();
                redirectWithNotice(detailUrl, "회원 정보를 수정했습니다.");
            } catch (error) {
                showFormError($("form-message"), error);
                $("submit").disabled = false;
            }
        });
    } catch (error) {
        showFormError($("form-message"), error);
    }
}
