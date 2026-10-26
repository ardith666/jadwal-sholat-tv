<?php
if (session_status() === PHP_SESSION_NONE) {
    session_start();
}
if (!isset($_SESSION['logged_in']) || $_SESSION['logged_in'] !== true) {
    header('Location: index.php');
    exit;
}
$username = $_SESSION['user'] ?? 'Admin';
?>
<!DOCTYPE html>
<html lang="id" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>MasjidTV • Dashboard Monitoring Realtime</title>

    <!-- PWA Settings -->
    <link rel="manifest" href="manifest.json">
    <meta name="theme-color" content="#064e3b">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <meta name="apple-mobile-web-app-title" content="MasjidTV">
    <link rel="apple-touch-icon" href="icons/apple-touch-icon.png">
    <link rel="icon" type="image/svg+xml" href="icons/icon.svg">
    <link rel="icon" type="image/png" href="icons/icon-192.png">

    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=Amiri:wght@400;700&display=swap" rel="stylesheet">

    <!-- Tailwind CSS -->
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            darkMode: 'class',
            theme: {
                extend: {
                    fontFamily: {
                        sans: ['Inter', 'sans-serif'],
                        arabic: ['Amiri', 'serif'],
                    },
                    colors: {
                        darkbg: '#090d16',
                        darkcard: '#0f172a',
                        darkborder: '#1e293b',
                        brand: {
                            50: '#ecfdf5',
                            100: '#d1fae5',
                            400: '#34d399',
                            500: '#10b981',
                            600: '#059669',
                            700: '#047857',
                            800: '#065f46',
                            900: '#064e3b',
                            950: '#022c22'
                        }
                    }
                }
            }
        }
    </script>

    <!-- Leaflet.js (Map) -->
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=" crossorigin="" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>

    <!-- Chart.js -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>

    <style>
        /* Custom scrollbar for timeline and tables */
        ::-webkit-scrollbar { width: 6px; height: 6px; }
        ::-webkit-scrollbar-track { background: transparent; }
        ::-webkit-scrollbar-thumb { background: rgba(100, 116, 139, 0.3); border-radius: 9999px; }
        ::-webkit-scrollbar-thumb:hover { background: rgba(100, 116, 139, 0.5); }

        /* Pulse animations for status dots */
        @keyframes pulse-dot {
            0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
            70% { transform: scale(1); box-shadow: 0 0 0 8px rgba(16, 185, 129, 0); }
            100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
        }
        .pulse-live {
            animation: pulse-dot 2s infinite;
        }

        /* Leaflet custom popup styling */
        .leaflet-popup-content-wrapper {
            background: #0f172a !important;
            color: #f1f5f9 !important;
            border-radius: 1rem !important;
            border: 1px solid rgba(16, 185, 129, 0.3) !important;
            box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5), 0 8px 10px -6px rgba(0, 0, 0, 0.5) !important;
            padding: 2px !important;
        }
        .light .leaflet-popup-content-wrapper {
            background: #ffffff !important;
            color: #0f172a !important;
            border: 1px solid rgba(16, 185, 129, 0.3) !important;
        }
        .leaflet-popup-tip {
            background: #0f172a !important;
        }
        .light .leaflet-popup-tip {
            background: #ffffff !important;
        }
        .leaflet-popup-content {
            margin: 10px 14px !important;
            line-height: 1.4 !important;
        }
        .leaflet-container {
            font-family: 'Inter', sans-serif !important;
            z-index: 10 !important;
        }
    </style>
