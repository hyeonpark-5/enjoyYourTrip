// 핫플레이스 등록(hotplace-form.html?placeId=126508) / 수정(hotplace-form.html?id=3)
// 사진은 브라우저에서 960x720 이하 JPEG로 줄여 업로드한다. 서버도 다시 검증·변환하므로 이 처리는 업로드 용량을 줄이고 WebP를 지원하기 위한 것이다.
import { api } from "../common/api.js";
import { initPage } from "../common/layout.js";
import { loadPhotoUrl } from "../common/photo.js";
import { $, h, params, redirectWithNotice, showFormError, today } from "../common/ui.js";

const MAX_BYTES = 8 * 1024 * 1024;
const MAX_WIDTH = 960;
const MAX_HEIGHT = 720;
const NO_FILE_TEXT = "선택된 파일 없음";

const query = params();
const editId = query.get("id");
const placeId = query.get("placeId");

let compressedPhoto = null; // 업로드할 사진(Blob). null이면 사진을 보내지 않는다.
let initialImage = ""; // 사진을 새로 고르지 않았을 때 보여줄 사진 (수정: 올린 사진, 등록: 관광지 기본 사진)
let placeImage = "";

const user = await initPage({ requireAuth: true });
if (user) {
    try {
        await load();
        setupForm();
        $("hotplace-form").hidden = false;
    } catch (error) {
        showFormError($("load-error"), error);
    }
}

async function load() {
    const types = await api("/api/v1/hotplaces/place-types");
    $("hotplace-type").replaceChildren(...types.map((type) => h("option", { value: type.code, text: type.label })));
    $("hotplace-visited-date").max = today();

    if (editId) {
        const spot = await api("/api/v1/hotplaces/" + encodeURIComponent(editId));
        document.title = "핫플레이스 수정 | EnjoyTrip";
        $("form-eyebrow").textContent = "EDIT HOT PLACE";
        $("form-title").textContent = "핫플레이스 내용 수정";
        $("hotplace-save").textContent = "수정사항 저장";
        $("cancel").href = "hotplaces.html";
        $("place-name").textContent = spot.placeName;
        $("place-address").textContent = spot.address;
        $("hotplace-visited-date").value = spot.visitedDate;
        $("hotplace-type").value = spot.placeType;
        $("hotplace-description").value = spot.description;
        placeImage = spot.placeImage;
        initialImage = spot.photoUrl ? await loadPhotoUrl(spot.photoUrl) : spot.placeImage;
        if (spot.photoUrl) {
            $("remove-photo-label").hidden = false;
            $("photo-help").textContent = "선택하지 않으면 기존 사진을 유지합니다. JPG, PNG, WebP · 최대 8MB";
        }
    } else {
        if (!placeId) throw new Error("관광지 찾기에서 관광지를 고른 뒤 등록해 주세요.");
        const place = await api("/api/v1/tours/places/" + encodeURIComponent(placeId));
        $("place-name").textContent = place.name;
        $("place-address").textContent = place.address;
        // 관광지 종류에 맞는 유형을 미리 고른다. (서버도 비어 있으면 같은 규칙으로 정한다)
        const matched = types.find((type) => type.contentTypeId === place.contentTypeId);
        $("hotplace-type").value = matched ? matched.code : "ETC";
        $("hotplace-visited-date").value = today();
        placeImage = place.image;
        initialImage = place.image;
    }
}

