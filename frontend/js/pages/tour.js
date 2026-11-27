// 관광지 찾기: 지역 선택 보기 → 지역/키워드 조회 결과 → 지도 확대·이동 시 주변 관광지 자동 조회.
// 주소 파라미터: areaCode, contentTypeId, keyword, focus(메인 화면 카드에서 넘어온 장소 id)
import { api } from "../common/api.js";
import { initPage } from "../common/layout.js";
import { DEFAULT_PLACE_TYPE, PLACE_TYPES, loadKakaoMaps, placeTypeOf, showMapSetup } from "../common/kakao.js";
import { $, h, params } from "../common/ui.js";

// 전국 17개 지역 (code는 TourAPI의 areaCode, 아이콘·색은 화면 표시용)
const REGIONS = [
    { code: "1", name: "서울", lat: 37.5665, lng: 126.978, icon: "🏙️", color: "#d94f70" },
    { code: "2", name: "인천", lat: 37.4563, lng: 126.7052, icon: "✈️", color: "#3977d4" },
    { code: "3", name: "대전", lat: 36.3504, lng: 127.3845, icon: "🔬", color: "#7a56b8" },
    { code: "4", name: "대구", lat: 35.8714, lng: 128.6014, icon: "🍎", color: "#d56d2d" },
    { code: "5", name: "광주", lat: 35.1595, lng: 126.8526, icon: "🎨", color: "#b84f9b" },
    { code: "6", name: "부산", lat: 35.1796, lng: 129.0756, icon: "🌊", color: "#1489a6" },
    { code: "7", name: "울산", lat: 35.5384, lng: 129.3114, icon: "⚙️", color: "#2f8f70" },
    { code: "8", name: "세종", lat: 36.48, lng: 127.289, icon: "🏛️", color: "#5f6bb2" },
    { code: "31", name: "경기", lat: 37.2636, lng: 127.0286, icon: "🎡", color: "#558b2f" },
    { code: "32", name: "강원", lat: 37.8228, lng: 128.1555, icon: "⛰️", color: "#1b806a" },
    { code: "33", name: "충북", lat: 36.6357, lng: 127.4913, icon: "🌲", color: "#8a7b2d" },
    { code: "34", name: "충남", lat: 36.6588, lng: 126.6728, icon: "🌅", color: "#bc7043" },
    { code: "35", name: "경북", lat: 36.576, lng: 128.5056, icon: "🏯", color: "#a34f35" },
    { code: "36", name: "경남", lat: 35.2383, lng: 128.6924, icon: "⚓", color: "#287c8e" },
    { code: "37", name: "전북", lat: 35.8203, lng: 127.1088, icon: "🍚", color: "#6d63a8" },
    { code: "38", name: "전남", lat: 34.8161, lng: 126.4629, icon: "🌿", color: "#3d8b58" },
    { code: "39", name: "제주", lat: 33.4996, lng: 126.5312, icon: "🍊", color: "#e56b32" }
];
// 지도 확대 수준(Kakao level)이 이 값 이하일 때만 주변 관광지를 불러온다(작을수록 확대). 더 넓게 보면 지역 선택 보기로 돌아간다.
const NEARBY_LEVEL = 10;
const NEARBY_DELAY = 550;

const query = params();
const AREA = query.get("areaCode") || "all";
const TYPE = query.get("contentTypeId") || "all";
const KEYWORD = (query.get("keyword") || "").trim();
const FOCUS_ID = query.get("focus") || "";
const SEARCHED = AREA !== "all" || KEYWORD !== "";

const state = {
    mode: SEARCHED ? "places" : "regions", // regions: 지역 선택 보기, places: 조회 결과, nearby: 지도 주변 조회 결과
    places: [],
    markers: [], // 지도에 표시 중인 오버레이(마커, 이름 말풍선)
    markerById: new Map(), // 장소 id -> { overlay, content }
    selectedId: null,
    selectedPlace: null,
    activeDetail: null, // 상세 패널에 표시 중인 정보 (설명 모달에서 사용)
    nearbySeq: 0,
    lastNearbyQuery: "",
    idleTimer: null,
    ignoreIdleUntil: 0,
    map: null,
    kakao: null
};

