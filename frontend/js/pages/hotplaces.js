// 나만의 핫플레이스: 지도 마커와 가로 카드 목록이 서로 연동된다.
import { api } from "../common/api.js";
import { loadKakaoMaps, showMapSetup } from "../common/kakao.js";
import { initPage } from "../common/layout.js";
import { loadPhotoUrl } from "../common/photo.js";
import { $, formatLongDate, h, redirectWithNotice, showNotice } from "../common/ui.js";

const mapState = { map: null, maps: null, markers: new Map(), selectedId: null };
let hotplaces = [];

const user = await initPage({ requireAuth: true });
if (user) {
    try {
        hotplaces = await api("/api/v1/hotplaces");
    } catch (error) {
        showNotice(error.message, "error");
    }
    render();
}

function render() {
    $("hotplace-count").textContent = hotplaces.length;
    $("hotplace-empty").hidden = hotplaces.length > 0;
    $("carousel").hidden = hotplaces.length === 0;
    $("map-wrap").hidden = hotplaces.length === 0;
    if (!hotplaces.length) return;

    $("hotplace-list").replaceChildren(...hotplaces.map(createCard));
    setupCarousel();
    initMap();
}

// ---- 카드 ----
function createCard(spot) {
    const image = spot.placeImage
        ? h("img", { class: "hotplace-card-image", src: spot.placeImage, alt: spot.placeName + " 대표 사진", loading: "lazy" })
        : h("div", { class: "hotplace-card-image hotplace-card-placeholder", "aria-hidden": "true", text: "📷" });
    if (spot.photoUrl) {
        // 직접 올린 사진이 있으면 받아서 바꿔 끼운다.
        loadPhotoUrl(spot.photoUrl).then((url) => {
            image.replaceWith(h("img", { class: "hotplace-card-image", src: url, alt: spot.placeName + "에서 찍은 여행 사진" }));
        }).catch(() => { /* 대표 사진 유지 */ });
    }

    return h("article", { class: "hotplace-card", "data-id": spot.id },
        h("button", { type: "button", class: "hotplace-card-main", onclick: () => selectHotplace(spot.id, { fromMarker: false }) },
            image,
            h("span", { class: "hotplace-card-copy" },
                h("span", { class: "hotplace-card-meta" },
                    h("span", { text: spot.placeTypeLabel }),
                    h("time", { dateTime: spot.visitedDate, text: formatLongDate(spot.visitedDate) })),
                h("strong", { text: spot.placeName }),
                h("small", { text: spot.address }),
                h("p", { text: spot.description }))),
        h("div", { class: "hotplace-card-actions" },
            h("a", { class: "hotplace-edit", href: "hotplace-form.html?id=" + spot.id, text: "내용 수정" }),
            h("button", {
                type: "button", class: "hotplace-delete", text: "기록 삭제",
                onclick: () => deleteHotplace(spot)
            })));
}

async function deleteHotplace(spot) {
    if (!confirm(spot.placeName + " 핫플레이스 기록을 삭제할까요?")) return;
    try {
        await api("/api/v1/hotplaces/" + spot.id, { method: "DELETE" });
        redirectWithNotice("hotplaces.html", spot.placeName + " 기록을 삭제했습니다.");
    } catch (error) {
        showNotice(error.message, "error");
    }
}

// ---- 카드 목록 가로 스크롤 버튼 ----
function setupCarousel() {
    const list = $("hotplace-list");
    const prev = $("hotplace-prev");
    const next = $("hotplace-next");
    const update = () => {
        const hasOverflow = list.scrollWidth > list.clientWidth + 2;
        prev.hidden = !hasOverflow || list.scrollLeft <= 2;
        next.hidden = !hasOverflow || list.scrollLeft + list.clientWidth >= list.scrollWidth - 2;
    };
    const scroll = (direction) => list.scrollBy({ left: direction * Math.max(260, list.clientWidth * 0.72), behavior: "smooth" });
    prev.addEventListener("click", () => scroll(-1));
    next.addEventListener("click", () => scroll(1));
    list.addEventListener("scroll", update, { passive: true });
    window.addEventListener("resize", update);
    window.addEventListener("load", update);
    update();
}

// ---- 카드와 지도 연동 ----
function selectHotplace(id, { fromMarker }) {
    mapState.selectedId = id;
    document.querySelectorAll(".hotplace-card").forEach((card) => card.classList.toggle("active", card.dataset.id === String(id)));
    mapState.markers.forEach(({ overlay, content }, key) => {
        content.classList.toggle("selected", key === id);
        overlay.setZIndex(key === id ? 5 : 2);
    });
    const spot = hotplaces.find((item) => item.id === id);
    if (mapState.map && spot) {
        if (!fromMarker && mapState.map.getLevel() > 6) mapState.map.setLevel(6, { animate: false });
        mapState.map.setCenter(new mapState.maps.LatLng(spot.lat, spot.lng));
    }
    const card = document.querySelector('.hotplace-card[data-id="' + id + '"]');
    if (fromMarker && card) card.scrollIntoView({ behavior: "smooth", inline: "center", block: "nearest" });
    if (!fromMarker) $("map-wrap").scrollIntoView({ behavior: "smooth", block: "nearest" });
}

function makeMarker(spot) {
    const maps = mapState.maps;
    const content = h("button", {
        type: "button", class: "hotplace-marker", "aria-label": spot.placeName + " 핫플레이스 기록 보기",
        onclick: () => selectHotplace(spot.id, { fromMarker: true })
    }, h("span", { text: "📸" }));
    const position = new maps.LatLng(spot.lat, spot.lng);
    const overlay = new maps.CustomOverlay({ map: mapState.map, position, content, yAnchor: 1.15, zIndex: 2 });

    // 마커에 마우스를 올리면 장소 이름과 주소를 말풍선으로 보여준다.
    const info = h("div", { class: "custom-overlay" }, h("strong", { text: spot.placeName }), h("span", { text: spot.address }));
    const infoOverlay = new maps.CustomOverlay({ position, content: info, yAnchor: 2.4, zIndex: 6 });
    content.addEventListener("mouseenter", () => infoOverlay.setMap(mapState.map));
    content.addEventListener("mouseleave", () => infoOverlay.setMap(null));
    mapState.markers.set(spot.id, { overlay, content });
}

async function initMap() {
    try {
        const maps = await loadKakaoMaps();
        mapState.maps = maps;
        mapState.map = new maps.Map($("map"), { center: new maps.LatLng(36.5, 127.8), level: 13 });
        hotplaces.forEach(makeMarker);
        if (hotplaces.length === 1) {
            mapState.map.setCenter(new maps.LatLng(hotplaces[0].lat, hotplaces[0].lng));
            mapState.map.setLevel(5);
        } else {
            const bounds = new maps.LatLngBounds();
            hotplaces.forEach((spot) => bounds.extend(new maps.LatLng(spot.lat, spot.lng)));
            mapState.map.setBounds(bounds, 56, 56, 56, 56);
        }
    } catch (error) {
        showMapSetup($("map"), error.message);
    }
}
