let currentPin = sessionStorage.getItem("admin_pin") || "";
let cachedSlides = [];
let cachedSettings = {};

document.addEventListener("DOMContentLoaded", () => {
  initLogin();
  initTabs();
  initSlideEditor();
  initSettingsForm();
  initPinForm();

  if (currentPin) {
    verifyPinAndLoad(currentPin);
  }
});

// ==========================================
// AUTHENTICATION
// ==========================================
function initLogin() {
  const loginForm = document.getElementById("loginForm");
  loginForm.addEventListener("submit", (e) => {
    e.preventDefault();
    const pin = document.getElementById("pinInput").value.trim();
    verifyPinAndLoad(pin);
  });

  document.getElementById("logoutBtn").addEventListener("click", () => {
    sessionStorage.removeItem("admin_pin");
    currentPin = "";
    document.getElementById("appContainer").classList.add("hidden");
    document.getElementById("loginModal").classList.remove("hidden");
    document.getElementById("pinInput").value = "";
  });
}

async function verifyPinAndLoad(pin) {
  try {
    const res = await fetch("/api/auth", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ pin })
    });
    const data = await res.json();
    if (data.status) {
      currentPin = pin;
      sessionStorage.setItem("admin_pin", pin);
      document.getElementById("loginModal").classList.add("hidden");
      document.getElementById("appContainer").classList.remove("hidden");
      loadAllData();
    } else {
      alert("PIN Salah! Default PIN bawaan adalah 1989.");
    }
  } catch (err) {
    alert("Gagal menghubungi server TV: " + err);
  }
}

// ==========================================
// TABS
// ==========================================
function initTabs() {
  document.querySelectorAll(".tab-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
      document.querySelectorAll(".tab-content").forEach(c => c.classList.remove("active"));

      btn.classList.add("active");
      const target = btn.dataset.tab;
      document.getElementById(target).classList.add("active");
    });
  });
}

// ==========================================
// DATA LOADING
// ==========================================
async function loadAllData() {
  loadSlides();
  loadSettings();
}

async function loadSlides() {
  try {
    const res = await fetch("/api/slides?pin=" + encodeURIComponent(currentPin));
    const data = await res.json();
    if (data.status) {
      cachedSlides = data.slides || [];
      renderSlides(cachedSlides);
    }
  } catch (err) {
    console.error("Error loading slides", err);
  }
}

function renderSlides(slides) {
  const container = document.getElementById("slidesList");
  container.innerHTML = "";

  slides.forEach((s, idx) => {
    const card = document.createElement("div");
    card.className = "slide-card" + (s.enabled ? "" : " disabled");

    const badgeClass = "badge-" + (s.type.toLowerCase().replace('_', ''));
    let detailText = "";
    if (s.type === "PRAYER") {
      detailText = "Jadwal 5 Waktu + Imsak/Dhuha, Countdown, dan Rotasi Ayat Al-Qur'an.";
    } else if (s.type === "KAS") {
      const saldo = (s.saldoAwal + s.pemasukan - s.pengeluaran).toLocaleString("id-ID");
      detailText = `Saldo Akhir: Rp ${saldo} | Catatan: ${s.catatanKas || '-'}`;
    } else if (s.type === "ANNOUNCEMENT") {
      const count = (s.agendaList || []).length;
      detailText = count > 0
          ? `${count} Agenda Terdaftar (Hero #1 + Daftar 6 berikutnya).`
          : "Belum ada agenda.";
    } else if (s.type === "LIVE_STREAM") {
      detailText = `RTSP: ${s.liveRtspUrl || '-'} | Lock: ${s.isLocked ? 'Aktif (Kunci Layar)' : 'Non-aktif'}`;
    } else if (s.type === "YOUTUBE") {
      detailText = `YouTube: ${s.youtubeUrl || '-'} | Lock: ${s.isLocked ? 'Aktif (Kunci Layar)' : 'Non-aktif'}`;
    } else if (s.type === "CCTV") {
      const count = (s.cctvStreams || []).length;
      detailText = `${count} Kamera CCTV Terpasang (Grid Responsif 1x1 s/d 2x3).`;
    } else if (s.type === "IMAGE") {
      detailText = `Poster Gambar: ${s.imageFileName || 'Belum ada gambar'}`;
    }

    card.innerHTML = `
      <div class="slide-header">
        <span class="slide-badge ${badgeClass}">${s.type}</span>
        <span class="slide-duration">⏱ ${s.durationSec}s</span>
      </div>
      <div class="slide-title">${idx + 1}. ${s.title}</div>
      <div class="slide-detail">${detailText}</div>
      <div class="slide-actions">
        <label class="switch-label">
          <input type="checkbox" ${s.enabled ? 'checked' : ''} onchange="toggleSlide('${s.id}', this.checked)">
          <span>${s.enabled ? 'Aktif di TV' : 'Non-Aktif'}</span>
        </label>
        <div>
          <button class="btn btn-secondary btn-sm" onclick="editSlide('${s.id}')">Edit</button>
          ${s.id !== 'slide_prayer_core' ? `<button class="btn btn-outline-danger btn-sm" onclick="deleteSlide('${s.id}')">Hapus</button>` : ''}
        </div>
      </div>
    `;
    container.appendChild(card);
  });
}