const REGION_ITEMS = REGIONS.map((region) => ({
    id: "region-" + region.code, isRegion: true, code: region.code, name: region.name,
    address: region.name + "의 관광지를 확인해 보세요", icon: region.icon, color: region.color,
    lat: region.lat, lng: region.lng
}));

// "all"은 API에 보내지 않는다.
const typeParam = () => ($("contentTypeId").value === "all" ? undefined : $("contentTypeId").value);

const setStatus = (message, stateName) => {
    const status = $("data-status");
    status.textContent = message;
    if (stateName) status.dataset.state = stateName;
    else delete status.dataset.state;
};

// ---- 검색 조건 ----
function fillSearchForm() {
    $("areaCode").append(...REGIONS.map((region) => h("option", { value: region.code, text: region.name })));
    $("contentTypeId").append(...Object.entries(PLACE_TYPES).map(([code, [name]]) => h("option", { value: code, text: name })));
    $("areaCode").value = REGIONS.some((region) => region.code === AREA) ? AREA : "all";
    $("contentTypeId").value = PLACE_TYPES[TYPE] ? TYPE : "all";
    $("keyword").value = KEYWORD;
}

// 지역을 고르면 해당 지역의 관광지를 조회한다. 테마 선택은 유지하고 검색어는 비운다.
const selectRegion = (code) => {
    const next = new URLSearchParams({ areaCode: code, contentTypeId: $("contentTypeId").value });
    location.href = "tour.html?" + next.toString();
};

// 지역 선택 보기에서는 선택한 관광지를 목록 맨 위에 함께 보여준다.
const listItems = () => (state.mode === "regions"
    ? [...(state.selectedPlace ? [state.selectedPlace] : []), ...REGION_ITEMS]
    : state.places);

// ---- 목록 ----
function createPlaceCard(place) {
    let media;
    let categoryText;
    if (place.isRegion) {
        media = h("span", { class: "place-number", style: { background: place.color }, text: place.icon });
        categoryText = "지역 여행지 보기";
    } else {
        const [typeName, typeIcon, typeColor] = placeTypeOf(place.contentTypeId);
        categoryText = typeIcon + " " + typeName;
        media = place.image
            ? h("span", { class: "place-thumb" }, h("img", { src: place.image, alt: "", loading: "lazy" }))
            : h("span", { class: "place-number", style: { background: typeColor }, text: typeIcon }); // 지도 마커와 같은 색
    }
    return h("button", {
        type: "button",
        class: "place-card" + (place.isRegion ? " region-card" : "") + (place.id === state.selectedId ? " active" : ""),
        "data-id": place.id,
        onclick: () => (place.isRegion ? selectRegion(place.code) : selectPlace(place.id))
    }, media, h("span", { class: "place-copy" },
        h("strong", { text: place.name }),
        h("span", { text: place.address }),
        h("small", { text: categoryText })));
}

function renderList(emptyTitle, emptyMessage) {
    $("region-overview-button").hidden = state.mode === "regions";
    const items = listItems();
    $("place-list").replaceChildren(...items.map(createPlaceCard));
    const count = $("place-count");
    count.hidden = items.length === 0;
    count.textContent = state.mode === "regions" ? REGIONS.length + "개 지역" : items.length + "곳";
    if (items.length > 0) {
        $("empty-state").hidden = true;
    } else {
        $("empty-title").textContent = emptyTitle || "조건에 맞는 여행지가 없어요.";
        $("empty-message").textContent = emptyMessage || "다른 지역이나 테마를 선택해 보세요.";
        $("empty-state").hidden = false;
    }
}

// ---- 지도 마커 ----
// 테마(관광지, 문화시설, 음식점 등)마다 다른 색과 아이콘의 마커
const makeThemeMarker = (place) => {
    const [typeName, typeIcon, typeColor] = placeTypeOf(place.contentTypeId);
    return h("button", {
        type: "button", class: "theme-marker", title: place.name + " · " + typeName,
        "aria-label": place.name + ", " + typeName,
        style: { background: typeColor, borderColor: "#fff" },
        onclick: () => selectPlace(place.id)
    }, h("span", { text: typeIcon }));
};

