// 글쓰기(post-form.html?type=POST) / 글 수정(post-form.html?id=3)
import { api } from "../common/api.js";
import { initPage } from "../common/layout.js";
import { $, params, redirectWithNotice, showFormError } from "../common/ui.js";

const TYPE_NAMES = { NOTICE: "공지사항", POST: "공유게시판" };

const user = await initPage({ requireAuth: true });
if (user) {
    const query = params();
    const id = query.get("id");
    let type = query.get("type") === "NOTICE" ? "NOTICE" : "POST";

    const content = $("content");
    const updateCount = () => { $("content-count").textContent = content.value.length; };
    content.addEventListener("input", updateCount);
    $("writer-name").textContent = user.name;

    try {
        if (id) {
            const post = await api("/api/v1/posts/" + encodeURIComponent(id));
            type = post.type;
            $("title").value = post.title;
            content.value = post.content;
            $("cancel").href = "post.html?id=" + post.id;
            $("submit").textContent = "수정";
        } else {
            $("cancel").href = "board.html?type=" + type;
        }
        $("form-title").textContent = TYPE_NAMES[type] + (id ? " 글 수정" : " 글쓰기");
        updateCount();
        $("post-form").hidden = false;
    } catch (error) {
        showFormError($("form-message"), error);
        $("post-form").hidden = false;
    }

    $("post-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        $("submit").disabled = true;
        const body = { title: $("title").value.trim(), content: content.value.trim() };
        try {
            const saved = id
                ? await api("/api/v1/posts/" + encodeURIComponent(id), { method: "PATCH", json: body })
                : await api("/api/v1/posts", { method: "POST", json: { ...body, type } });
            redirectWithNotice("post.html?id=" + saved.id, id ? "글을 수정했습니다." : "글을 등록했습니다.");
        } catch (error) {
            showFormError($("form-message"), error);
            $("submit").disabled = false;
        }
    });
}