function setupForm() {
    const photoInput = $("hotplace-photo");
    const preview = $("hotplace-photo-preview");
    const message = $("hotplace-form-message");
    const removeCheckbox = $("remove-photo");
    const saveButton = $("hotplace-save");

    const setPreview = (url) => {
        preview.style.backgroundImage = url ? 'url("' + url.replaceAll('"', "%22") + '")' : "";
        preview.classList.toggle("has-image", Boolean(url));
        [...preview.children].forEach((child) => { child.hidden = Boolean(url); }); // 사진이 없을 때만 안내 표시
    };
    const resetPhoto = () => {
        compressedPhoto = null;
        photoInput.value = "";
        $("hotplace-photo-name").textContent = NO_FILE_TEXT;
    };

    let selectSeq = 0;
    photoInput.addEventListener("change", async () => {
        const file = photoInput.files && photoInput.files[0];
        const seq = ++selectSeq;
        if (!file) { // 파일 선택을 취소한 경우
            resetPhoto();
            setPreview(removeCheckbox.checked ? placeImage : initialImage);
            return;
        }
        message.textContent = "사진을 준비하는 중입니다.";
        saveButton.disabled = true;
        try {
            const blob = await compress(file);
            if (seq !== selectSeq) return; // 그 사이 다른 파일을 고른 경우
            compressedPhoto = blob;
            $("hotplace-photo-name").textContent = file.name;
            setPreview(URL.createObjectURL(blob));
            removeCheckbox.checked = false;
            message.textContent = "";
        } catch (error) {
            if (seq !== selectSeq) return;
            resetPhoto();
            setPreview(removeCheckbox.checked ? placeImage : initialImage);
            message.textContent = error.message;
        } finally {
            if (seq === selectSeq) saveButton.disabled = false;
        }
    });

    removeCheckbox.addEventListener("change", () => {
        if (removeCheckbox.checked) {
            selectSeq++;
            resetPhoto();
            setPreview(placeImage);
        } else if (!compressedPhoto) {
            setPreview(initialImage);
        }
    });

    const description = $("hotplace-description");
    const updateCount = () => { $("hotplace-description-count").textContent = String(description.value.length); };
    description.addEventListener("input", updateCount);
    updateCount();

    $("hotplace-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        saveButton.disabled = true; // 같은 요청이 두 번 전송되지 않게 한다.
        const form = new FormData();
        form.append("placeType", $("hotplace-type").value);
        form.append("visitedDate", $("hotplace-visited-date").value);
        form.append("description", description.value.trim());
        if (compressedPhoto) form.append("photo", new File([compressedPhoto], "photo.jpg", { type: "image/jpeg" }));
        try {
            if (editId) {
                form.append("removePhoto", String(removeCheckbox.checked));
                await api("/api/v1/hotplaces/" + encodeURIComponent(editId), { method: "PATCH", form });
                redirectWithNotice("hotplaces.html", "핫플레이스를 수정했습니다.");
            } else {
                form.append("placeId", placeId);
                await api("/api/v1/hotplaces", { method: "POST", form });
                redirectWithNotice("hotplaces.html", "핫플레이스를 등록했습니다.");
            }
        } catch (error) {
            showFormError(message, error);
            saveButton.disabled = false;
        }
    });

    setPreview(initialImage);
}

function compress(file) {
    return new Promise((resolve, reject) => {
        if (!file.type.startsWith("image/")) {
            reject(new Error("이미지 파일만 등록할 수 있습니다."));
            return;
        }
        if (file.size > MAX_BYTES) {
            reject(new Error("사진은 8MB 이하 파일을 선택해 주세요."));
            return;
        }
        const image = new Image();
        const url = URL.createObjectURL(file);
        image.addEventListener("error", () => reject(new Error("지원하지 않는 이미지입니다.")));
        image.addEventListener("load", () => {
            URL.revokeObjectURL(url);
            const scale = Math.min(1, MAX_WIDTH / image.width, MAX_HEIGHT / image.height);
            const canvas = document.createElement("canvas");
            canvas.width = Math.max(1, Math.round(image.width * scale));
            canvas.height = Math.max(1, Math.round(image.height * scale));
            const context = canvas.getContext("2d");
            context.fillStyle = "#fff"; // 투명 배경은 흰색으로 채워 JPEG로 변환한다.
            context.fillRect(0, 0, canvas.width, canvas.height);
            context.drawImage(image, 0, 0, canvas.width, canvas.height);
            canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error("사진을 변환하지 못했습니다."))), "image/jpeg", 0.78);
        });
        image.src = url;
    });
}
