import { api } from "../common/api.js";
import { register } from "../common/auth.js";
import { initPage } from "../common/layout.js";
import { placeTypeOf } from "../common/kakao.js";
import { $, h } from "../common/ui.js";

// 테마 바로가기 타일 (아이콘은 고정 SVG 경로)
const TILES = [
    ["12", "관광지", "#178f70", '<path d="M4 8h3l2-3h6l2 3h3v11H4Z" /><circle cx="12" cy="13" r="3.5" />'],
    ["32", "숙박", "#435f91", '<path d="M4 11l8-7 8 7v9H4Z" /><path d="M10 20v-5h4v5" />'],
    ["39", "음식점", "#d9574f", '<path d="M4 11h16a8 8 0 0 1-16 0Z" /><path d="M9 3.5c0 2 1 2 1 4M13 3.5c0 2 1 2 1 4" />'],
    ["14", "문화시설", "#7259b5", '<path d="M3 9l9-5 9 5M5 9v9M9.5 9v9M14.5 9v9M19 9v9M3 20h18" />'],
    ["15", "축제·공연·행사", "#e98335", '<path d="M9 18V5l11-2v13" /><circle cx="6.5" cy="18" r="2.5" /><circle cx="17.5" cy="16" r="2.5" />'],
    ["25", "여행코스", "#377fca", '<path d="M12 3v18M6 5.5h11L19.5 8 17 10.5H6ZM18 13H8l-2.5 2.5L8 18h10Z" />'],
    ["28", "레포츠", "#138e96", '<circle cx="6" cy="16" r="3.5" /><circle cx="18" cy="16" r="3.5" /><path d="M6 16l4-7h5l3 7M10 9l2.5 7M13 6h3" />'],
    ["38", "쇼핑", "#d65b8c", '<path d="M5 8h14l-1 12H6Z" /><path d="M9 8V6a3 3 0 0 1 6 0v2" />']
];

function renderTiles() {
    $("theme-tile-grid").replaceChildren(...TILES.map(([code, name, color, icon]) => {
        const iconBox = h("span", { class: "theme-tile-icon", "aria-hidden": "true" });
        iconBox.innerHTML = '<svg viewBox="0 0 24 24">' + icon + "</svg>";
        const tile = h("a", { class: "theme-tile", href: "tour.html?contentTypeId=" + code }, iconBox, name);
        tile.style.setProperty("--tile-color", color);
        return tile;
    }));
}

// ---- 지금 떠나기 좋은 곳 ----
// 카드를 누르면 관광지 찾기에서 이름으로 검색하고, 같은 장소(focus)를 바로 선택해 보여준다.
function featuredCard(place, isClone) {
    const [typeName, typeIcon, typeColor] = placeTypeOf(place.contentTypeId);
    const media = h("span", { class: "featured-media" });
    if (place.image) {
        media.append(h("img", { src: place.image, alt: "", loading: "lazy" }));
    } else {
        media.textContent = typeIcon;
        media.style.background = typeColor;
    }
    return h("a", {
        class: "featured-card",
        href: "tour.html?keyword=" + encodeURIComponent(place.name) + "&focus=" + encodeURIComponent(place.id),
        tabIndex: isClone ? -1 : 0,
        "aria-hidden": isClone ? "true" : null,
        "aria-label": isClone ? null : place.name + ", 지도에서 보기"
    }, media, h("span", { class: "featured-copy" },
        h("span", { class: "featured-badge", style: { background: typeColor }, text: typeName }),
        h("strong", { text: place.name }),
        h("small", { text: place.address })));
}

async function loadFeatured() {
    try {
        const places = await api("/api/v1/tours/places/featured");
        if (!Array.isArray(places) || places.length === 0) throw new Error("no places");
        // 화면 폭보다 짧으면 비어 보이므로 최소 10장이 되도록 반복하고, 같은 묶음을 두 번 이어 붙여 끊김 없이 흐르게 한다.
        let loop = places;
        while (loop.length < 10) loop = loop.concat(places);
        const track = $("featured-track");
        track.replaceChildren(
            ...loop.map((place, index) => featuredCard(place, index >= places.length)),
            ...loop.map((place) => featuredCard(place, true)));
        track.style.setProperty("--featured-duration", (loop.length * 6) + "s");
    } catch (e) {
        $("featured-section").hidden = true;
    }
}

renderTiles();
const user = await initPage();
$("home-signup").hidden = Boolean(user);
$("home-signup").addEventListener("click", () => register());
loadFeatured();