async function toggleSlide(id, enabled) {
  const slide = cachedSlides.find(s => s.id === id);
  if (slide) {
    slide.enabled = enabled;
    await saveSlidesToServer();
  }
}

async function deleteSlide(id) {
  if (confirm("Hapus slide ini dari layar TV?")) {
    cachedSlides = cachedSlides.filter(s => s.id !== id);
    await saveSlidesToServer();
  }
}

async function saveSlidesToServer() {
  try {
    const res = await fetch("/api/slides/save", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ pin: currentPin, slides: cachedSlides })
    });
    const d = await res.json();
    if (d.status) {
      loadSlides();
    }
  } catch (err) {
    alert("Gagal menyimpan slide: " + err);
  }
}

// ==========================================
// SLIDE EDITOR MODAL
// ==========================================
function initSlideEditor() {
  const modal = document.getElementById("slideEditorModal");
  const slideTypeSelect = document.getElementById("slideType");

  document.getElementById("addNewSlideBtn").addEventListener("click", () => {
    openSlideEditor(null);
  });

  document.getElementById("closeEditorBtn").addEventListener("click", () => {
    modal.classList.add("hidden");
  });

  slideTypeSelect.addEventListener("change", () => {
    updateDynamicSections(slideTypeSelect.value);
  });

  document.getElementById("addAgendaItemBtn").addEventListener("click", () => {
    appendAgendaItemRow();
  });

  document.getElementById("addCctvStreamBtn").addEventListener("click", () => {
    appendCctvStreamRow();
  });

  document.getElementById("slideForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const id = document.getElementById("slideId").value || ("slide_" + Date.now());
    const type = document.getElementById("slideType").value;
    const title = document.getElementById("slideName").value.trim();
    const durationSec = parseInt(document.getElementById("slideDuration").value) || 30;
    const enabled = document.getElementById("slideEnabled").checked;

    let slide = cachedSlides.find(s => s.id === id);
    if (!slide) {
      slide = { id, type, title, durationSec, enabled };
      cachedSlides.push(slide);
    } else {
      slide.type = type;
      slide.title = title;
      slide.durationSec = durationSec;
      slide.enabled = enabled;
    }

    if (type === "KAS") {
      slide.saldoAwal = parseInt(document.getElementById("kasSaldoAwal").value) || 0;
      slide.pemasukan = parseInt(document.getElementById("kasPemasukan").value) || 0;
      slide.pengeluaran = parseInt(document.getElementById("kasPengeluaran").value) || 0;
      slide.catatanKas = document.getElementById("kasCatatan").value.trim();
    } else if (type === "ANNOUNCEMENT") {
      slide.agendaList = collectAgendaItems();
    } else if (type === "LIVE_STREAM") {
      slide.liveRtspUrl = document.getElementById("liveRtspUrl").value.trim();
      slide.isLocked = document.getElementById("chkLockLive").checked;
      slide.audioEnabled = document.getElementById("chkAudioLive").checked;
    } else if (type === "YOUTUBE") {
      slide.youtubeUrl = document.getElementById("youtubeUrl").value.trim();
      slide.isLocked = document.getElementById("chkLockYoutube").checked;
    } else if (type === "CCTV") {
      slide.cctvStreams = collectCctvStreams();
    } else if (type === "IMAGE") {
      const fileInput = document.getElementById("posterFile");
      if (fileInput.files.length > 0) {
        const file = fileInput.files[0];
        const fileName = "poster_" + Date.now() + "_" + file.name.replace(/[^a-zA-Z0-9._-]/g, "");
        const base64Data = await toBase64(file);
        await fetch("/api/upload-image", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ pin: currentPin, fileName, base64Data })
        });
        slide.imageFileName = fileName;
      }
    }

    await saveSlidesToServer();
    modal.classList.add("hidden");
  });
}

