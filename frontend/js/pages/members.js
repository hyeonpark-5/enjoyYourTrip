// 멤버 목록 (관리자). 검색 조건과 페이지는 주소(?key=&word=&page=)에 남겨 새로고침·뒤로가기에도 유지한다.
import { api } from "../common/api.js";
import { initPage } from "../common/layout.js";
import { $, h, params, renderPager, showNotice } from "../common/ui.js";

const user = await initPage({ requireAdmin: true });
if (user && user.role === "ADMIN") {
    const query = params();
    $("key").value = query.get("key") || "";
    $("word").value = query.get("word") || "";

    const go = (page) => {
        const next = new URLSearchParams();
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
        const page = await api("/api/v1/members", {
            query: { key: query.get("key"), word: query.get("word"), page: query.get("page") || 0, size: 10 }
        });
        $("total").textContent = page.totalElements;
        $("rows").replaceChildren(...(page.content.length
            ? page.content.map((member) => h("tr", {},
                h("td", { text: member.id }),
                h("td", { text: member.name }),
                h("td", {}, h("a", { href: "member.html?id=" + member.id, text: member.email })),
                h("td", { text: member.role })))
            : [h("tr", {}, h("td", { colSpan: 4, text: "조건에 맞는 회원이 없습니다." }))]));
        renderPager($("pager"), page.page, page.totalPages, go);
    } catch (error) {
        showNotice(error.firstMessage || error.message, "error");
    }
}