// 지역 선택 보기의 지역 마커 (아이콘 + 지역 이름)
const makeRegionMarker = (region) => h("button", {
    type: "button", class: "region-marker", style: { background: region.color },
    "aria-label": region.name + " 지역 여행지 보기",
    onclick: () => selectRegion(region.code)
}, h("span", { text: region.icon }), region.name);

// 선택한 장소의 마커를 강조하고 다른 마커 위로 올린다.
function updateSelectedMarker() {
    state.markerById.forEach(({ overlay, content }, id) => {
        const selected = id === state.selectedId;
        content.classList.toggle("selected", selected);
        overlay.setZIndex(selected ? 5 : 2);
    });
}

function addPlaceMarker(place) {
    const maps = state.kakao;
    const map = state.map;
    const position = new maps.LatLng(place.lat, place.lng);
    const content = makeThemeMarker(place);
    const overlay = new maps.CustomOverlay({ map, position, content, yAnchor: 1.15, zIndex: 2 });

    // 마커에 마우스를 올리면 이름과 테마를 말풍선으로 보여준다.
    const [typeName, typeIcon] = placeTypeOf(place.contentTypeId);
    const info = h("div", { class: "custom-overlay" },
        h("strong", { text: place.name }), h("span", { text: typeIcon + " " + typeName }));
    const infoOverlay = new maps.CustomOverlay({ position, content: info, yAnchor: 2.25, zIndex: 6 });
    content.addEventListener("mouseenter", () => infoOverlay.setMap(map));
    content.addEventListener("mouseleave", () => infoOverlay.setMap(null));

    state.markers.push(overlay, infoOverlay);
    state.markerById.set(place.id, { overlay, content });
}

function renderMarkers({ fitBounds }) {
    const maps = state.kakao;
    const map = state.map;
    if (!map) return;
    state.markers.forEach((overlay) => overlay.setMap(null));
    state.markers = [];
    state.markerById.clear();

    const targets = [];
    if (state.mode === "regions") {
        REGIONS.forEach((region) => {
            const position = new maps.LatLng(region.lat, region.lng);
            state.markers.push(new maps.CustomOverlay({ map, position, content: makeRegionMarker(region), yAnchor: 0.5 }));
            targets.push(position);
        });
        if (state.selectedPlace) addPlaceMarker(state.selectedPlace);
    } else {
        state.places.forEach((place) => {
            addPlaceMarker(place);
            targets.push(new maps.LatLng(place.lat, place.lng));
        });
    }
    updateSelectedMarker();

    if (fitBounds && targets.length) {
        const bounds = new maps.LatLngBounds();
        targets.forEach((position) => bounds.extend(position));
        // 지도 범위 조정으로 생기는 idle 이벤트가 주변 조회로 이어지지 않도록 잠시 무시한다.
        state.ignoreIdleUntil = Date.now() + 1200;
        map.setBounds(bounds);
    }
}

// ---- 상세 ----
const backgroundUrl = (url) => (url ? 'url("' + url.replaceAll('"', "%22") + '")' : "");

function showDetail(place, overview) {
    $("detail-panel").hidden = false;
    const [name, icon] = placeTypeOf(place.contentTypeId);
    $("detail-category").textContent = icon + " " + name;
    $("detail-title").textContent = place.name;
    $("detail-address").textContent = place.address;
    $("detail-tel").textContent = place.tel || "";
    $("detail-tel").hidden = !place.tel;
    $("detail-overview").textContent = overview;
    $("detail-media").style.backgroundImage = backgroundUrl(place.image);
    // 등록 화면이 관광지 정보를 다시 조회하므로 장소 id만 넘긴다. 로그인하지 않았으면 로그인 화면을 거친다.
    $("hotplace-add-from-detail").href = "hotplace-form.html?placeId=" + encodeURIComponent(place.id);
    $("hotplace-add-from-detail").hidden = false;

    state.activeDetail = {
        title: place.name, category: icon + " " + name, address: place.address, tel: place.tel || "",
        overview, image: place.image || ""
    };
    if (descriptionDialog.open) fillDescriptionDialog(); // 모달이 열린 동안 상세 정보가 도착하면 바로 반영
}