function updateDynamicSections(type) {
  document.getElementById("fieldsKas").classList.add("hidden");
  document.getElementById("fieldsAnnouncement").classList.add("hidden");
  document.getElementById("fieldsLiveStream").classList.add("hidden");
  document.getElementById("fieldsCctv").classList.add("hidden");
  document.getElementById("fieldsImage").classList.add("hidden");

  if (type === "KAS") document.getElementById("fieldsKas").classList.remove("hidden");
  else if (type === "ANNOUNCEMENT") document.getElementById("fieldsAnnouncement").classList.remove("hidden");
  else if (type === "LIVE_STREAM") document.getElementById("fieldsLiveStream").classList.remove("hidden");
  else if (type === "YOUTUBE") document.getElementById("fieldsYoutube").classList.remove("hidden");
  else if (type === "CCTV") document.getElementById("fieldsCctv").classList.remove("hidden");
  else if (type === "IMAGE") document.getElementById("fieldsImage").classList.remove("hidden");
}

function editSlide(id) {
  const slide = cachedSlides.find(s => s.id === id);
  if (slide) openSlideEditor(slide);
}

function openSlideEditor(slide) {
  const modal = document.getElementById("slideEditorModal");
  document.getElementById("editorTitle").innerText = slide ? "Edit Slide: " + slide.title : "Tambah Slide Baru";

  document.getElementById("slideId").value = slide ? slide.id : "";
  document.getElementById("slideType").value = slide ? slide.type : "ANNOUNCEMENT";
  document.getElementById("slideName").value = slide ? slide.title : "";
  document.getElementById("slideDuration").value = slide ? slide.durationSec : 30;
  document.getElementById("slideEnabled").checked = slide ? slide.enabled : true;

  if (slide && slide.id === "slide_prayer_core") {
    document.getElementById("slideType").disabled = true;
  } else {
    document.getElementById("slideType").disabled = false;
  }

  // Pre-fill Kas
  if (slide && slide.type === "KAS") {
    document.getElementById("kasSaldoAwal").value = slide.saldoAwal || 0;
    document.getElementById("kasPemasukan").value = slide.pemasukan || 0;
    document.getElementById("kasPengeluaran").value = slide.pengeluaran || 0;
    document.getElementById("kasCatatan").value = slide.catatanKas || "";
  } else {
    document.getElementById("kasSaldoAwal").value = 0;
    document.getElementById("kasPemasukan").value = 0;
    document.getElementById("kasPengeluaran").value = 0;
    document.getElementById("kasCatatan").value = "";
  }

  // Pre-fill Agenda
  const agendaContainer = document.getElementById("agendaItemsContainer");
  agendaContainer.innerHTML = "";
  if (slide && slide.agendaList && slide.agendaList.length > 0) {
    slide.agendaList.forEach(a => appendAgendaItemRow(a.title, a.waktu, a.narasumber, a.desc));
  } else {
    appendAgendaItemRow("Kajian Utama", "Ahad, 06.00 WIB", "Ust. Fulan", "Pembahasan Kitab Riyadhus Shalihin.");
  }

  // Pre-fill Live Stream
  document.getElementById("liveRtspUrl").value = (slide && slide.liveRtspUrl) ? slide.liveRtspUrl : "rtsp://admin:LNUMXL@192.168.1.114:554/ch1/main";
  document.getElementById("chkLockLive").checked = slide ? (slide.isLocked !== false) : true;
  document.getElementById("chkAudioLive").checked = slide ? (slide.audioEnabled === true) : false;

  // Pre-fill YouTube
  document.getElementById("youtubeUrl").value = (slide && slide.youtubeUrl) ? slide.youtubeUrl : "https://www.youtube.com/watch?v=live_stream";
  document.getElementById("chkLockYoutube").checked = slide ? (slide.isLocked !== false) : true;

  // Pre-fill CCTV
  const cctvContainer = document.getElementById("cctvStreamsContainer");
  cctvContainer.innerHTML = "";
  if (slide && slide.cctvStreams && slide.cctvStreams.length > 0) {
    slide.cctvStreams.forEach(c => appendCctvStreamRow(c.name, c.rtspUrl));
  } else {
    appendCctvStreamRow("Kamera 1 / Shaf Depan", "rtsp://admin:LNUMXL@192.168.1.114:554/ch1/main");
    appendCctvStreamRow("Kamera 2 / Halaman Parkir", "rtsp://admin:LNUMXL@192.168.1.114:554/ch1/main");
  }

  updateDynamicSections(slide ? slide.type : "ANNOUNCEMENT");
  modal.classList.remove("hidden");
}

