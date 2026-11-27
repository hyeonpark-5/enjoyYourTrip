// 여행 계획 만들기(plan-form.html) / 수정(plan-form.html?id=3)
// 관광지를 검색해 동선에 담고, 순서를 바꾸고, 지도에 경로와 총 직선거리를 표시한다. 저장 시 서버가 다시 검증한다.
import { api } from "../common/api.js";
import { loadKakaoMaps } from "../common/kakao.js";
import { initPage } from "../common/layout.js";
import { $, formatDistance, h, params, redirectWithNotice, showFormError } from "../common/ui.js";

const EARTH_RADIUS_KM = 6371;
const DEFAULT_CENTER = { lat: 36.35, lng: 127.75 };
const SEARCH_LIMIT = 12;

const editId = params().get("id");

// route: 방문 순서대로 나열한 장소 [{ placeId, name, address, lat, lng, contentTypeId }]
let route = [];
let searchResults = [];
let draggedIndex = null;
let searchSeq = 0;
let map = null;
let maps = null;
let mapObjects = []; // 지도의 번호 마커와 경로선

async function loadPlan() {
    try {
        const plan = await api("/api/v1/plans/" + encodeURIComponent(editId));
        document.title = "여행 계획 수정 | EnjoyTrip";
        $("page-title").textContent = "여행 계획 수정";
        $("plan-save").textContent = "수정사항 저장";
        $("plan-name").value = plan.name;
        $("plan-budget").value = plan.budget > 0 ? plan.budget : "";
        $("plan-start-date").value = plan.startDate;
        $("plan-end-date").value = plan.endDate;
        $("plan-notes").value = plan.notes;
        route = plan.stops.map(toStop);
    } catch (error) {
        showFormError($("plan-form-message"), error);
    }
}

function toStop(source) {
    return {
        placeId: String(source.placeId),
        name: String(source.name),
        address: String(source.address || ""),
        lat: Number(source.lat),
        lng: Number(source.lng),
        contentTypeId: String(source.contentTypeId || "")
    };
}

// ---- 거리 (하버사인 공식, 서버의 TripPlan.distanceKm과 같은 방식) ----
function distanceKm(stops) {
    const rad = (degrees) => degrees * Math.PI / 180;
    let total = 0;
    for (let i = 1; i < stops.length; i++) {
        const lat1 = rad(stops[i - 1].lat);
        const lat2 = rad(stops[i].lat);
        const deltaLat = lat2 - lat1;
        const deltaLng = rad(stops[i].lng - stops[i - 1].lng);
        const a = Math.sin(deltaLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) ** 2;
        total += EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
    return total;
}

// ---- 동선 편집 ----
const hasStop = (placeId) => route.some((stop) => stop.placeId === String(placeId));

function addStop(place) {
    if (hasStop(place.id)) {
        $("plan-search-status").textContent = place.name + "은(는) 이미 여행 동선에 추가되어 있습니다.";
        return;
    }
    route.push(toStop({ ...place, placeId: place.id }));
    $("plan-search-status").textContent = place.name + "을(를) 여행 동선에 추가했습니다.";
    renderRoute();
}

function removeStop(index) {
    route.splice(index, 1);
    renderRoute();
}

function moveStop(from, to) {
    if (from === to || from < 0 || to < 0 || from >= route.length || to >= route.length) return;
    const [moved] = route.splice(from, 1);
    route.splice(to, 0, moved);
    renderRoute();
}

const actionButton = (className, text, label, handler, disabled) =>
    h("button", { type: "button", class: className, text, "aria-label": label, disabled: Boolean(disabled), onclick: handler });

function createRouteItem(stop, index) {
    const item = h("li", { class: "plan-route-item", draggable: true, "data-index": index },
        h("span", { class: "route-order", text: index + 1 }),
        h("div", { class: "route-copy" }, h("strong", { text: stop.name }), h("span", { text: stop.address })),
        // 드래그가 어려운 화면(터치 등)을 위해 위/아래 버튼으로도 순서를 바꿀 수 있다.
        h("div", { class: "route-actions" },
            actionButton("route-move", "↑", stop.name + " 위로 이동", () => moveStop(index, index - 1), index === 0),
            actionButton("route-move", "↓", stop.name + " 아래로 이동", () => moveStop(index, index + 1), index === route.length - 1),
            actionButton("route-remove", "삭제", stop.name + " 삭제", () => removeStop(index), false)));

    item.addEventListener("dragstart", () => {
        draggedIndex = index;
        item.classList.add("dragging");
    });
    item.addEventListener("dragend", () => item.classList.remove("dragging"));
    item.addEventListener("dragover", (event) => event.preventDefault());
    item.addEventListener("drop", (event) => {
        event.preventDefault();
        const from = draggedIndex;
        draggedIndex = null;
        if (from !== null) moveStop(from, index);
    });
    return item;
}

function renderRoute() {
    $("plan-route-list").replaceChildren(...route.map(createRouteItem));
    $("plan-route-count").textContent = route.length + "곳";
    $("plan-route-distance").textContent = "총 직선거리 " + formatDistance(distanceKm(route));
    if (searchResults.length) renderSearchResults();
    renderMap();
}

// ---- 관광지 검색 ----
function renderSearchResults() {
    $("plan-search-results").replaceChildren(...searchResults.map((place) => {
        const added = hasStop(place.id);
        return h("div", { class: "plan-search-item" + (added ? " selected" : "") },
            h("div", {}, h("strong", { text: place.name }), h("span", { text: place.address })),
            actionButton("", added ? "✓ 추가됨" : "+ 추가", place.name + " 동선에 추가", () => addStop(place), added));
    }));
}

async function searchPlaces() {
    const keyword = $("plan-place-search").value.trim();
    const status = $("plan-search-status");
    if (!keyword) {
        status.textContent = "검색할 관광지명을 입력해 주세요.";
        return;
    }
    const seq = ++searchSeq;
    $("plan-place-search-button").disabled = true;
    status.textContent = "“" + keyword + "” 검색 중입니다.";
    try {
        const data = await api("/api/v1/tours/places", { query: { keyword } });
        if (seq !== searchSeq) return; // 더 최근 검색이 있으면 이전 응답은 버린다.
        searchResults = data.slice(0, SEARCH_LIMIT);
        renderSearchResults();
        status.textContent = searchResults.length
            ? searchResults.length + "곳을 찾았습니다. 원하는 장소를 추가하세요."
            : "검색 결과가 없습니다.";
    } catch (error) {
        if (seq !== searchSeq) return;
        status.textContent = "여행지를 검색하지 못했습니다. 잠시 후 다시 시도해 주세요.";
    } finally {
        if (seq === searchSeq) $("plan-place-search-button").disabled = false;
    }
}

// ---- 지도 ----
function renderMap() {
    if (!map) return;
    mapObjects.forEach((object) => object.setMap(null));
    mapObjects = [];
    if (!route.length) {
        map.setCenter(new maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng));
        map.setLevel(13);
        return;
    }
    const bounds = new maps.LatLngBounds();
    const path = route.map((stop, index) => {
        const position = new maps.LatLng(stop.lat, stop.lng);
        bounds.extend(position);
        const marker = h("span", {
            class: "route-map-marker", title: stop.name, text: index + 1,
            "aria-label": (index + 1) + "번째 장소: " + stop.name
        });
        mapObjects.push(new maps.CustomOverlay({ map, position, content: marker, yAnchor: 1.1 }));
        return position;
    });
    if (path.length > 1) {
        mapObjects.push(new maps.Polyline({
            map, path, strokeWeight: 4, strokeColor: "#0a6fb5", strokeOpacity: 0.85, strokeStyle: "solid"
        }));
        map.setBounds(bounds, 48, 48, 48, 48);
    } else {
        map.setCenter(path[0]);
        map.setLevel(6);
    }
}