// ---- 여행지 상세 설명 모달 ----
const descriptionDialog = $("place-description-dialog");

function fillDescriptionDialog() {
    const detail = state.activeDetail;
    if (!detail) return;
    $("place-description-title").textContent = detail.title;
    $("place-description-category").textContent = detail.category;
    $("place-description-address").textContent = detail.address;
    $("place-description-tel").textContent = detail.tel;
    $("place-description-tel").hidden = !detail.tel;
    $("place-description-overview").textContent = detail.overview;
    $("place-description-image").style.backgroundImage = backgroundUrl(detail.image);
}

const closeDescriptionDialog = () => {
    if (descriptionDialog.open) descriptionDialog.close();
};

$("place-description-open").addEventListener("click", () => {
    if (!state.activeDetail) return;
    fillDescriptionDialog();
    descriptionDialog.showModal();
});
$("place-description-close").addEventListener("click", closeDescriptionDialog);
$("place-description-confirm").addEventListener("click", closeDescriptionDialog);
descriptionDialog.addEventListener("click", (event) => { // 모달 바깥(배경)을 누르면 닫는다.
    if (event.target === descriptionDialog) closeDescriptionDialog();
});

const findPlace = (id) => state.places.find((place) => place.id === id)
    || (state.selectedPlace && state.selectedPlace.id === id ? state.selectedPlace : null);

async function selectPlace(id) {
    const place = findPlace(id);
    if (!place) return;
    state.selectedId = id;
    state.selectedPlace = place;
    document.querySelectorAll(".place-card").forEach((card) => card.classList.toggle("active", card.dataset.id === id));
    updateSelectedMarker();
    if (state.map) {
        state.ignoreIdleUntil = Date.now() + 700; // 이동 애니메이션 중 생기는 idle은 무시
        state.map.panTo(new state.kakao.LatLng(place.lat, place.lng));
    }

    showDetail(place, "상세 정보를 불러오는 중입니다.");
    try {
        const detail = await api("/api/v1/tours/places/" + encodeURIComponent(id));
        if (state.selectedId !== id) return; // 그 사이 다른 장소를 선택한 경우
        place.image = detail.image || place.image;
        place.tel = detail.tel || place.tel;
        showDetail(place, detail.overview || "상세 소개가 등록되지 않았습니다.");
    } catch (error) {
        if (state.selectedId === id) showDetail(place, error.message || "상세 정보를 불러오지 못했습니다.");
    }
}

$("detail-close").addEventListener("click", () => {
    state.selectedId = null;
    state.selectedPlace = null;
    state.activeDetail = null;
    $("detail-panel").hidden = true;
    closeDescriptionDialog();
    if (state.mode === "regions") { // 지역 보기에서는 선택한 장소를 목록/지도에서도 치운다.
        renderList();
        renderMarkers({ fitBounds: false });
    } else {
        document.querySelectorAll(".place-card.active").forEach((card) => card.classList.remove("active"));
        updateSelectedMarker();
    }
});

// ---- 지도 이동/확대에 따른 주변 관광지, 축소 시 지역 선택 보기 ----
const radiusForLevel = (level) => (level <= 4 ? 3000 : level <= 6 ? 10000 : 20000);

function showRegionOverview() {
    state.mode = "regions";
    state.nearbySeq++; // 진행 중인 주변 조회 응답은 버린다.
    state.lastNearbyQuery = "";
    renderList();
    renderMarkers({ fitBounds: false });
    setStatus("전국 지역 선택 보기로 전환했습니다. 지역을 선택하거나 다시 확대해 보세요.", "success");
}