</head>
<body class="bg-slate-50 text-slate-900 dark:bg-darkbg dark:text-slate-100 min-h-screen flex flex-col font-sans transition-colors duration-200">

    <!-- Top Sticky Navigation -->
    <header class="sticky top-0 z-30 backdrop-blur-md bg-white/80 dark:bg-darkcard/80 border-b border-slate-200 dark:border-darkborder transition-colors">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between gap-4">
            
            <!-- Brand / Logo -->
            <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-900 via-brand-700 to-emerald-500 p-0.5 flex-shrink-0 shadow-md">
                    <div class="w-full h-full bg-slate-950 rounded-[10px] flex items-center justify-center text-xl">
                        🕌
                    </div>
                </div>
                <div>
                    <div class="flex items-center gap-2">
                        <span class="font-extrabold text-base sm:text-lg tracking-tight text-slate-900 dark:text-white">Masjid<span class="text-emerald-500">TV</span></span>
                        <span class="hidden sm:inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
                            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 pulse-live"></span>
                            Live MySQL
                        </span>
                    </div>
                    <p class="text-[11px] text-slate-500 dark:text-slate-400 hidden xs:block">Monitoring Jadwal Sholat TV</p>
                </div>
            </div>

            <!-- Middle: Live Clock & Date (Hidden on very small mobile) -->
            <div class="hidden md:flex flex-col items-center justify-center text-center">
                <div class="font-mono text-sm font-bold text-slate-800 dark:text-slate-200 tracking-wider" id="digitalClock">--:--:-- WIB</div>
                <div class="text-[11px] text-slate-500 dark:text-slate-400" id="currentDate">Memuat tanggal...</div>
            </div>

            <!-- Right Controls: Refresh, Theme, Install PWA, User/Logout -->
            <div class="flex items-center gap-2 sm:gap-3">
                
                <!-- PWA Install Button (Shown if available) -->
                <button 
                    id="pwaInstallHeaderBtn" 
                    class="hidden items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30 text-xs font-semibold transition"
                    title="Install PWA di Perangkat"
                >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                    <span class="hidden sm:inline">Install App</span>
                </button>

                <!-- Refresh Button -->
                <button 
                    id="refreshBtn" 
                    onclick="loadAllData(true)" 
                    class="p-2 rounded-xl border border-slate-200 dark:border-darkborder bg-white dark:bg-slate-800/80 hover:bg-slate-100 dark:hover:bg-slate-700/60 text-slate-700 dark:text-slate-200 text-xs font-medium transition flex items-center gap-1.5 shadow-sm"
                    title="Perbarui Data Sekarang"
                >
                    <svg id="refreshSpinIcon" class="w-4 h-4 text-emerald-500 transition-transform duration-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
                    <span class="hidden sm:inline">Sync</span>
                </button>

                <!-- Theme Toggle -->
                <button 
                    id="themeToggleBtn" 
                    class="p-2 rounded-xl border border-slate-200 dark:border-darkborder bg-white dark:bg-slate-800/80 hover:bg-slate-100 dark:hover:bg-slate-700/60 text-slate-700 dark:text-slate-200 transition shadow-sm"
                    title="Ganti Tema Terang/Gelap"
                >
                    <!-- Sun Icon (shown in dark mode) -->
                    <svg id="sunIcon" class="w-4 h-4 hidden" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>
                    <!-- Moon Icon (shown in light mode) -->
                    <svg id="moonIcon" class="w-4 h-4 hidden" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"></path></svg>
                </button>

                <!-- Logout -->
                <a 
                    href="logout.php" 
                    onclick="return confirm('Apakah Anda yakin ingin keluar dari dashboard?')"
                    class="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-rose-200 dark:border-rose-900/60 bg-rose-50/50 dark:bg-rose-950/30 text-rose-600 dark:text-rose-400 hover:bg-rose-100 dark:hover:bg-rose-900/40 text-xs font-semibold transition"
                    title="Keluar dari sesi"
                >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path></svg>
                    <span class="hidden sm:inline">Keluar</span>
                </a>

            </div>

        </div>
    </header>

    <!-- Main Container -->
    <main class="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 py-6 space-y-6">

        <!-- Top Alert / Status Banner -->
        <div id="connStatusBanner" class="hidden p-3 rounded-2xl bg-amber-500/10 border border-amber-500/30 text-amber-500 text-xs flex items-center justify-between">
            <div class="flex items-center gap-2">
                <span>⚠️</span>
                <span id="connStatusText">Sedang memperbarui koneksi ke webservice...</span>
            </div>
            <button onclick="loadAllData(true)" class="underline font-bold">Coba Lagi</button>
        </div>

        <!-- SECTION 1: KEY METRICS CARDS (5 Cards) -->
        <section class="grid grid-cols-2 lg:grid-cols-5 gap-3 sm:gap-4">
            
            <!-- Card 1: Total Devices -->
            <div class="bg-white dark:bg-darkcard border border-slate-200 dark:border-darkborder rounded-2xl p-4 sm:p-5 shadow-sm hover:border-emerald-500/40 transition-all flex flex-col justify-between">
                <div class="flex items-center justify-between text-slate-500 dark:text-slate-400">
                    <span class="text-xs font-medium uppercase tracking-wider">Perangkat</span>
                    <div class="w-8 h-8 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-700 dark:text-slate-300">
                        📺
                    </div>
                </div>
                <div class="mt-2">
                    <p class="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white" id="stat-total">0</p>
                    <p class="text-[11px] text-slate-500 dark:text-slate-400 mt-1">Total TV terdaftar</p>
                </div>
            </div>

            <!-- Card 2: Active 24h -->
            <div class="bg-white dark:bg-darkcard border border-slate-200 dark:border-darkborder rounded-2xl p-4 sm:p-5 shadow-sm hover:border-emerald-500/40 transition-all flex flex-col justify-between">
                <div class="flex items-center justify-between text-slate-500 dark:text-slate-400">
                    <span class="text-xs font-medium uppercase tracking-wider">Aktif 24 Jam</span>
                    <div class="w-8 h-8 rounded-xl bg-emerald-500/10 flex items-center justify-center text-emerald-500">
                        <span class="w-2.5 h-2.5 rounded-full bg-emerald-500 pulse-live"></span>
                    </div>
                </div>
                <div class="mt-2">
                    <p class="text-2xl sm:text-3xl font-extrabold tracking-tight text-emerald-500" id="stat-active">0</p>
                    <p class="text-[11px] text-emerald-600 dark:text-emerald-400/90 mt-1 font-medium" id="stat-active-pct">0% online</p>
                </div>
            </div>

            <!-- Card 3: Total Kas Balance -->
            <div class="col-span-2 sm:col-span-1 bg-white dark:bg-darkcard border border-slate-200 dark:border-darkborder rounded-2xl p-4 sm:p-5 shadow-sm hover:border-emerald-500/40 transition-all flex flex-col justify-between">
                <div class="flex items-center justify-between text-slate-500 dark:text-slate-400">
                    <span class="text-xs font-medium uppercase tracking-wider">Total Saldo Kas</span>
                    <div class="w-8 h-8 rounded-xl bg-amber-500/10 flex items-center justify-center text-amber-500 text-base">
                        💰
                    </div>
                </div>
                <div class="mt-2">
                    <p class="text-xl sm:text-2xl font-extrabold tracking-tight text-slate-900 dark:text-white truncate" id="stat-cash" title="Rp 0">Rp 0</p>
                    <p class="text-[11px] text-slate-500 dark:text-slate-400 mt-1">Akumulasi kas masjid</p>
                </div>
            </div>

            <!-- Card 4: Distinct Provinces -->
            <div class="bg-white dark:bg-darkcard border border-slate-200 dark:border-darkborder rounded-2xl p-4 sm:p-5 shadow-sm hover:border-emerald-500/40 transition-all flex flex-col justify-between">
                <div class="flex items-center justify-between text-slate-500 dark:text-slate-400">
                    <span class="text-xs font-medium uppercase tracking-wider">Provinsi</span>
                    <div class="w-8 h-8 rounded-xl bg-blue-500/10 flex items-center justify-center text-blue-500 text-base">
                        🗺️
                    </div>
                </div>
                <div class="mt-2">
                    <p class="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white" id="stat-prov">0</p>
                    <p class="text-[11px] text-slate-500 dark:text-slate-400 mt-1">Cakupan wilayah</p>
                </div>
            </div>

            <!-- Card 5: History Log Count -->
            <div class="bg-white dark:bg-darkcard border border-slate-200 dark:border-darkborder rounded-2xl p-4 sm:p-5 shadow-sm hover:border-emerald-500/40 transition-all flex flex-col justify-between">
                <div class="flex items-center justify-between text-slate-500 dark:text-slate-400">
                    <span class="text-xs font-medium uppercase tracking-wider">Riwayat Log</span>
                    <div class="w-8 h-8 rounded-xl bg-purple-500/10 flex items-center justify-center text-purple-500 text-base">
                        📜
                    </div>
                </div>
                <div class="mt-2">
                    <p class="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white" id="stat-hist">0</p>
                    <p class="text-[11px] text-slate-500 dark:text-slate-400 mt-1">Pembaruan tercatat</p>
                </div>
            </div>

        </section>

        <!-- SECTION 2: LEAFLET MAP SECTION (Compact & Sleek) -->
        <section class="bg-white dark:bg-darkcard border border-slate-200 dark:border-darkborder rounded-2xl overflow-hidden shadow-sm">
            <div class="p-3.5 sm:p-4 border-b border-slate-200 dark:border-darkborder flex flex-wrap items-center justify-between gap-2">
                <div class="flex items-center gap-2">
                    <span class="text-base sm:text-lg">📍</span>
                    <div>
                        <h2 class="text-sm font-bold text-slate-900 dark:text-white">Peta Sebaran Perangkat TV</h2>
                        <p class="text-[11px] text-slate-500 dark:text-slate-400">Lokasi geografis TV masjid yang terhubung</p>
                    </div>
                </div>
                <div class="flex items-center gap-2">
                    <span id="mapPointCount" class="text-[11px] font-semibold px-2.5 py-1 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700">
                        0 Titik Terdeteksi
                    </span>
                    <button 
                        onclick="resetMapView()" 
                        class="text-xs px-2.5 py-1 rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30 transition flex items-center gap-1"
                        title="Pusatkan Peta ke Seluruh Titik"
                    >
                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4"></path></svg>
                        <span>Pusatkan</span>
                    </button>
                </div>
            </div>
            <!-- Map Container (Compact Height ~280px) -->
            <div class="relative w-full h-[280px] sm:h-[300px] bg-slate-900">
                <div id="deviceMap" class="w-full h-full z-10"></div>
                <!-- Legend Overlay -->
                <div class="absolute bottom-2 left-2 z-20 bg-slate-950/85 backdrop-blur-md px-3 py-1.5 rounded-xl border border-slate-800 text-[10px] text-slate-300 flex items-center gap-3 pointer-events-none shadow-lg">
                    <div class="flex items-center gap-1.5">
                        <span class="w-2.5 h-2.5 rounded-full bg-emerald-500 pulse-live"></span>
                        <span>Online (24 Jam)</span>
                    </div>
                    <div class="flex items-center gap-1.5">
                        <span class="w-2.5 h-2.5 rounded-full bg-slate-400"></span>
                        <span>Offline</span>
                    </div>
                </div>
            </div>
        </section>

        <!-- SECTION: TIMELINE AKTIVASI & PENDAFTARAN TV -->
        <section class="bg-white dark:bg-darkcard border border-slate-200 dark:border-darkborder rounded-2xl overflow-hidden shadow-sm">
            <div class="p-3.5 sm:p-4 border-b border-slate-200 dark:border-darkborder flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div class="flex items-center gap-3">
                    <div class="w-9 h-9 rounded-xl bg-emerald-500/10 text-emerald-500 flex items-center justify-center text-lg flex-shrink-0">
                        📈
                    </div>
                    <div>
                        <div class="flex items-center gap-2">
                            <h2 class="text-sm font-bold text-slate-900 dark:text-white">Timeline Aktivasi & Pendaftaran TV</h2>
                            <span id="activationBadge" class="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
                                +0 TV Baru
                            </span>
                        </div>
                        <p class="text-[11px] text-slate-500 dark:text-slate-400">Grafik tren pertumbuhan perangkat TV yang terdaftar dari waktu ke waktu</p>
                    </div>
                </div>

                <div class="flex items-center gap-2 self-start sm:self-center">
                    <div class="inline-flex p-1 bg-slate-100 dark:bg-slate-800/80 rounded-xl text-xs font-semibold">
                        <button onclick="setActivationRange('7d')" id="btn-act-7d" class="px-2.5 py-1 rounded-lg text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition">7 Hari</button>
                        <button onclick="setActivationRange('30d')" id="btn-act-30d" class="px-2.5 py-1 rounded-lg bg-white dark:bg-darkcard shadow-sm text-slate-900 dark:text-white transition">30 Hari</button>
                        <button onclick="setActivationRange('all')" id="btn-act-all" class="px-2.5 py-1 rounded-lg text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition">Semua</button>
                    </div>
                </div>
            </div>

            <div class="p-4 sm:p-5 relative min-h-[240px] flex items-center justify-center">
                <div class="w-full h-56 relative">
                    <canvas id="activationChart"></canvas>
                </div>
                <div id="emptyActivationMsg" class="hidden absolute text-xs text-slate-400 text-center">
                    <p class="text-2xl mb-1">📅</p>
                    <p>Belum ada data timeline aktivasi perangkat.</p>
                </div>
            </div>
        </section>

        <!-- SECTION 3: PROVINCE CHART & LIVE TIMELINE -->
        <section class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            
            <!-- Left: Province Chart -->
            <div class="bg-white dark:bg-darkcard border border-slate-200 dark:border-darkborder rounded-2xl overflow-hidden shadow-sm flex flex-col">
                <div class="p-4 border-b border-slate-200 dark:border-darkborder flex items-center justify-between">
                    <div>
                        <h3 class="text-sm font-bold text-slate-900 dark:text-white">Sebaran Provinsi</h3>
                        <p class="text-[11px] text-slate-500 dark:text-slate-400">Distribusi wilayah perangkat TV</p>
                    </div>
                    <span class="text-xs px-2 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-500" id="provTotalLabel">0 Prov</span>
                </div>
                <div class="p-4 flex-1 flex flex-col justify-center items-center min-h-[260px] relative">
                    <div class="w-full h-48 relative flex items-center justify-center">
                        <canvas id="provChart"></canvas>
                        <div id="emptyProvMsg" class="hidden text-xs text-slate-500 text-center">
                            Belum ada data provinsi
                        </div>
                    </div>
                    <!-- Province breakdown top list -->
                    <div class="w-full mt-3 divide-y divide-slate-100 dark:divide-darkborder text-xs max-h-28 overflow-y-auto" id="provListSummary">
                        <!-- Filled by JS -->
                    </div>
                </div>
            </div>

            <!-- Right: Live Activity Feed (Timeline) -->
            <div class="lg:col-span-2 bg-white dark:bg-darkcard border border-slate-200 dark:border-darkborder rounded-2xl overflow-hidden shadow-sm flex flex-col">
                <div class="p-4 border-b border-slate-200 dark:border-darkborder flex items-center justify-between">
                    <div>
                        <h3 class="text-sm font-bold text-slate-900 dark:text-white">Aktivitas Terkini</h3>
                        <p class="text-[11px] text-slate-500 dark:text-slate-400">Laporan kas dan ping terakhir perangkat</p>
                    </div>
                    <div class="flex items-center gap-2">
                        <span class="w-2 h-2 rounded-full bg-emerald-500 pulse-live"></span>
                        <span class="text-[11px] text-slate-500 dark:text-slate-400 font-mono" id="timelineCount">0 log</span>
                    </div>
                </div>
                <div class="divide-y divide-slate-100 dark:divide-darkborder/70 max-h-[360px] overflow-y-auto flex-1" id="timeline">
                    <!-- Dynamic timeline items -->
                    <div class="p-6 text-center text-xs text-slate-500">
                        Memuat data riwayat...
                    </div>
                </div>
            </div>

        </section>

        <!-- SECTION 4: COMPREHENSIVE DEVICES TABLE -->
        <section class="bg-white dark:bg-darkcard border border-slate-200 dark:border-darkborder rounded-2xl overflow-hidden shadow-sm">
            
            <!-- Table Header Controls: Search & Filter Tabs -->
            <div class="p-4 sm:p-5 border-b border-slate-200 dark:border-darkborder flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                    <h3 class="text-base font-bold text-slate-900 dark:text-white">Daftar Perangkat Masjid</h3>
                    <p class="text-xs text-slate-500 dark:text-slate-400 mt-0.5">Monitoring status koneksi dan saldo kas per perangkat</p>
                </div>
                
                <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
                    <!-- Status Filter Tabs -->
                    <div class="inline-flex p-1 bg-slate-100 dark:bg-slate-800/80 rounded-xl text-xs font-semibold">
                        <button onclick="setFilter('all')" id="tab-all" class="px-3 py-1.5 rounded-lg bg-white dark:bg-darkcard shadow-sm text-slate-900 dark:text-white transition">Semua</button>
                        <button onclick="setFilter('online')" id="tab-online" class="px-3 py-1.5 rounded-lg text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition">Online</button>
                        <button onclick="setFilter('offline')" id="tab-offline" class="px-3 py-1.5 rounded-lg text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition">Offline</button>
                    </div>

                    <!-- Search Input -->
                    <div class="relative">
                        <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                        </div>
                        <input 
                            type="text" 
                            id="tableSearchInput" 
                            oninput="renderDeviceTable()" 
                            placeholder="Cari masjid / kota / ID..." 
                            class="w-full sm:w-64 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-darkborder rounded-xl pl-9 pr-4 py-2 text-xs text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition"
                        >
                    </div>
                </div>
            </div>

            <!-- Table Container (Horizontal Scroll on Mobile) -->
            <div class="overflow-x-auto">
                <table class="w-full text-left text-xs whitespace-nowrap sm:whitespace-normal">
                    <thead class="bg-slate-50/75 dark:bg-slate-900/50 text-slate-500 dark:text-slate-400 border-b border-slate-200 dark:border-darkborder font-semibold uppercase tracking-wider text-[11px]">
                        <tr>
                            <th class="py-3 px-4">Status</th>
                            <th class="py-3 px-4">Masjid & ID</th>
                            <th class="py-3 px-4">Lokasi</th>
                            <th class="py-3 px-4">Saldo Kas</th>
                            <th class="py-3 px-4">Terakhir Terhubung</th>
                            <th class="py-3 px-4 text-center">Aksi</th>
                        </tr>
                    </thead>
                    <tbody id="deviceTableBody" class="divide-y divide-slate-100 dark:divide-darkborder/70 text-slate-700 dark:text-slate-300">
                        <tr>
                            <td colspan="6" class="p-8 text-center text-slate-400">
                                Memuat daftar perangkat...
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- Table Footer Pagination / Count Info -->
            <div class="p-4 border-t border-slate-200 dark:border-darkborder flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 dark:text-slate-400 gap-2">
                <div id="tableRecordInfo">
                    Menampilkan 0 dari 0 perangkat
                </div>
                <div class="flex items-center gap-2">
                    <button id="prevPageBtn" onclick="changePage(-1)" class="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-darkborder hover:bg-slate-100 dark:hover:bg-slate-800 disabled:opacity-40 disabled:cursor-not-allowed">Sebelumnya</button>
                    <span id="pageNumberDisplay" class="font-mono font-semibold px-2">1</span>
                    <button id="nextPageBtn" onclick="changePage(1)" class="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-darkborder hover:bg-slate-100 dark:hover:bg-slate-800 disabled:opacity-40 disabled:cursor-not-allowed">Selanjutnya</button>
                </div>
            </div>

        </section>

    </main>

    <!-- DEVICE DETAIL MODAL -->
    <div id="deviceModal" class="fixed inset-0 z-50 hidden items-center justify-center p-4 bg-slate-950/75 backdrop-blur-sm transition-opacity">
        <div class="bg-white dark:bg-darkcard border border-slate-200 dark:border-darkborder w-full max-w-xl rounded-3xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh] animate-in fade-in zoom-in-95 duration-200">
            
            <!-- Modal Header -->
            <div class="p-5 border-b border-slate-200 dark:border-darkborder flex items-center justify-between bg-slate-50/50 dark:bg-slate-900/50">
                <div class="flex items-center gap-3">
                    <div class="w-10 h-10 rounded-2xl bg-emerald-500/10 text-emerald-500 flex items-center justify-center text-xl">
                        🕌
                    </div>
                    <div>
                        <h4 class="text-base font-bold text-slate-900 dark:text-white" id="modalMosqueName">Nama Masjid</h4>
                        <div class="flex items-center gap-2 mt-0.5">
                            <span id="modalStatusBadge" class="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-500">
                                <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span> Online
                            </span>
                            <span class="text-slate-400 text-xs">•</span>
                            <span class="text-xs text-slate-500 dark:text-slate-400 font-mono" id="modalDeviceIdShort">ID: -</span>
                        </div>
                    </div>
                </div>
                <button onclick="closeModal()" class="p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                </button>
            </div>

            <!-- Modal Body (Scrollable) -->
            <div class="p-5 space-y-4 overflow-y-auto text-xs">
                
                <!-- Quick Info Grid -->
                <div class="grid grid-cols-2 gap-3">
                    <div class="p-3 rounded-xl bg-slate-50 dark:bg-slate-900/70 border border-slate-200 dark:border-darkborder">
                        <span class="text-slate-500 dark:text-slate-400 block text-[11px]">Kota / Kabupaten</span>
                        <span class="font-bold text-slate-900 dark:text-white text-sm" id="modalCity">-</span>
                    </div>
                    <div class="p-3 rounded-xl bg-slate-50 dark:bg-slate-900/70 border border-slate-200 dark:border-darkborder">
                        <span class="text-slate-500 dark:text-slate-400 block text-[11px]">Provinsi</span>
                        <span class="font-bold text-slate-900 dark:text-white text-sm" id="modalProvince">-</span>
                    </div>
                    <div class="p-3 rounded-xl bg-slate-50 dark:bg-slate-900/70 border border-slate-200 dark:border-darkborder">
                        <span class="text-slate-500 dark:text-slate-400 block text-[11px]">Saldo Kas Dilaporkan</span>
                        <span class="font-bold text-emerald-500 text-sm" id="modalKasBalance">Rp 0</span>
                    </div>
                    <div class="p-3 rounded-xl bg-slate-50 dark:bg-slate-900/70 border border-slate-200 dark:border-darkborder">
                        <span class="text-slate-500 dark:text-slate-400 block text-[11px]">Terakhir Aktif</span>
                        <span class="font-semibold text-slate-800 dark:text-slate-200 text-xs" id="modalLastSeen">-</span>
                    </div>
                </div>

                <!-- GPS Coordinates & Google Maps Link -->
                <div class="p-3 rounded-xl bg-slate-50 dark:bg-slate-900/70 border border-slate-200 dark:border-darkborder flex items-center justify-between">
                    <div>
                        <span class="text-slate-500 dark:text-slate-400 block text-[11px]">Koordinat Geografis</span>
                        <span class="font-mono text-slate-800 dark:text-slate-200" id="modalCoords">-6.200000, 106.800000</span>
                    </div>
                    <a id="modalGmapsLink" href="#" target="_blank" rel="noopener noreferrer" class="px-3 py-1.5 rounded-lg bg-emerald-500 text-slate-950 font-bold hover:bg-emerald-400 transition inline-flex items-center gap-1.5">
                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"></path></svg>
                        <span>Google Maps</span>
                    </a>
                </div>

                <!-- Full Device ID with Copy -->
                <div class="p-3 rounded-xl bg-slate-50 dark:bg-slate-900/70 border border-slate-200 dark:border-darkborder">
                    <span class="text-slate-500 dark:text-slate-400 block text-[11px] mb-1">Device ID Lengkap</span>
                    <div class="flex items-center justify-between gap-2">
                        <code class="font-mono text-[11px] text-emerald-400 select-all truncate" id="modalDeviceIdFull">...</code>
                        <button onclick="copyToClipboard(currentSelectedDeviceId)" class="px-2.5 py-1 rounded bg-slate-200 dark:bg-slate-800 hover:bg-emerald-500 hover:text-slate-950 font-semibold transition flex-shrink-0">
                            Salin
                        </button>
                    </div>
                </div>

                <!-- Payload JSON Data Preview -->
                <div class="border border-slate-200 dark:border-darkborder rounded-xl overflow-hidden">
                    <div class="p-2.5 bg-slate-100 dark:bg-slate-900/90 text-slate-600 dark:text-slate-400 font-semibold flex items-center justify-between text-[11px]">
                        <span>Payload Data TV (JSON)</span>
                        <button onclick="togglePayloadRaw()" class="text-emerald-500 hover:underline">Toggle Tampilan</button>
                    </div>
                    <pre id="modalPayloadJson" class="p-3 bg-slate-950 text-emerald-300 font-mono text-[11px] max-h-48 overflow-auto whitespace-pre-wrap select-all">{}</pre>
                </div>

            </div>

            <!-- Modal Footer -->
            <div class="p-4 border-t border-slate-200 dark:border-darkborder bg-slate-50/50 dark:bg-slate-900/50 flex justify-end">
                <button onclick="closeModal()" class="px-5 py-2 rounded-xl bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 font-bold text-xs transition">
                    Tutup
                </button>
            </div>

        </div>
    </div>

    <!-- Toast Notification for Copy / Actions -->
    <div id="toast" class="fixed bottom-6 right-6 z-50 bg-slate-900 text-emerald-400 border border-emerald-500/30 px-4 py-2.5 rounded-2xl shadow-xl text-xs font-semibold transform translate-y-12 opacity-0 transition-all duration-300 flex items-center gap-2">
        <svg class="w-4 h-4 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
        <span id="toastMsg">Teks berhasil disalin!</span>
    </div>

    <!-- Footer -->
    <footer class="mt-auto py-6 border-t border-slate-200 dark:border-darkborder text-center text-xs text-slate-500 dark:text-slate-500">
        <div class="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
            <p>&copy; <?php echo date('Y'); ?> Jadwal Sholat TV • Monitoring Digital Masjid</p>
            <p class="text-[11px]">Versi Dashboard 2.2 • PWA Ready</p>
        </div>
    </footer>

    <!-- SCRIPT APPLICATION LOGIC -->
    <script>
        // Global State
        let rawDevices = [];
        let rawHistory = [];
        let rawStats = {};
        let currentFilter = 'all';
        let currentPage = 1;
        const pageSize = 10;
        let provChartInstance = null;
        let activationChartInstance = null;
        let currentActivationRange = '30d';
        let leafletMap = null;
        let mapMarkersLayer = null;
        let currentSelectedDeviceId = '';
        let cityMappingData = null;

        // Load City & Province Mapping Dictionary
        async function loadCityMapping() {
            if (cityMappingData) return cityMappingData;
            try {
                const res = await fetch('cities_mapping.json');
                cityMappingData = await res.json();
            } catch(e) {
                console.warn('Failed to load cities_mapping.json', e);
            }
            return cityMappingData;
        }

        // Resolve Province from City Code or Name
        function resolveProvince(item) {
            if (item.province && item.province.trim() !== '' && item.province.trim() !== 'Lainnya') {
                return item.province.trim();
            }
            const city = String(item.city || '').trim();
            if (!city || !cityMappingData) return item.province || 'Lainnya';

            // Check by ID (e.g. 1001, 1301)
            if (cityMappingData.by_id && cityMappingData.by_id[city]) {
                return cityMappingData.by_id[city].province || 'Lainnya';
            }
            // Check by full name (e.g. KAB. SLEMAN)
            const upper = city.toUpperCase();
            if (cityMappingData.by_name && cityMappingData.by_name[upper]) {
                return cityMappingData.by_name[upper];
            }
            // Check by cleaned name (e.g. SLEMAN)
            const clean = upper.replace(/^(KAB\.|KABUPATEN|KOTA)\s+/i, '').trim();
            if (cityMappingData.by_name && cityMappingData.by_name[clean]) {
                return cityMappingData.by_name[clean];
            }
            return 'Lainnya';
        }

        // Resolve Human-Readable City Name
        function resolveCityName(item) {
            const city = String(item.city || '').trim();
            if (!city) return '-';
            if (cityMappingData?.by_id && cityMappingData.by_id[city]) {
                return cityMappingData.by_id[city].name || city;
            }
            return city;
        }

        // Resolve GPS Coordinates (Fall back to Province centroid if default -6.2, 106.8)
        function resolveCoordinates(dev, idx) {
            let lat = parseFloat(dev.lat);
            let lng = parseFloat(dev.lng);
            const isDefault = isNaN(lat) || isNaN(lng) || (lat === 0 && lng === 0) || (Math.abs(lat - (-6.2)) < 0.001 && Math.abs(lng - 106.8) < 0.001);

            if (isDefault && dev.province && cityMappingData?.prov_coords && cityMappingData.prov_coords[dev.province]) {
                const base = cityMappingData.prov_coords[dev.province];
                const hash = (dev.device_id || '').split('').reduce((acc, c) => acc + c.charCodeAt(0), idx + 1);
                lat = base[0] + (((hash % 40) - 20) * 0.006);
                lng = base[1] + (((Math.floor(hash / 40) % 40) - 20) * 0.006);
            }
            return { lat, lng };
        }

        // Indonesian Rupiah Formatter
        function formatRupiah(value) {
            return 'Rp ' + Number(value || 0).toLocaleString('id-ID');
        }

        // Relative Time Formatter ("5 menit lalu", etc.)
        function timeAgo(dateString) {
            if (!dateString) return '-';
            const date = new Date(dateString.replace(' ', 'T'));
            if (isNaN(date.getTime())) return dateString;
            const now = new Date();
            const diffSec = Math.floor((now - date) / 1000);
            
            if (diffSec < 60) return 'Baru saja';
            if (diffSec < 3600) return Math.floor(diffSec / 60) + ' menit lalu';
            if (diffSec < 86400) return Math.floor(diffSec / 3600) + ' jam lalu';
            if (diffSec < 604800) return Math.floor(diffSec / 86400) + ' hari lalu';
            return date.toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' });
        }

        // Clock & Date updater
        function updateClock() {
            const now = new Date();
            const clockEl = document.getElementById('digitalClock');
            const dateEl = document.getElementById('currentDate');
            
            const hours = String(now.getHours()).padStart(2, '0');
            const minutes = String(now.getMinutes()).padStart(2, '0');
            const seconds = String(now.getSeconds()).padStart(2, '0');
            
            if (clockEl) clockEl.textContent = `${hours}:${minutes}:${seconds} WIB`;
            
            const days = ['Minggu', 'Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu'];
            const months = ['Jan', 'Feb', 'Mar', 'Apr', 'Mei', 'Jun', 'Jul', 'Agu', 'Sep', 'Okt', 'Nov', 'Des'];
            const dayName = days[now.getDay()];
            const monthName = months[now.getMonth()];
            
            if (dateEl) dateEl.textContent = `${dayName}, ${now.getDate()} ${monthName} ${now.getFullYear()}`;
        }
        setInterval(updateClock, 1000);
        updateClock();

        // Theme Toggle Handler
        const html = document.documentElement;
        const sunIcon = document.getElementById('sunIcon');
        const moonIcon = document.getElementById('moonIcon');
        const themeBtn = document.getElementById('themeToggleBtn');

        function applyTheme(isDark) {
            html.classList.toggle('dark', isDark);
            html.classList.toggle('light', !isDark);
            if (sunIcon && moonIcon) {
                sunIcon.classList.toggle('hidden', !isDark);
                moonIcon.classList.toggle('hidden', isDark);
            }
            localStorage.setItem('masjidtv_theme', isDark ? 'dark' : 'light');
            
            // Re-render chart text colors if chart exists
            if (provChartInstance) {
                provChartInstance.options.plugins.legend.labels.color = isDark ? '#cbd5e1' : '#475569';
                provChartInstance.update();
            }

            if (activationChartInstance) {
                const gridColor = isDark ? 'rgba(255, 255, 255, 0.06)' : 'rgba(0, 0, 0, 0.06)';
                const textColor = isDark ? '#94a3b8' : '#64748b';
                activationChartInstance.options.scales.x.grid.color = gridColor;
                activationChartInstance.options.scales.x.ticks.color = textColor;
                activationChartInstance.options.scales.y.grid.color = gridColor;
                activationChartInstance.options.scales.y.ticks.color = textColor;
                activationChartInstance.options.plugins.legend.labels.color = textColor;
                activationChartInstance.update();
            }
        }

        const savedTheme = localStorage.getItem('masjidtv_theme') || 'dark';
        applyTheme(savedTheme === 'dark');

        if (themeBtn) {
            themeBtn.onclick = () => {
                const isCurrentlyDark = html.classList.contains('dark');
                applyTheme(!isCurrentlyDark);
            };
        }

        // Toast Message Utility
        function showToast(msg) {
            const toast = document.getElementById('toast');
            const toastMsg = document.getElementById('toastMsg');
            toastMsg.textContent = msg;
            toast.classList.remove('translate-y-12', 'opacity-0');
            toast.classList.add('translate-y-0', 'opacity-100');
            setTimeout(() => {
                toast.classList.add('translate-y-12', 'opacity-0');
                toast.classList.remove('translate-y-0', 'opacity-100');
            }, 2500);
        }

        function copyToClipboard(text) {
            if (!text) return;
            navigator.clipboard.writeText(text).then(() => {
                showToast('Teks berhasil disalin!');
            }).catch(() => {
                showToast('Gagal menyalin teks');
            });
        }

        // Initialize Leaflet Map (100% Free - No API Key Required)
        function initMap() {
            if (leafletMap) {
                setTimeout(() => leafletMap.invalidateSize(), 200);
                return;
            }
            
            const mapContainer = document.getElementById('deviceMap');
            if (!mapContainer) return;

            // Center of Indonesia coordinates
            const indonesiaCenter = [-2.5489, 118.0149];
            leafletMap = L.map('deviceMap', {
                zoomControl: true,
                scrollWheelZoom: false
            }).setView(indonesiaCenter, 5);

            // Official OpenStreetMap tile layer (No API key required)
            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a> contributors',
                maxZoom: 19
            }).addTo(leafletMap);

            mapMarkersLayer = L.featureGroup().addTo(leafletMap);
            setTimeout(() => leafletMap.invalidateSize(), 300);
        }

        // Render Leaflet Markers
        function updateMapMarkers(devices) {
            if (!leafletMap || !mapMarkersLayer) return;
            mapMarkersLayer.clearLayers();

            const now = Date.now();
            let validPoints = 0;

            devices.forEach(dev => {
                const lat = parseFloat(dev.lat);
                const lng = parseFloat(dev.lng);

                if (!isNaN(lat) && !isNaN(lng) && (lat !== 0 || lng !== 0)) {
                    validPoints++;
                    const isOnline = (now - new Date(dev.last_seen.replace(' ', 'T')).getTime()) < 86400000;
                    const markerColor = isOnline ? '#10b981' : '#94a3b8';
                    const markerFill = isOnline ? '#059669' : '#64748b';

                    const customMarker = L.circleMarker([lat, lng], {
                        radius: 8,
                        fillColor: markerFill,
                        color: markerColor,
                        weight: 2,
                        opacity: 1,
                        fillOpacity: 0.85
                    });

                    const popupContent = `
                        <div class="text-xs space-y-1">
                            <div class="flex items-center justify-between gap-2 border-b border-slate-700/50 pb-1">
                                <span class="font-bold text-sm text-white">${dev.mosque || 'Masjid TV'}</span>
                                <span class="px-1.5 py-0.5 rounded text-[10px] font-bold ${isOnline ? 'bg-emerald-500/20 text-emerald-400' : 'bg-slate-700 text-slate-300'}">
                                    ${isOnline ? 'ONLINE' : 'OFFLINE'}
                                </span>
                            </div>
                            <p class="text-slate-300">${dev.city || '-'}, ${dev.province || '-'}</p>
                            <p class="text-emerald-400 font-semibold font-mono">${formatRupiah(dev.payload?.kas?.balance || 0)}</p>
                            <p class="text-[10px] text-slate-400">Aktif: ${timeAgo(dev.last_seen)}</p>
                            <button onclick="openModal('${dev.device_id}')" class="mt-2 w-full text-center py-1 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold rounded-lg text-[11px] transition">
                                Lihat Detail
                            </button>
                        </div>
                    `;

                    customMarker.bindPopup(popupContent);
                    mapMarkersLayer.addLayer(customMarker);
                }
            });

            document.getElementById('mapPointCount').textContent = `${validPoints} Titik Terdeteksi`;

            // Adjust view if markers exist
            if (validPoints > 0) {
                try {
                    leafletMap.fitBounds(mapMarkersLayer.getBounds(), { padding: [30, 30], maxZoom: 12 });
                } catch(e) {}
            }
        }

        function resetMapView() {
            if (mapMarkersLayer && mapMarkersLayer.getLayers().length > 0) {
                leafletMap.fitBounds(mapMarkersLayer.getBounds(), { padding: [30, 30] });
            } else if (leafletMap) {
                leafletMap.setView([-2.5489, 118.0149], 5);
            }
        }

        // Timeline Aktivasi Chart & Filter
        function setActivationRange(range) {
            currentActivationRange = range;
            ['7d', '30d', 'all'].forEach(r => {
                const btn = document.getElementById('btn-act-' + r);
                if (btn) {
                    if (r === range) {
                        btn.className = 'px-2.5 py-1 rounded-lg bg-white dark:bg-darkcard shadow-sm text-slate-900 dark:text-white transition';
                    } else {
                        btn.className = 'px-2.5 py-1 rounded-lg text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition';
                    }
                }
            });
            updateActivationChart(rawDevices);
        }

        function formatShortIndoDate(d) {
            const months = ['Jan', 'Feb', 'Mar', 'Apr', 'Mei', 'Jun', 'Jul', 'Agu', 'Sep', 'Okt', 'Nov', 'Des'];
            return `${d.getDate()} ${months[d.getMonth()]}`;
        }

        function updateActivationChart(devices) {
            const ctx = document.getElementById('activationChart');
            const emptyMsg = document.getElementById('emptyActivationMsg');
            if (!ctx) return;

            const now = new Date();
            const dateMap = {};
            const labels = [];
            const dayKeys = [];

            let daysCount = currentActivationRange === '7d' ? 7 : (currentActivationRange === '30d' ? 30 : 0);

            if (daysCount > 0) {
                for (let i = daysCount - 1; i >= 0; i--) {
                    const d = new Date();
                    d.setDate(now.getDate() - i);
                    const yyyy = d.getFullYear();
                    const mm = String(d.getMonth() + 1).padStart(2, '0');
                    const dd = String(d.getDate()).padStart(2, '0');
                    const key = `${yyyy}-${mm}-${dd}`;
                    dayKeys.push(key);
                    dateMap[key] = 0;
                    labels.push(formatShortIndoDate(d));
                }
            } else {
                let earliest = new Date();
                devices.forEach(dev => {
                    const dStr = dev.created_at || dev.last_seen;
                    if (dStr) {
                        const d = new Date(dStr.replace(' ', 'T'));
                        if (!isNaN(d.getTime()) && d < earliest) earliest = d;
                    }
                });
                const diffDays = Math.min(90, Math.max(14, Math.ceil(Math.abs(now - earliest) / (1000 * 60 * 60 * 24))));
                for (let i = diffDays; i >= 0; i--) {
                    const d = new Date();
                    d.setDate(now.getDate() - i);
                    const yyyy = d.getFullYear();
                    const mm = String(d.getMonth() + 1).padStart(2, '0');
                    const dd = String(d.getDate()).padStart(2, '0');
                    const key = `${yyyy}-${mm}-${dd}`;
                    dayKeys.push(key);
                    dateMap[key] = 0;
                    labels.push(formatShortIndoDate(d));
                }
            }

            let periodNewCount = 0;
            devices.forEach(dev => {
                const dateRaw = dev.created_at || dev.last_seen;
                if (dateRaw) {
                    const key = dateRaw.slice(0, 10);
                    if (dateMap.hasOwnProperty(key)) {
                        dateMap[key] = (dateMap[key] || 0) + 1;
                        periodNewCount++;
                    }
                }
            });

            const dailyData = dayKeys.map(k => dateMap[k] || 0);

            // Calculate cumulative total line
            let runningSum = 0;
            const cumulativeData = dailyData.map(v => {
                runningSum += v;
                return runningSum;
            });

            const badge = document.getElementById('activationBadge');
            if (badge) {
                const rangeLabel = currentActivationRange === '7d' ? '7 Hari' : (currentActivationRange === '30d' ? '30 Hari' : 'Semua');
                badge.textContent = `+${periodNewCount} TV Baru (${rangeLabel})`;
            }

            if (activationChartInstance) {
                activationChartInstance.destroy();
            }

            if (devices.length === 0 && emptyMsg) {
                emptyMsg.classList.remove('hidden');
            } else if (emptyMsg) {
                emptyMsg.classList.add('hidden');
            }

            const isDark = html.classList.contains('dark');
            const gridColor = isDark ? 'rgba(255, 255, 255, 0.06)' : 'rgba(0, 0, 0, 0.06)';
            const textColor = isDark ? '#94a3b8' : '#64748b';

            activationChartInstance = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [
                        {
                            label: 'Pendaftar TV Baru (Harian)',
                            data: dailyData,
                            borderColor: '#10b981',
                            backgroundColor: 'rgba(16, 185, 129, 0.12)',
                            borderWidth: 2.5,
                            fill: true,
                            tension: 0.35,
                            pointBackgroundColor: '#10b981',
                            pointHoverRadius: 6,
                            pointRadius: dailyData.length > 30 ? 0 : 3
                        },
                        {
                            label: 'Total Kumulatif Periode Ini',
                            data: cumulativeData,
                            borderColor: '#38bdf8',
                            borderDash: [4, 4],
                            borderWidth: 2,
                            fill: false,
                            tension: 0.25,
                            pointRadius: 0
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    interaction: {
                        mode: 'index',
                        intersect: false
                    },
                    plugins: {
                        legend: {
                            display: true,
                            position: 'top',
                            align: 'end',
                            labels: {
                                color: textColor,
                                boxWidth: 12,
                                boxHeight: 12,
                                font: { size: 11, family: 'Inter' }
                            }
                        },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    return ` ${context.dataset.label}: ${context.raw} TV`;
                                }
                            }
                        }
                    },
                    scales: {
                        x: {
                            grid: { color: gridColor },
                            ticks: {
                                color: textColor,
                                maxRotation: 0,
                                autoSkip: true,
                                maxTicksLimit: 10,
                                font: { size: 10, family: 'Inter' }
                            }
                        },
                        y: {
                            beginAtZero: true,
                            grid: { color: gridColor },
                            ticks: {
                                color: textColor,
                                precision: 0,
                                font: { size: 10, family: 'Inter' }
                            }
                        }
                    }
                }
            });
        }

        // Render Province Chart & Summary List
        function updateProvinceChart(devices) {
            const counts = {};
            devices.forEach(dev => {
                const prov = dev.province ? dev.province.trim() : 'Lainnya';
                counts[prov] = (counts[prov] || 0) + 1;
            });

            const labels = Object.keys(counts);
            const data = Object.values(counts);
            const total = data.reduce((a, b) => a + b, 0);

            document.getElementById('provTotalLabel').textContent = `${labels.length} Prov`;

            const colors = ['#10b981', '#38bdf8', '#a855f7', '#f59e0b', '#ec4899', '#14b8a6', '#6366f1', '#eab308'];

            const ctx = document.getElementById('provChart');
            if (provChartInstance) {
                provChartInstance.destroy();
            }

            if (labels.length === 0) {
                document.getElementById('emptyProvMsg').classList.remove('hidden');
            } else {
                document.getElementById('emptyProvMsg').classList.add('hidden');
                const isDark = html.classList.contains('dark');

                provChartInstance = new Chart(ctx, {
                    type: 'doughnut',
                    data: {
                        labels: labels,
                        datasets: [{
                            data: data,
                            backgroundColor: colors.slice(0, labels.length),
                            borderWidth: 2,
                            borderColor: isDark ? '#0f172a' : '#ffffff'
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                display: false
                            },
                            tooltip: {
                                callbacks: {
                                    label: function(item) {
                                        const pct = ((item.raw / total) * 100).toFixed(1);
                                        return ` ${item.label}: ${item.raw} TV (${pct}%)`;
                                    }
                                }
                            }
                        },
                        cutout: '70%'
                    }
                });
            }

            // Summary list below chart
            const listEl = document.getElementById('provListSummary');
            listEl.innerHTML = '';
            
            // Sort provinces by device count descending
            const sorted = Object.entries(counts).sort((a, b) => b[1] - a[1]);
            sorted.forEach(([provName, count], idx) => {
                const pct = total > 0 ? Math.round((count / total) * 100) : 0;
                const col = colors[idx % colors.length];
                listEl.innerHTML += `
                    <div class="py-1.5 flex items-center justify-between">
                        <div class="flex items-center gap-2 truncate">
                            <span class="w-2.5 h-2.5 rounded-full flex-shrink-0" style="background-color: ${col}"></span>
                            <span class="text-slate-800 dark:text-slate-200 truncate">${provName}</span>
                        </div>
                        <div class="flex items-center gap-2 font-mono flex-shrink-0">
                            <span class="font-bold text-slate-900 dark:text-white">${count}</span>
                            <span class="text-slate-400 text-[10px]">(${pct}%)</span>
                        </div>
                    </div>
                `;
            });
        }

        // Render Timeline Feed
        function updateTimeline(history) {
            const tl = document.getElementById('timeline');
            document.getElementById('timelineCount').textContent = `${history.length} log`;
            tl.innerHTML = '';

            if (history.length === 0) {
                tl.innerHTML = '<div class="p-8 text-center text-xs text-slate-500">Belum ada aktivitas tercatat.</div>';
                return;
            }

            history.slice(0, 50).forEach(item => {
                const bal = formatRupiah(item.kas_balance);
                const title = item.mosque || ('Perangkat ' + (item.device_id ? item.device_id.slice(-8) : '-'));
                const loc = [item.city, item.province].filter(Boolean).join(', ') || 'Lokasi belum disetel';
                
                tl.innerHTML += `
                    <div class="p-3.5 hover:bg-slate-50 dark:hover:bg-slate-800/40 transition flex items-center justify-between gap-3">
                        <div class="flex items-center gap-3 min-w-0">
                            <div class="w-8 h-8 rounded-xl bg-emerald-500/10 text-emerald-500 flex items-center justify-center flex-shrink-0 text-sm">
                                🕌
                            </div>
                            <div class="min-w-0">
                                <p class="text-xs font-bold text-slate-800 dark:text-slate-200 truncate">${title}</p>
                                <p class="text-[11px] text-slate-500 dark:text-slate-400 truncate">${loc}</p>
                            </div>
                        </div>
                        <div class="text-right flex-shrink-0">
                            <p class="text-xs font-mono font-bold text-emerald-500">${bal}</p>
                            <p class="text-[10px] text-slate-400" title="${item.created_at}">${timeAgo(item.created_at)}</p>
                        </div>
                    </div>
                `;
            });
        }

        // Filter Table Handler
        function setFilter(filterType) {
            currentFilter = filterType;
            ['all', 'online', 'offline'].forEach(tab => {
                const el = document.getElementById('tab-' + tab);
                if (tab === filterType) {
                    el.className = 'px-3 py-1.5 rounded-lg bg-white dark:bg-darkcard shadow-sm text-slate-900 dark:text-white transition';
                } else {
                    el.className = 'px-3 py-1.5 rounded-lg text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition';
                }
            });
            currentPage = 1;
            renderDeviceTable();
        }

        // Render Device Table
        function renderDeviceTable() {
            const tbody = document.getElementById('deviceTableBody');
            const searchVal = (document.getElementById('tableSearchInput')?.value || '').toLowerCase().trim();
            const now = Date.now();

            let filtered = rawDevices.filter(dev => {
                const isOnline = (now - new Date(dev.last_seen.replace(' ', 'T')).getTime()) < 86400000;
                
                // Status Filter
                if (currentFilter === 'online' && !isOnline) return false;
                if (currentFilter === 'offline' && isOnline) return false;

                // Search Filter
                if (searchVal) {
                    const matchId = (dev.device_id || '').toLowerCase().includes(searchVal);
                    const matchMosque = (dev.mosque || '').toLowerCase().includes(searchVal);
                    const matchCity = (dev.city || '').toLowerCase().includes(searchVal);
                    const matchProv = (dev.province || '').toLowerCase().includes(searchVal);
                    if (!matchId && !matchMosque && !matchCity && !matchProv) return false;
                }

                return true;
            });

            // Pagination calculations
            const totalRecords = filtered.length;
            const totalPages = Math.ceil(totalRecords / pageSize) || 1;
            if (currentPage > totalPages) currentPage = totalPages;
            if (currentPage < 1) currentPage = 1;

            const startIdx = (currentPage - 1) * pageSize;
            const pageData = filtered.slice(startIdx, startIdx + pageSize);

            document.getElementById('tableRecordInfo').textContent = `Menampilkan ${totalRecords === 0 ? 0 : startIdx + 1} - ${Math.min(startIdx + pageSize, totalRecords)} dari ${totalRecords} perangkat`;
            document.getElementById('pageNumberDisplay').textContent = `${currentPage} / ${totalPages}`;
            document.getElementById('prevPageBtn').disabled = currentPage <= 1;
            document.getElementById('nextPageBtn').disabled = currentPage >= totalPages;

            tbody.innerHTML = '';

            if (pageData.length === 0) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="6" class="p-8 text-center text-slate-400">
                            Tidak ada perangkat yang sesuai dengan filter.
                        </td>
                    </tr>
                `;
                return;
            }

            pageData.forEach(dev => {
                const isOnline = (now - new Date(dev.last_seen.replace(' ', 'T')).getTime()) < 86400000;
                const mosque = dev.mosque || 'Masjid Belum Dinamai';
                const idShort = dev.device_id ? dev.device_id.slice(-10) : '-';
                const loc = [dev.city, dev.province].filter(Boolean).join(', ') || '-';
                const kas = formatRupiah(dev.payload?.kas?.balance || 0);

                const statusBadge = isOnline 
                    ? `<span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20"><span class="w-1.5 h-1.5 rounded-full bg-emerald-500 pulse-live"></span>Online</span>`
                    : `<span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-slate-500/10 text-slate-500 dark:text-slate-400 border border-slate-500/20"><span class="w-1.5 h-1.5 rounded-full bg-slate-400"></span>Offline</span>`;

                tbody.innerHTML += `
                    <tr class="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition">
                        <td class="py-3 px-4">${statusBadge}</td>
                        <td class="py-3 px-4">
                            <div class="font-bold text-slate-900 dark:text-white">${mosque}</div>
                            <div class="flex items-center gap-1.5 mt-0.5">
                                <span class="font-mono text-[10px] text-slate-400">ID: ...${idShort}</span>
                                <button onclick="copyToClipboard('${dev.device_id}')" class="text-slate-400 hover:text-emerald-500" title="Salin Full Device ID">
                                    <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"></path></svg>
                                </button>
                            </div>
                        </td>
                        <td class="py-3 px-4 text-slate-600 dark:text-slate-300">${loc}</td>
                        <td class="py-3 px-4 font-mono font-bold text-emerald-600 dark:text-emerald-400">${kas}</td>
                        <td class="py-3 px-4">
                            <div class="text-slate-700 dark:text-slate-300 font-medium">${timeAgo(dev.last_seen)}</div>
                            <div class="text-[10px] text-slate-400">${dev.last_seen}</div>
                        </td>
                        <td class="py-3 px-4 text-center">
                            <button onclick="openModal('${dev.device_id}')" class="px-3 py-1.5 rounded-lg bg-emerald-500/10 hover:bg-emerald-500 text-emerald-600 dark:text-emerald-400 hover:text-slate-950 font-bold transition text-xs border border-emerald-500/20">
                                Detail
                            </button>
                        </td>
                    </tr>
                `;
            });
        }

        function changePage(direction) {
            currentPage += direction;
            renderDeviceTable();
        }

        // Open Device Detail Modal
        function openModal(deviceId) {
            const dev = rawDevices.find(d => d.device_id === deviceId);
            if (!dev) return;

            currentSelectedDeviceId = dev.device_id;
            const now = Date.now();
            const isOnline = (now - new Date(dev.last_seen.replace(' ', 'T')).getTime()) < 86400000;

            document.getElementById('modalMosqueName').textContent = dev.mosque || 'Masjid TV';
            document.getElementById('modalDeviceIdShort').textContent = 'ID: ' + dev.device_id.slice(-12);
            document.getElementById('modalDeviceIdFull').textContent = dev.device_id;
            document.getElementById('modalCity').textContent = dev.city || '-';
            document.getElementById('modalProvince').textContent = dev.province || '-';
            document.getElementById('modalKasBalance').textContent = formatRupiah(dev.payload?.kas?.balance || 0);
            document.getElementById('modalLastSeen').textContent = `${dev.last_seen} (${timeAgo(dev.last_seen)})`;

            const lat = parseFloat(dev.lat) || 0;
            const lng = parseFloat(dev.lng) || 0;
            document.getElementById('modalCoords').textContent = `${lat.toFixed(6)}, ${lng.toFixed(6)}`;
            document.getElementById('modalGmapsLink').href = `https://www.google.com/maps?q=${lat},${lng}`;

            const statusEl = document.getElementById('modalStatusBadge');
            if (isOnline) {
                statusEl.className = 'inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-500';
                statusEl.innerHTML = '<span class="w-1.5 h-1.5 rounded-full bg-emerald-500 pulse-live"></span> Online';
            } else {
                statusEl.className = 'inline-flex items-center gap-1 text-[11px] font-semibold text-slate-400';
                statusEl.innerHTML = '<span class="w-1.5 h-1.5 rounded-full bg-slate-400"></span> Offline';
            }

            document.getElementById('modalPayloadJson').textContent = JSON.stringify(dev.payload || {}, null, 2);

            const modal = document.getElementById('deviceModal');
            modal.classList.remove('hidden');
            modal.classList.add('flex');
        }

        function closeModal() {
            const modal = document.getElementById('deviceModal');
            modal.classList.add('hidden');
            modal.classList.remove('flex');
        }

        // Close modal on click outside or Escape
        window.addEventListener('click', (e) => {
            const modal = document.getElementById('deviceModal');
            if (e.target === modal) closeModal();
        });
        window.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') closeModal();
        });

        function togglePayloadRaw() {
            const pre = document.getElementById('modalPayloadJson');
            pre.classList.toggle('whitespace-pre-wrap');
            pre.classList.toggle('whitespace-pre');
        }

        // Main Data Fetcher
        async function loadAllData(isManual = false) {
            initMap(); // Ensure Leaflet map is initialized immediately (No API key needed)

            const spinIcon = document.getElementById('refreshSpinIcon');
            if (spinIcon) spinIcon.classList.add('animate-spin');

            const statusBanner = document.getElementById('connStatusBanner');

            try {
                const ts = Date.now();
                const [devicesRes, statsRes, historyRes] = await Promise.all([
                    fetch(`../webservice/api.php?action=devices&t=${ts}`).then(async r => {
                        const txt = await r.text();
                        try {
                            return JSON.parse(txt);
                        } catch(e) {
                            throw new Error('Respon webservice bukan JSON (Periksa koneksi database di webservice/config.php)');
                        }
                    }),
                    fetch(`../webservice/api.php?action=stats&t=${ts}`).then(async r => {
                        const txt = await r.text();
                        try {
                            return JSON.parse(txt);
                        } catch(e) {
                            throw new Error('Respon stats bukan JSON');
                        }
                    }),
                    fetch(`../webservice/api.php?action=history&t=${ts}`).then(async r => {
                        const txt = await r.text();
                        try {
                            return JSON.parse(txt);
                        } catch(e) {
                            throw new Error('Respon history bukan JSON');
                        }
                    })
                ]);

                rawDevices = Array.isArray(devicesRes) ? devicesRes : [];
                rawStats = statsRes || {};
                rawHistory = Array.isArray(historyRes) ? historyRes : [];

                if (statusBanner) statusBanner.classList.add('hidden');

                // Load mapping dictionary and resolve city codes & provinces
                await loadCityMapping();

                rawDevices = rawDevices.map((dev, idx) => {
                    dev.province = resolveProvince(dev);
                    dev.city = resolveCityName(dev);
                    const coords = resolveCoordinates(dev, idx);
                    dev.lat = coords.lat;
                    dev.lng = coords.lng;
                    return dev;
                });

                rawHistory = rawHistory.map((h, idx) => {
                    h.province = resolveProvince(h);
                    h.city = resolveCityName(h);
                    return h;
                });

                // Calculate accurate distinct provinces from mapped devices
                const distinctProvinces = new Set(rawDevices.map(d => d.province).filter(p => p && p !== 'Lainnya')).size;

                // Update Key Stats KPI
                document.getElementById('stat-total').textContent = rawStats.total ?? rawDevices.length;
                document.getElementById('stat-active').textContent = rawStats.active ?? 0;
                const totalNum = rawStats.total || rawDevices.length || 1;
                const activeNum = rawStats.active || 0;
                const activePct = Math.round((activeNum / totalNum) * 100);
                document.getElementById('stat-active-pct').textContent = `${activePct}% online`;
                document.getElementById('stat-cash').textContent = formatRupiah(rawStats.cash || 0);
                document.getElementById('stat-prov').textContent = distinctProvinces > 0 ? distinctProvinces : (rawStats.prov ?? 0);
                document.getElementById('stat-hist').textContent = rawHistory.length;

                // Update Visuals & Map
                initMap();
                updateMapMarkers(rawDevices);
                updateActivationChart(rawDevices);
                updateProvinceChart(rawDevices);
                updateTimeline(rawHistory);
                renderDeviceTable();

                if (isManual) {
                    showToast('Data berhasil diperbarui!');
                }

            } catch (err) {
                console.error('Error fetching dashboard data:', err);
                if (statusBanner) {
                    statusBanner.classList.remove('hidden');
                    document.getElementById('connStatusText').textContent = 'Gagal memuat data dari webservice: ' + err.message;
                }
            } finally {
                if (spinIcon) {
                    setTimeout(() => spinIcon.classList.remove('animate-spin'), 400);
                }
            }
        }

        // PWA Service Worker Registration
        if ('serviceWorker' in navigator) {
            window.addEventListener('load', () => {
                navigator.serviceWorker.register('sw.js')
                    .then(() => console.log('MasjidTV PWA SW registered successfully'))
                    .catch((err) => console.log('SW reg error:', err));
            });
        }

        // PWA Install Prompt Handler
        let deferredPrompt;
        const pwaHeaderBtn = document.getElementById('pwaInstallHeaderBtn');

        window.addEventListener('beforeinstallprompt', (e) => {
            e.preventDefault();
            deferredPrompt = e;
            if (pwaHeaderBtn) {
                pwaHeaderBtn.classList.remove('hidden');
                pwaHeaderBtn.classList.add('inline-flex');
            }
        });

        if (pwaHeaderBtn) {
            pwaHeaderBtn.addEventListener('click', async () => {
                if (deferredPrompt) {
                    deferredPrompt.prompt();
                    const { outcome } = await deferredPrompt.userChoice;
                    deferredPrompt = null;
                    if (pwaHeaderBtn) pwaHeaderBtn.classList.add('hidden');
                }
            });
        }

        // Initialize Map & Initial Load
        document.addEventListener('DOMContentLoaded', () => {
            initMap();
        });
        initMap();
        loadAllData();
        setInterval(() => loadAllData(false), 60000);
    </script>
</body>
</html>
