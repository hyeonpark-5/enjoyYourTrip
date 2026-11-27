import { api } from "../common/api.js";
import { initPage } from "../common/layout.js";
import { $, formatDateTime, params, redirectWithNotice, showFormError } from "../common/ui.js";

const TYPE_NAMES = { NOTICE: "공지사항", POST: "공유게시판" };

const user = await initPage();
const id = params().get("id");

try {
    const post = await api("/api/v1/posts/" + encodeURIComponent(id));
    document.title = post.title + " | EnjoyTrip";
    $("post-type").textContent = TYPE_NAMES[post.type];
    $("post-title").textContent = post.title;
    $("post-author").textContent = post.authorName;
    $("post-created").textContent = formatDateTime(post.createdAt);
    if (post.updatedAt && formatDateTime(post.updatedAt) !== formatDateTime(post.createdAt)) {
        $("post-updated").textContent = "수정 " + formatDateTime(post.updatedAt);
        $("post-updated").hidden = false;
    }
    $("post-views").textContent = "조회 " + post.viewCount;
    $("post-content").textContent = post.content;
    $("post-list").href = "board.html?type=" + post.type;

    // 작성자 본인 또는 관리자에게만 수정·삭제 버튼을 보여준다. (서버에서도 다시 검사한다.)
    const canModify = Boolean(user) && (user.id === post.authorId || user.role === "ADMIN");
    $("post-modify").hidden = !canModify;
    $("post-delete").hidden = !canModify;
    $("post-modify").href = "post-form.html?id=" + post.id;
    $("post-delete").addEventListener("click", async () => {
        if (!confirm("정말 삭제하시겠습니까?")) return;
        try {
            await api("/api/v1/posts/" + post.id, { method: "DELETE" });
            redirectWithNotice("board.html?type=" + post.type, "게시글을 삭제했습니다.");
        } catch (error) {
            showFormError($("form-message"), error);
        }
    });
    $("post-card").hidden = false;
} catch (error) {
    showFormError($("form-message"), error);
}