async function loadNearby() {
    const map = state.map;
    const level = map.getLevel();
    const center = map.getCenter();
    const radius = radiusForLevel(level);
    const contentTypeId = typeParam();
    const key = center.getLat().toFixed(3) + ":" + center.getLng().toFixed(3) + ":" + level + ":" + contentTypeId;
    if (key === state.lastNearbyQuery && state.mode === "nearby") return;
    state.lastNearbyQuery = key;

    const seq = ++state.nearbySeq;
    setStatus("현재 지도 주변의 여행지를 찾는 중입니다.");
    try {
        const data = await api("/api/v1/tours/places/nearby", {
            query: { lat: center.getLat(), lng: center.getLng(), radius, contentTypeId }
        });
        if (seq !== state.nearbySeq) return; // 더 최근 요청이 있으면 이전 응답은 버린다.
        // 선택한 장소는 지도를 움직여도 목록에 유지한다.
        const selected = state.selectedPlace;
        state.mode = "nearby";
        state.places = selected && !data.some((place) => place.id === selected.id) ? [selected, ...data] : data;
        renderList("주변에 여행지가 없어요.", "지도를 이동하거나 다른 테마를 선택해 보세요.");
        renderMarkers({ fitBounds: false });
        setStatus("지도 중심 반경 " + (radius / 1000) + "km의 여행지를 불러왔습니다.", "success");
    } catch (error) {
        if (seq !== state.nearbySeq) return;
        state.lastNearbyQuery = ""; // 실패한 위치는 다시 시도할 수 있게 한다.
        setStatus("지도 주변 데이터를 불러오지 못했습니다 (" + error.message + ").", "error");
    }
}

function onMapIdle() {
    window.clearTimeout(state.idleTimer);
    if ($("keyword").value.trim()) return; // 검색어로 찾은 결과는 지도 이동으로 바꾸지 않는다.
    if (Date.now() < state.ignoreIdleUntil) return;
    if (state.map.getLevel() > NEARBY_LEVEL) { // 너무 넓게 보고 있으면 지역 선택 보기로 돌아간다.
        if (state.mode !== "regions") showRegionOverview();
        return;
    }
    state.idleTimer = window.setTimeout(loadNearby, NEARBY_DELAY);
}

function renderLegend() {
    $("theme-legend").replaceChildren(...[...Object.values(PLACE_TYPES), DEFAULT_PLACE_TYPE].map(([name, icon, color]) =>
        h("span", { class: "legend-item" }, h("span", { class: "legend-pin", style: { background: color }, text: icon }), name)));
}

function focusInitialPlace() {
    if (!FOCUS_ID || !findPlace(FOCUS_ID)) return;
    if (state.map) {
        state.ignoreIdleUntil = Date.now() + 1200;
        state.map.setLevel(7);
    }
    selectPlace(FOCUS_ID);
}

// 전국 지역 선택 화면으로 돌아갈 때는 지금 고른 테마를 유지한다.
$("region-overview-button").addEventListener("click", (event) => {
    event.preventDefault();
    location.href = "tour.html?contentTypeId=" + encodeURIComponent($("contentTypeId").value);
});

// ---- 초기화 ----
fillSearchForm();
renderLegend();
await initPage();

if (SEARCHED) {
    setStatus("한국관광공사 데이터를 불러오는 중입니다.");
    try {
        state.places = await api("/api/v1/tours/places", {
            query: { areaCode: AREA, contentTypeId: typeParam(), keyword: KEYWORD || undefined }
        });
        setStatus("한국관광공사 데이터를 불러왔습니다. 지도를 확대하거나 이동하면 주변 여행지가 갱신됩니다.", "success");
    } catch (error) {
        setStatus(error.firstMessage || error.message, "error");
    }
}
renderList();

try {
    state.kakao = await loadKakaoMaps();
    state.ignoreIdleUntil = Date.now() + 1500; // 최초 지도 생성·범위 조정으로 인한 idle 무시
    state.map = new state.kakao.Map($("map"), { center: new state.kakao.LatLng(36.5, 127.8), level: 13 });
    state.kakao.event.addListener(state.map, "idle", onMapIdle);
    renderMarkers({ fitBounds: true });
} catch (error) {
    $("theme-legend").remove();
    showMapSetup($("map"), error.message);
}
focusInitialPlace();