async function initMap() {
    const container = $("plan-map");
    try {
        maps = await loadKakaoMaps();
        container.replaceChildren();
        map = new maps.Map(container, { center: new maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng), level: 13 });
        map.addControl(new maps.ZoomControl(), maps.ControlPosition.RIGHT);
        renderMap();
    } catch (error) {
        container.replaceChildren(h("div", { class: "plan-map-placeholder" },
            h("p", { text: error.message + " (동선 편집과 저장은 지도 없이도 가능합니다.)" })));
    }
}

// ---- 일정 ----
// 도착일은 출발일보다 빠를 수 없으므로, 출발일이 바뀌면 최소값을 맞추고 필요하면 도착일을 보정한다.
function syncDateRange(announce) {
    const start = $("plan-start-date").value;
    const end = $("plan-end-date").value;
    if (start) $("plan-end-date").min = start;
    else $("plan-end-date").removeAttribute("min");
    if (!start || !end || end >= start) return;
    $("plan-end-date").value = start;
    if (announce) $("plan-form-message").textContent = "도착일을 출발일과 같은 날짜로 조정했습니다.";
}

function setupEvents() {
    $("plan-place-search-button").addEventListener("click", searchPlaces);
    $("plan-place-search").addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            searchPlaces();
        }
    });
    $("plan-start-date").addEventListener("change", () => syncDateRange(true));
    $("plan-end-date").addEventListener("change", () => syncDateRange(true));

    $("plan-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const message = $("plan-form-message");
        if (!route.length) {
            message.textContent = "여행 동선에 관광지를 한 곳 이상 추가해 주세요.";
            return;
        }
        $("plan-save").disabled = true; // 같은 요청이 두 번 전송되지 않게 한다.
        const budget = $("plan-budget").value;
        const body = {
            name: $("plan-name").value.trim(),
            budget: budget === "" ? null : Number(budget),
            startDate: $("plan-start-date").value,
            endDate: $("plan-end-date").value,
            notes: $("plan-notes").value.trim(),
            stops: route
        };
        try {
            if (editId) {
                await api("/api/v1/plans/" + encodeURIComponent(editId), { method: "PUT", json: body });
                redirectWithNotice("plans.html", "여행 계획을 수정했습니다.");
            } else {
                await api("/api/v1/plans", { method: "POST", json: body });
                redirectWithNotice("plans.html", "여행 계획을 저장했습니다.");
            }
        } catch (error) {
            showFormError(message, error);
            $("plan-save").disabled = false;
        }
    });
}

// ---- 초기화 (위의 const 함수들이 모두 준비된 뒤 실행해야 한다) ----
const user = await initPage({ requireAuth: true });
if (user) {
    if (editId) await loadPlan();
    setupEvents();
    syncDateRange(false);
    renderRoute();
    initMap();
}
