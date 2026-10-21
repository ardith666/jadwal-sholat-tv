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

    const badgeClass = "badge-" + (s.type.toLowerCase());
    let detailText = "";
    if (s.type === "PRAYER") {
      detailText = "Jadwal 5 Waktu + Imsak/Dhuha, Countdown, dan Rotasi Ayat Al-Qur'an.";
    } else if (s.type === "KAS") {
      const saldo = (s.saldoAwal + s.pemasukan - s.pengeluaran).toLocaleString("id-ID");
      detailText = `Saldo Akhir: Rp ${saldo} | Catatan: ${s.catatanKas || '-'}`;
    } else if (s.type === "ANNOUNCEMENT") {
      detailText = `${s.tanggalAgenda || ''} — ${s.pengumumanTeks || '-'}`;
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
      slide.tanggalAgenda = document.getElementById("agendaWaktu").value.trim();
      slide.narasumber = document.getElementById("agendaNarasumber").value.trim();
      slide.pengumumanTeks = document.getElementById("agendaTeks").value.trim();
    } else if (type === "IMAGE") {
      const fileInput = document.getElementById("posterFile");
      if (fileInput.files.length > 0) {
        const file = fileInput.files[0];
        const fileName = "poster_" + Date.now() + "_" + file.name.replace(/[^a-zA-Z0-9._-]/g, "");
        const base64Data = await toBase64(file);
        // Upload image to TV
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
  document.getElementById("fieldsImage").classList.add("hidden");

  if (type === "KAS") document.getElementById("fieldsKas").classList.remove("hidden");
  else if (type === "ANNOUNCEMENT") document.getElementById("fieldsAnnouncement").classList.remove("hidden");
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

  // Pre-fill fields
  if (slide) {
    document.getElementById("kasSaldoAwal").value = slide.saldoAwal || 0;
    document.getElementById("kasPemasukan").value = slide.pemasukan || 0;
    document.getElementById("kasPengeluaran").value = slide.pengeluaran || 0;
    document.getElementById("kasCatatan").value = slide.catatanKas || "";

    document.getElementById("agendaWaktu").value = slide.tanggalAgenda || "";
    document.getElementById("agendaNarasumber").value = slide.narasumber || "";
    document.getElementById("agendaTeks").value = slide.pengumumanTeks || "";
  } else {
    document.getElementById("kasSaldoAwal").value = 0;
    document.getElementById("kasPemasukan").value = 0;
    document.getElementById("kasPengeluaran").value = 0;
    document.getElementById("kasCatatan").value = "";

    document.getElementById("agendaWaktu").value = "";
    document.getElementById("agendaNarasumber").value = "";
    document.getElementById("agendaTeks").value = "";
  }

  updateDynamicSections(slide ? slide.type : "ANNOUNCEMENT");
  modal.classList.remove("hidden");
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
