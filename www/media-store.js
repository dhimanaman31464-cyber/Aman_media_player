import { Capacitor } from "@capacitor/core";
import { CapacitorMediaStore } from "@odion-cloud/capacitor-mediastore";

const statusEl = document.getElementById("recent");
const refreshButton = document.getElementById("refreshMedia");

async function scanDeviceMedia() {
  if (!Capacitor.isNativePlatform()) {
    statusEl.textContent = "फोन की पूरी मीडिया लाइब्रेरी देखने के लिए Android ऐप में खोलें।";
    return;
  }

  statusEl.textContent = "फोन की फोटो, वीडियो और MP3 फाइलें स्कैन हो रही हैं…";
  if (refreshButton) refreshButton.disabled = true;

  try {
    await CapacitorMediaStore.requestPermissions({
      types: ["audio", "image", "video"]
    });

    const [audioResult, videoResult, imageResult] = await Promise.all([
      CapacitorMediaStore.getMediasByType({ mediaType: "audio", includeExternal: true }),
      CapacitorMediaStore.getMediasByType({ mediaType: "video", includeExternal: true }),
      CapacitorMediaStore.getMediasByType({ mediaType: "image", includeExternal: true })
    ]);

    const toItems = (items) => (items || []).map((item) => {
      const rawUri = item.uri || item.path || "";
      const playableUri = rawUri.startsWith("content://")
        ? rawUri
        : Capacitor.convertFileSrc(rawUri);
      return {
        name: item.title || item.displayName || item.name || "Untitled media",
        url: playableUri
      };
    });

    media.audio.splice(0, media.audio.length, ...toItems(audioResult.media));
    media.video.splice(0, media.video.length, ...toItems(videoResult.media));
    media.image.splice(0, media.image.length, ...toItems(imageResult.media));
    render();

    const count = media.audio.length + media.video.length + media.image.length;
    statusEl.textContent = count
      ? `आपकी लाइब्रेरी में ${media.audio.length} गाने, ${media.video.length} वीडियो और ${media.image.length} फोटो मिले।`
      : "कोई मीडिया फाइल नहीं मिली। अनुमति जाँचें या Scan again दबाएँ।";
  } catch (error) {
    console.error("Media scan failed:", error);
    statusEl.textContent = "मीडिया स्कैन नहीं हो पाया। Android Settings में मीडिया अनुमति दें और फिर Scan again दबाएँ।";
  } finally {
    if (refreshButton) refreshButton.disabled = false;
  }
}

refreshButton?.addEventListener("click", scanDeviceMedia);
window.addEventListener("focus", () => {
  if (Capacitor.isNativePlatform()) scanDeviceMedia();
});
scanDeviceMedia();
