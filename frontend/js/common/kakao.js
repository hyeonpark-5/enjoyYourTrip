// Kakao 지도 SDK를 필요할 때 한 번만 불러온다. 키가 없거나 불러오지 못하면 지도 자리에 안내 문구를 보여준다.
import { h } from "./ui.js";

const KEY = (window.APP_CONFIG || {}).KAKAO_APP_KEY || "";
let loading = null;

/** @returns Promise<kakao.maps> — 실패하면 Error */
export function loadKakaoMaps() {
    if (!KEY) {
        return Promise.reject(new Error(
            "js/config.js의 KAKAO_APP_KEY에 Kakao JavaScript 키를 입력하면 지도가 표시됩니다."));
    }
    if (!loading) {
        loading = new Promise((resolve, reject) => {
            const script = document.createElement("script");
            script.src = "https://dapi.kakao.com/v2/maps/sdk.js?autoload=false&appkey=" + encodeURIComponent(KEY);
            script.onload = () => window.kakao.maps.load(() => resolve(window.kakao.maps));
            script.onerror = () => reject(new Error(
                "Kakao 지도 SDK를 불러오지 못했습니다. JavaScript 키와 Web 플랫폼 도메인(" + location.origin + ") 등록을 확인하세요."));
            document.head.append(script);
        });
    }
    return loading;
}

/** 지도 영역을 안내 문구로 바꾼다. */
export function showMapSetup(container, message) {
    container.className = "map-setup";
    container.replaceChildren(h("div", { class: "setup-card" },
        h("span", { class: "setup-icon", "aria-hidden": "true", text: "⌖" }),
        h("strong", { text: "Kakao Map 연결 준비가 필요해요" }),
        h("p", { text: message })));
}

/** 관광지 테마별 이름·아이콘·색 (contentTypeId 기준) */
export const PLACE_TYPES = {
    "12": ["관광지", "📍", "#178f70"], "14": ["문화시설", "🏛️", "#7259b5"], "15": ["축제·공연·행사", "🎉", "#e98335"],
    "25": ["여행코스", "🗺️", "#377fca"], "28": ["레포츠", "🚲", "#138e96"], "32": ["숙박", "🛏️", "#435f91"],
    "38": ["쇼핑", "🛍️", "#d65b8c"], "39": ["음식점", "🍽️", "#d9574f"]
};
export const DEFAULT_PLACE_TYPE = ["기타", "✨", "#607d78"];
export const placeTypeOf = (contentTypeId) => PLACE_TYPES[contentTypeId] || DEFAULT_PLACE_TYPE;
