// 핫플레이스 사진은 본인만 볼 수 있어 <img src>로 바로 쓸 수 없다(토큰 헤더 필요).
// fetch로 받아 Blob URL을 만들어 쓴다.
import { api } from "./api.js";

/** @param {string} photoUrl HotplaceResponse.photoUrl (예: /api/v1/hotplaces/3/photo) */
export async function loadPhotoUrl(photoUrl) {
    const response = await api(photoUrl, { raw: true });
    return URL.createObjectURL(await response.blob());
}
