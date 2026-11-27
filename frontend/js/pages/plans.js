import { api } from "../common/api.js";
import { initPage } from "../common/layout.js";
import { $, formatBudget, formatDistance, h, redirectWithNotice, showNotice } from "../common/ui.js";

const user = await initPage({ requireAuth: true });
if (user) {
    try {
        const plans = await api("/api/v1/plans");
        $("plan-list").replaceChildren(...(plans.length
            ? plans.map(planItem)
            : [h("p", { class: "data-status", text: "아직 저장한 여행 계획이 없습니다. 위의 \"새 여행 계획 만들기\"로 첫 계획을 만들어 보세요." })]));
    } catch (error) {
        showNotice(error.message, "error");
    }
}

function planItem(plan) {
    return h("article", { class: "saved-plan-item" },
        h("div", { class: "saved-plan-top" },
            h("strong", { text: plan.name }),
            h("span", { class: "filter-chip", text: plan.startDate + " ~ " + plan.endDate })),
        h("p", { text: plan.stopCount + "곳 · 직선거리 " + formatDistance(plan.distanceKm) + " · " + formatBudget(plan.budget) }),
        h("p", { class: "saved-plan-route", text: plan.routeSummary }),
        h("div", { class: "saved-plan-actions" },
            h("a", { class: "text-action", href: "plan-form.html?id=" + plan.id, text: "수정" }),
            h("button", { type: "button", class: "text-action", text: "삭제", onclick: () => deletePlan(plan) })));
}

async function deletePlan(plan) {
    if (!confirm("“" + plan.name + "” 여행 계획을 삭제할까요?")) return;
    try {
        await api("/api/v1/plans/" + plan.id, { method: "DELETE" });
        redirectWithNotice("plans.html", "“" + plan.name + "” 계획을 삭제했습니다.");
    } catch (error) {
        showNotice(error.message, "error");
    }
}
