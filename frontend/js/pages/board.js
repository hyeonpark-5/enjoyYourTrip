// 게시글 목록. 게시판 종류·검색 조건·페이지는 주소(?type=&key=&word=&page=)에 남겨 새로고침·뒤로가기에도 유지한다.
import { api } from "../common/api.js";
import { initPage } from "../common/layout.js";
import { $, formatDate, h, params, renderPager, showNotice } from "../common/ui.js";

const SIZE = 10;
const query = params();
const type = query.get("type") === "POST" ? "POST" : "NOTICE";

const user = await initPage();

const tab = $("tab-" + type);
tab.classList.add("active");
tab.setAttribute("aria-selected", "true");
$("key").value = query.get("key") || "";
$("word").value = query.get("word") || "";

// 공지는 관리자만, 공유게시판은 로그인한 회원 누구나 쓸 수 있다.
const canWrite = Boolean(user) && (type === "POST" || user.role === "ADMIN");
$("write").hidden = !canWrite;
$("write").href = "post-form.html?type=" + type;

const go = (page) => {
    const next = new URLSearchParams({ type });
    if ($("key").value) next.set("key", $("key").value);
    if ($("word").value.trim()) next.set("word", $("word").value.trim());
    if (page > 0) next.set("page", page);
    location.search = next.toString();
};
$("search-form").addEventListener("submit", (event) => {
    event.preventDefault();
    go(0);
});

try {
    const page = await api("/api/v1/posts", {
        query: { type, key: query.get("key"), word: query.get("word"), page: query.get("page") || 0, size: SIZE }
    });
    $("total").textContent = "총 " + page.totalElements + "건";
    $("rows").replaceChildren(...(page.content.length
        ? page.content.map((post, index) => h("tr", {},
            h("td", { class: "col-no", text: page.totalElements - page.page * page.size - index }),
            h("td", {}, h("a", { href: "post.html?id=" + post.id, text: post.title })),
            h("td", { class: "col-author", text: post.authorName }),
            h("td", { class: "col-date", text: formatDate(post.createdAt) }),
            h("td", { class: "col-views", text: post.viewCount })))
        : [h("tr", {}, h("td", { colSpan: 5, class: "board-empty", text: "등록된 글이 없습니다." }))]));
    renderPager($("pager"), page.page, page.totalPages, go);
} catch (error) {
    showNotice(error.firstMessage || error.message, "error");
}