function appendAgendaItemRow(title = "", waktu = "", narasumber = "", desc = "") {
  const container = document.getElementById("agendaItemsContainer");
  const row = document.createElement("div");
  row.className = "dynamic-item-box";
  row.style.background = "rgba(255,255,255,0.05)";
  row.style.padding = "10px";
  row.style.borderRadius = "6px";
  row.style.border = "1px solid var(--border-color)";

  row.innerHTML = `
    <div style="display: flex; gap: 8px; margin-bottom: 6px;">
      <input type="text" class="form-control agenda-title" placeholder="Nama Agenda / Judul Kajian" value="${escapeHtml(title)}" style="flex: 2;" required>
      <input type="text" class="form-control agenda-waktu" placeholder="Waktu (misal: Ahad, 06.00 WIB)" value="${escapeHtml(waktu)}" style="flex: 1;" required>
      <button type="button" class="btn btn-outline-danger btn-sm" onclick="this.closest('.dynamic-item-box').remove()">✕</button>
    </div>
    <div style="display: flex; gap: 8px;">
      <input type="text" class="form-control agenda-narasumber" placeholder="Penceramah / Penyelenggara (misal: Ust. Fulan)" value="${escapeHtml(narasumber)}" style="flex: 1;">
      <input type="text" class="form-control agenda-desc" placeholder="Keterangan singkat" value="${escapeHtml(desc)}" style="flex: 2;">
    </div>
  `;
  container.appendChild(row);
}

function collectAgendaItems() {
  const res = [];
  document.querySelectorAll("#agendaItemsContainer .dynamic-item-box").forEach(row => {
    const title = row.querySelector(".agenda-title").value.trim();
    const waktu = row.querySelector(".agenda-waktu").value.trim();
    const narasumber = row.querySelector(".agenda-narasumber").value.trim();
    const desc = row.querySelector(".agenda-desc").value.trim();
    if (title || waktu) {
      res.push({ title, waktu, narasumber, desc });
    }
  });
  return res;
}

function appendCctvStreamRow(name = "", url = "") {
  const container = document.getElementById("cctvStreamsContainer");
  const row = document.createElement("div");
  row.className = "dynamic-item-box";
  row.style.display = "flex";
  row.style.gap = "8px";
  row.style.alignItems = "center";
  row.style.background = "rgba(255,255,255,0.05)";
  row.style.padding = "8px";
  row.style.borderRadius = "6px";
  row.style.border = "1px solid var(--border-color)";

  row.innerHTML = `
    <input type="text" class="form-control cctv-name" placeholder="Nama Lokasi Kamera" value="${escapeHtml(name)}" style="flex: 1;" required>
    <input type="text" class="form-control cctv-url" placeholder="rtsp://admin:pass@ip:554/ch1/main" value="${escapeHtml(url)}" style="flex: 2;" required>
    <button type="button" class="btn btn-outline-danger btn-sm" onclick="this.closest('.dynamic-item-box').remove()">✕</button>
  `;
  container.appendChild(row);
}

