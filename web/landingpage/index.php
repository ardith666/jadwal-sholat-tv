<?php
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, "https://api.github.com/repos/ardith666/jadwal-sholat-tv/releases/latest");
curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
curl_setopt($ch, CURLOPT_USERAGENT, "MasjidTV-LandingPage");
$res = curl_exec($ch);
curl_close($ch);
$release = json_decode($res, true);
$apkUrl = "";
if (isset($release['assets'])) {
    foreach ($release['assets'] as $asset) {
        if (strpos($asset['name'], '.apk') !== false) {
            $apkUrl = $asset['browser_download_url'];
            break;
        }
    }
}
$version = $release['tag_name'] ?? 'v2.1';
$pageTitle = "Jadwal Sholat TV v" . str_replace('v','',$version) . " - Solusi Digitalisasi Informasi Masjid & Musholla";
$pageDesc = "Aplikasi Android TV gratis untuk menampilkan jadwal sholat akurat 514 kota di Indonesia, ayat Al-Qur'an ambient, dan transparansi laporan kas masjid.";
$protocol = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http");
$host = $_SERVER['HTTP_HOST'] ?? 'localhost';
$reqUri = $_SERVER['REQUEST_URI'] ?? '/';
$actualUrl = "$protocol://$host$reqUri";
$baseDir = rtrim('/' . ltrim(str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'] ?? $reqUri)), '/.'), '/');
$ogImage = "$protocol://$host$baseDir/ogimg.png";
?>
<!DOCTYPE html>
<html lang="id" class="scroll-smooth">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    
    <!-- SEO Meta Tags -->
    <title><?php echo $pageTitle; ?></title>
    <meta name="description" content="<?php echo $pageDesc; ?>">
    <meta name="keywords" content="jadwal sholat tv, aplikasi masjid android tv, display masjid digital, jadwal sholat indonesia, pengumuman kas masjid tv, quran ambient tv, smart tv masjid">
    <meta name="author" content="ardith666">
    <meta name="robots" content="index, follow">
    
    <!-- Open Graph / Facebook -->
    <meta property="og:type" content="website">
    <meta property="og:url" content="<?php echo $actualUrl; ?>">
    <meta property="og:title" content="<?php echo $pageTitle; ?>">
    <meta property="og:description" content="<?php echo $pageDesc; ?>">
    <meta property="og:image" content="<?php echo $ogImage; ?>">

    <!-- Twitter -->
    <meta property="twitter:card" content="summary_large_image">
    <meta property="twitter:url" content="<?php echo $actualUrl; ?>">
    <meta property="twitter:title" content="<?php echo $pageTitle; ?>">
    <meta property="twitter:description" content="<?php echo $pageDesc; ?>">
    <meta property="twitter:image" content="<?php echo $ogImage; ?>">

    <link rel="icon" type="image/png" href="ogimg.png">
    <link rel="apple-touch-icon" href="ogimg.png">
    
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Amiri:wght@400;700&family=Inter:wght@300;400;600;700&display=swap" rel="stylesheet">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/gsap/3.12.2/gsap.min.js"></script>
    
    <style>
        .font-arabic { font-family: 'Amiri', serif; }
        .font-inter { font-family: 'Inter', sans-serif; }
        .hero-bg { 
            position: relative;
            background: linear-gradient(rgba(6, 78, 59, 0.8), rgba(6, 78, 59, 0.9)), 
                        url('https://images.unsplash.com/photo-1590076215667-873d383561a0?q=80&w=2000&auto=format&fit=crop');
            background-size: cover;
            background-position: center;
            background-attachment: fixed;
        }
        .hero-pattern { background-image: url("https://www.transparenttextures.com/patterns/islamic-art.png"); }
        .glitter { position: absolute; pointer-events: none; background: white; border-radius: 50%; opacity: 0; animation: glitter-anim 4s infinite; z-index: 5; }
        @keyframes glitter-anim { 0%, 100% { opacity: 0; transform: scale(0) rotate(0deg); } 50% { opacity: 0.7; transform: scale(1.3) rotate(180deg); } }
        .parallax-target { will-change: transform; }
    </style>
</head>
<body class="font-inter bg-slate-50 text-slate-900 overflow-x-hidden">
    <div id="glitter-container" class="fixed inset-0 pointer-events-none z-50"></div>

    <nav class="fixed top-0 w-full z-40 transition-all duration-300" id="navbar">
        <div class="max-w-7xl mx-auto px-6 py-6 flex justify-between items-center">
            <div class="flex items-center gap-2 group cursor-pointer text-white" id="nav-brand">
                <span class="text-3xl transform group-hover:rotate-12 transition-transform">🕌</span>
                <span class="font-bold text-xl tracking-tight">Jadwal Sholat TV</span>
            </div>
            <div class="hidden md:flex items-center gap-8 text-sm font-medium text-emerald-50" id="nav-links">
                <a href="#fitur" class="hover:text-white transition">Fitur</a>
                <a href="#cara-pasang" class="hover:text-white transition">Panduan</a>
                <a href="#faq" class="hover:text-white transition">FAQ</a>
                <a href="../dashboard/" class="hover:text-white transition inline-flex items-center gap-1.5"><span class="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>Dashboard</a>
                <a href="<?php echo $apkUrl; ?>" class="bg-emerald-500 text-white px-6 py-2.5 rounded-full font-bold hover:bg-emerald-600 shadow-lg shadow-emerald-900/20 transition active:scale-95">Download</a>
            </div>
        </div>
    </nav>

    <header class="hero-bg text-white relative min-h-screen flex items-center overflow-hidden pt-20">
        <div class="absolute inset-0 hero-pattern opacity-10"></div>
        <div class="max-w-7xl mx-auto px-6 grid lg:grid-cols-2 gap-16 items-center relative z-10 py-20">
            <div class="space-y-8 parallax-target" data-speed="0.03">
                <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-400/20 border border-emerald-400/30 text-emerald-300 text-xs font-bold tracking-widest uppercase animate-pulse">
                    <span>Versi <?php echo $version; ?> Tersedia</span>
                </div>
                <h1 class="text-5xl md:text-7xl font-bold leading-[1.1]">Modernisasi <br><span class="text-emerald-400">Dakwah Digital</span> Di Masjid Anda</h1>
                <p class="text-xl text-emerald-50/80 leading-relaxed max-w-xl"><?php echo $pageDesc; ?></p>
                <div class="flex flex-col sm:flex-row gap-5">
                    <a href="<?php echo $apkUrl; ?>" class="group bg-white text-emerald-900 px-8 py-5 rounded-2xl font-bold text-center transition-all shadow-2xl hover:bg-emerald-50 flex items-center justify-center gap-3 active:scale-95">
                        <span class="text-2xl">📥</span>
                        <div class="text-left">
                            <p class="text-[10px] uppercase opacity-60 leading-none mb-1">Unduh Gratis</p>
                            <p class="text-lg leading-none">Download APK</p>
                        </div>
                    </a>
                </div>
            </div>
            <div class="relative parallax-target" data-speed="-0.05">
                <div class="relative z-10 group">
                    <div class="absolute -inset-2 bg-emerald-400/20 rounded-[2rem] blur-xl opacity-50"></div>
                    <div class="relative bg-black/40 rounded-[2rem] p-2 backdrop-blur-xl border border-white/20 shadow-2xl overflow-hidden">
                        <img src="tv-preview.png" alt="Preview Tampilan Jadwal Sholat TV" class="rounded-[1.5rem] shadow-2xl w-full object-cover aspect-video">
                    </div>
                </div>
                <div class="absolute -top-6 -right-6 bg-white p-5 rounded-2xl shadow-2xl text-slate-900 hidden md:flex items-center gap-4 animate-bounce">
                    <div class="w-12 h-12 rounded-full bg-emerald-100 flex items-center justify-center text-2xl text-emerald-600">⚡</div>
                    <div>
                        <p class="text-[10px] uppercase font-bold text-slate-400">Response Speed</p>
                        <p class="text-lg font-bold text-emerald-700">Sangat Ringan</p>
                    </div>
                </div>
            </div>
        </div>
    </header>

    <section id="fitur" class="py-32 bg-white">
        <div class="max-w-7xl mx-auto px-6">
            <div class="grid lg:grid-cols-2 gap-20 items-center">
                <div class="space-y-12">
                    <div class="space-y-4">
                        <h2 class="text-emerald-600 font-bold tracking-widest uppercase text-sm">Fitur Unggulan</h2>
                        <h3 class="text-4xl md:text-5xl font-bold leading-tight">Segala Kebutuhan Masjid Dalam Satu Aplikasi</h3>
                    </div>
                    <div class="space-y-8">
                        <div class="flex gap-6 group">
                            <div class="flex-shrink-0 w-16 h-16 bg-emerald-50 rounded-2xl flex items-center justify-center text-3xl group-hover:bg-emerald-600 group-hover:text-white transition-colors duration-300">🕋</div>
                            <div>
                                <h4 class="text-xl font-bold mb-2">Jadwal Sholat 514 Kota</h4>
                                <p class="text-slate-600 leading-relaxed">Database lengkap seluruh Indonesia sesuai perhitungan resmi Kemenag RI.</p>
                            </div>
                        </div>
                        <div class="flex gap-6 group">
                            <div class="flex-shrink-0 w-16 h-16 bg-emerald-50 rounded-2xl flex items-center justify-center text-3xl group-hover:bg-emerald-600 group-hover:text-white transition-colors duration-300">📖</div>
                            <div>
                                <h4 class="text-xl font-bold mb-2">6.236 Ayat Al-Qur'an Ambient</h4>
                                <p class="text-slate-600 leading-relaxed">Menampilkan ayat-ayat suci secara bergantian dengan animasi halus.</p>
                            </div>
                        </div>
                        <div class="flex gap-6 group">
                            <div class="flex-shrink-0 w-16 h-16 bg-emerald-50 rounded-2xl flex items-center justify-center text-3xl group-hover:bg-emerald-600 group-hover:text-white transition-colors duration-300">💰</div>
                            <div>
                                <h4 class="text-xl font-bold mb-2">Transparansi Laporan Kas</h4>
                                <p class="text-slate-600 leading-relaxed">Tampilkan saldo infaq dan operasional masjid secara otomatis.</p>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="grid grid-cols-2 gap-4">
                    <div class="space-y-4 pt-12">
                        <div class="bg-slate-50 p-6 rounded-3xl space-y-3 shadow-sm hover:shadow-md transition">
                            <p class="text-4xl">📢</p>
                            <h5 class="font-bold">Auto Adzan</h5>
                            <p class="text-xs text-slate-500">Suara adzan berkumandang tepat waktu.</p>
                        </div>
                        <div class="bg-emerald-600 p-6 rounded-3xl space-y-3 text-white shadow-lg">
                            <p class="text-4xl">📺</p>
                            <h5 class="font-bold">D-Pad Ready</h5>
                            <p class="text-xs text-emerald-100">Navigasi remote TV standar.</p>
                        </div>
                    </div>
                    <div class="space-y-4">
                        <div class="bg-emerald-50 p-6 rounded-3xl space-y-3 border border-emerald-100 shadow-sm">
                            <p class="text-4xl">⏳</p>
                            <h5 class="font-bold">Countdown</h5>
                            <p class="text-xs text-slate-500">Hitung mundur adzan & iqomah.</p>
                        </div>
                        <div class="bg-slate-100 p-6 rounded-3xl space-y-3 shadow-sm">
                            <p class="text-4xl">🌙</p>
                            <h5 class="font-bold">Imsak/Terbit</h5>
                            <p class="text-xs text-slate-500">Info lengkap jadwal harian.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- Installation Steps -->
    <section id="cara-pasang" class="py-32 bg-slate-50">
        <div class="max-w-7xl mx-auto px-6 text-center">
            <h3 class="text-3xl font-bold mb-20">Hanya 3 Langkah Untuk Memulai</h3>
            <div class="grid md:grid-cols-3 gap-12 relative">
                <div class="hidden md:block absolute top-10 left-1/4 right-1/4 h-0.5 border-t-2 border-dashed border-emerald-200"></div>
                <div class="relative space-y-6">
                    <div class="w-20 h-20 bg-white rounded-full flex items-center justify-center text-3xl font-bold text-emerald-600 shadow-xl mx-auto z-10 relative">1</div>
                    <h4 class="text-xl font-bold">Download APK</h4>
                    <p class="text-slate-500 text-sm px-6">Unduh file APK terbaru ke Flashdisk, lalu pasang di Android TV Anda.</p>
                </div>
                <div class="relative space-y-6">
                    <div class="w-20 h-20 bg-white rounded-full flex items-center justify-center text-3xl font-bold text-emerald-600 shadow-xl mx-auto z-10 relative">2</div>
                    <h4 class="text-xl font-bold">Pilih Kota</h4>
                    <p class="text-slate-500 text-sm px-6">Buka aplikasi, masuk ke pengaturan, dan pilih kota/kabupaten Anda.</p>
                </div>
                <div class="relative space-y-6">
                    <div class="w-20 h-20 bg-white rounded-full flex items-center justify-center text-3xl font-bold text-emerald-600 shadow-xl mx-auto z-10 relative">3</div>
                    <h4 class="text-xl font-bold">Selesai</h4>
                    <p class="text-slate-500 text-sm px-6">TV Anda otomatis menjadi display informasi profesional Masjid.</p>
                </div>
            </div>
        </div>
    </section>

    <!-- FAQ -->
    <section id="faq" class="py-32 bg-white">
        <div class="max-w-3xl mx-auto px-6">
            <h3 class="text-3xl font-bold text-center mb-16">Pertanyaan Umum (FAQ)</h3>
            <div class="space-y-6">
                <div class="p-6 rounded-2xl border border-slate-100 bg-slate-50">
                    <h5 class="font-bold mb-2">Apakah aplikasi ini benar-benar gratis?</h5>
                    <p class="text-slate-600 text-sm">Ya, 100% gratis. Tanpa biaya langganan, tanpa iklan, dan tanpa fitur yang dikunci. Dibuat murni untuk kemaslahatan masjid.</p>
                </div>
                <div class="p-6 rounded-2xl border border-slate-100 bg-slate-50">
                    <h5 class="font-bold mb-2">Perangkat apa saja yang didukung?</h5>
                    <p class="text-slate-600 text-sm">Semua TV dengan sistem operasi Android TV atau Google TV. Juga mendukung Android TV Box/STB.</p>
                </div>
                <div class="p-6 rounded-2xl border border-slate-100 bg-slate-50">
                    <h5 class="font-bold mb-2">Bagaimana jika waktu sholat berbeda sedikit?</h5>
                    <p class="text-slate-600 text-sm">Anda bisa menggunakan fitur "Koreksi Waktu" di pengaturan untuk menyesuaikan menit tiap waktu sholat.</p>
                </div>
            </div>
        </div>
    </section>

    <section class="hero-bg py-24 text-white text-center overflow-hidden relative">
        <div class="absolute inset-0 hero-pattern opacity-10"></div>
        <div class="max-w-4xl mx-auto px-6 space-y-10 relative z-10">
            <h2 class="text-4xl md:text-5xl font-bold leading-tight font-arabic italic">"Barangsiapa menunjukkan kepada kebaikan, <br>maka baginya pahala seperti pahala pelakunya."</h2>
            <div class="pt-6">
                <a href="<?php echo $apkUrl; ?>" class="bg-white text-emerald-900 px-12 py-6 rounded-3xl font-bold text-2xl transition hover:scale-110 active:scale-95 shadow-2xl inline-block">
                    Download APK Gratis
                </a>
            </div>
            <p class="text-emerald-200">Modernisasi Masjid Anda Sekarang Juga.</p>
        </div>
    </section>

    <footer class="py-12 bg-emerald-950 text-emerald-500 text-center text-xs border-t border-emerald-900">
        <p>&copy; <?php echo date('Y'); ?> Jadwal Sholat TV • Proyek Untuk Ummat.</p>
    </footer>

    <script>
        window.onscroll = function() {
            const nav = document.getElementById('navbar');
            const brand = document.getElementById('nav-brand');
            const links = document.getElementById('nav-links');
            if (window.scrollY > 50) {
                nav.classList.add('bg-white/95', 'backdrop-blur-md', 'shadow-xl', 'py-3');
                nav.classList.remove('py-6');
                brand.classList.replace('text-white', 'text-emerald-900');
                links.classList.replace('text-emerald-50', 'text-slate-600');
            } else {
                nav.classList.remove('bg-white/95', 'backdrop-blur-md', 'shadow-xl', 'py-3');
                nav.classList.add('py-6');
                brand.classList.replace('text-emerald-900', 'text-white');
                links.classList.replace('text-slate-600', 'text-emerald-50');
            }
        };

        document.addEventListener('mousemove', (e) => {
            const targets = document.querySelectorAll('.parallax-target');
            targets.forEach(t => {
                const speed = t.getAttribute('data-speed');
                const x = (window.innerWidth / 2 - e.pageX) * speed;
                const y = (window.innerHeight / 2 - e.pageY) * speed;
                gsap.to(t, { x, y, duration: 1.5, ease: 'power2.out' });
            });
        });

        function createGlitter() {
            const container = document.getElementById('glitter-container');
            for(let i=0; i<60; i++) {
                const g = document.createElement('div');
                g.className = 'glitter';
                const size = Math.random() * 4 + 1 + 'px';
                g.style.width = size; g.style.height = size;
                g.style.left = Math.random() * 100 + '%';
                g.style.top = Math.random() * 100 + '%';
                g.style.animationDelay = Math.random() * 8 + 's';
                g.style.animationDuration = (Math.random() * 3 + 4) + 's';
                container.appendChild(g);
            }
        }
        createGlitter();
    </script>
</body>
</html>