function collectCctvStreams() {
  const res = [];
  document.querySelectorAll("#cctvStreamsContainer .dynamic-item-box").forEach(row => {
    const name = row.querySelector(".cctv-name").value.trim();
    const rtspUrl = row.querySelector(".cctv-url").value.trim();
    if (rtspUrl) {
      res.push({ name: name || "Kamera", rtspUrl });
    }
  });
  return res;
}

function escapeHtml(str) {
  if (!str) return "";
  return str.replace(/&/g, "&amp;").replace(/"/g, "&quot;").replace(/'/g, "&#039;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

function toBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result.split(",")[1]);
    reader.onerror = error => reject(error);
  });
}

// ==========================================
// SETTINGS FORM
// ==========================================
async function loadSettings() {
  try {
    const res = await fetch("/api/settings?pin=" + encodeURIComponent(currentPin));
    const d = await res.json();
    if (d.status) {
      cachedSettings = d.settings || {};
      populateSettings(cachedSettings);
    }
  } catch (err) {
    console.error("Error loading settings", err);
  }
}

function populateSettings(s) {
  const citySelect = document.getElementById("citySelect");
  citySelect.innerHTML = "";
  if (s.cities) {
    s.cities.forEach(c => {
      const opt = document.createElement("option");
      opt.value = c.id;
      opt.textContent = c.name;
      if (c.id === s.currentCityId) opt.selected = true;
      citySelect.appendChild(opt);
    });
  }

  const themeSelect = document.getElementById("themeSelect");
  themeSelect.innerHTML = "";
  if (s.themes) {
    s.themes.forEach(t => {
      const opt = document.createElement("option");
      opt.value = t[0];
      opt.textContent = t[1];
      if (t[0] === s.currentTheme) opt.selected = true;
      themeSelect.appendChild(opt);
    });
  }

  document.getElementById("soundSelect").value = s.soundMode || "adzan";
  document.getElementById("chkCountdown").checked = s.countdownEnabled !== false;
  document.getElementById("chkCountdownAudio").checked = s.countdownAudioEnabled !== false;
  document.getElementById("chkKeepAwake").checked = s.keepAwake !== false;

  document.getElementById("chkShowArab").checked = s.showArab !== false;
  document.getElementById("chkShowLatin").checked = s.showLatin !== false;
  document.getElementById("chkShowTrans").checked = s.showTranslation !== false;
}

function initSettingsForm() {
  document.getElementById("settingsForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const citySelect = document.getElementById("citySelect");
    const payload = {
      pin: currentPin,
      cityId: citySelect.value,
      cityName: citySelect.options[citySelect.selectedIndex].text,
      theme: document.getElementById("themeSelect").value,
      soundMode: document.getElementById("soundSelect").value,
      countdownEnabled: document.getElementById("chkCountdown").checked,
      countdownAudioEnabled: document.getElementById("chkCountdownAudio").checked,
      keepAwake: document.getElementById("chkKeepAwake").checked,
      showArab: document.getElementById("chkShowArab").checked,
      showLatin: document.getElementById("chkShowLatin").checked,
      showTranslation: document.getElementById("chkShowTrans").checked
    };

    try {
      const res = await fetch("/api/settings/save", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      const d = await res.json();
      if (d.status) {
        alert("Pengaturan TV berhasil disimpan!");
        loadSettings();
      }
    } catch (err) {
      alert("Gagal menyimpan pengaturan: " + err);
    }
  });
}

// ==========================================
// PIN SECURITY
// ==========================================
function initPinForm() {
  document.getElementById("changePinForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const newPin = document.getElementById("newPin").value.trim();
    const confirmPin = document.getElementById("confirmPin").value.trim();

    if (newPin !== confirmPin) {
      alert("Konfirmasi PIN tidak cocok!");
      return;
    }

    try {
      const res = await fetch("/api/pin/save", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ currentPin, newPin })
      });
      const d = await res.json();
      if (d.status) {
        alert("PIN Administrator berhasil diubah!");
        currentPin = newPin;
        sessionStorage.setItem("admin_pin", newPin);
        document.getElementById("newPin").value = "";
        document.getElementById("confirmPin").value = "";
      } else {
        alert("Gagal mengubah PIN: " + d.message);
      }
    } catch (err) {
      alert("Error: " + err);
    }
  });
}
