<?php
if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

// Rate Limit Configuration
if (!defined('RATE_LIMIT_FILE')) define('RATE_LIMIT_FILE', __DIR__ . '/.rate_limits.json');
if (!defined('MAX_LOGIN_ATTEMPTS')) define('MAX_LOGIN_ATTEMPTS', 5);
if (!defined('LOCKOUT_TIME_SECONDS')) define('LOCKOUT_TIME_SECONDS', 600); // 10 minutes

if (!function_exists('getClientIP')) {
    function getClientIP() {
        if (!empty($_SERVER['HTTP_CF_CONNECTING_IP'])) return $_SERVER['HTTP_CF_CONNECTING_IP'];
        if (!empty($_SERVER['HTTP_X_FORWARDED_FOR'])) {
            $ips = explode(',', $_SERVER['HTTP_X_FORWARDED_FOR']);
            return trim($ips[0]);
        }
        return $_SERVER['REMOTE_ADDR'] ?? '127.0.0.1';
    }
}

if (!function_exists('getRateLimitData')) {
    function getRateLimitData() {
        if (!file_exists(RATE_LIMIT_FILE)) return [];
        $data = @json_decode(file_get_contents(RATE_LIMIT_FILE), true);
        return is_array($data) ? $data : [];
    }
}

if (!function_exists('saveRateLimitData')) {
    function saveRateLimitData($data) {
        @file_put_contents(RATE_LIMIT_FILE, json_encode($data, JSON_PRETTY_PRINT), LOCK_EX);
    }
}

if (!function_exists('checkRateLimit')) {
    function checkRateLimit($ip) {
        $data = getRateLimitData();
        $now = time();
        
        if (isset($data[$ip])) {
            $entry = $data[$ip];
            // Check if locked
            if (!empty($entry['locked_until']) && $entry['locked_until'] > $now) {
                $remaining = $entry['locked_until'] - $now;
                return [
                    'locked' => true,
                    'remaining_seconds' => $remaining,
                    'attempts' => $entry['attempts'] ?? MAX_LOGIN_ATTEMPTS,
                    'remaining_attempts' => 0
                ];
            }
            
            // Reset if window expired
            if ($now - ($entry['last_attempt'] ?? 0) > LOCKOUT_TIME_SECONDS) {
                unset($data[$ip]);
                saveRateLimitData($data);
                return ['locked' => false, 'remaining_seconds' => 0, 'attempts' => 0, 'remaining_attempts' => MAX_LOGIN_ATTEMPTS];
            }
            
            $attempts = $entry['attempts'] ?? 0;
            $remainingAttempts = max(0, MAX_LOGIN_ATTEMPTS - $attempts);
            return [
                'locked' => false,
                'remaining_seconds' => 0,
                'attempts' => $attempts,
                'remaining_attempts' => $remainingAttempts
            ];
        }
        
        return ['locked' => false, 'remaining_seconds' => 0, 'attempts' => 0, 'remaining_attempts' => MAX_LOGIN_ATTEMPTS];
    }
}

if (!function_exists('recordFailedLogin')) {
    function recordFailedLogin($ip) {
        $data = getRateLimitData();
        $now = time();
        $attempts = isset($data[$ip]['attempts']) ? $data[$ip]['attempts'] + 1 : 1;
        
        $lockedUntil = 0;
        if ($attempts >= MAX_LOGIN_ATTEMPTS) {
            $lockedUntil = $now + LOCKOUT_TIME_SECONDS;
        }
        
        $data[$ip] = [
            'attempts' => $attempts,
            'last_attempt' => $now,
            'locked_until' => $lockedUntil
        ];
        saveRateLimitData($data);
        
        return [
            'locked' => $lockedUntil > 0,
            'remaining_seconds' => $lockedUntil > 0 ? LOCKOUT_TIME_SECONDS : 0,
            'remaining_attempts' => max(0, MAX_LOGIN_ATTEMPTS - $attempts)
        ];
    }
}

if (!function_exists('resetRateLimit')) {
    function resetRateLimit($ip) {
        $data = getRateLimitData();
        if (isset($data[$ip])) {
            unset($data[$ip]);
            saveRateLimitData($data);
        }
    }
}

// Modern SVG Captcha Generator (Pure Digits 0-9)
if (!function_exists('generateCaptchaDigits')) {
    function generateCaptchaDigits($length = 5) {
        $code = '';
        for ($i = 0; $i < $length; $i++) {
            $code .= (string)random_int(0, 9);
        }
        return $code;
    }
}

if (!function_exists('createCaptchaToken')) {
    function createCaptchaToken($code) {
        $secretKey = 'MasjidTV_Secret_Captcha_Salt_v2';
        $time = time();
        $sig = hash_hmac('sha256', (string)$code . '|' . $time, $secretKey);
        return base64_encode($time . '|' . $sig);
    }
}

if (!function_exists('verifyCaptchaToken')) {
    function verifyCaptchaToken($inputCode, $token) {
        if (empty($inputCode) || empty($token)) return false;
        $secretKey = 'MasjidTV_Secret_Captcha_Salt_v2';
        $decoded = @base64_decode($token);
        if (!$decoded || strpos($decoded, '|') === false) return false;
        $parts = explode('|', $decoded, 2);
        if (count($parts) !== 2) return false;
        list($time, $sig) = $parts;
        // 15 minutes validity
        if (abs(time() - (int)$time) > 900) return false;
        $expectedSig = hash_hmac('sha256', (string)$inputCode . '|' . $time, $secretKey);
        return hash_equals($expectedSig, $sig);
    }
}

if (!function_exists('renderCaptchaSvg')) {
    function renderCaptchaSvg($code) {
        $width = 190;
        $height = 54;
        $svg = '<svg xmlns="http://www.w3.org/2000/svg" width="'.$width.'" height="'.$height.'" viewBox="0 0 '.$width.' '.$height.'" class="rounded-xl w-full h-full select-none">';
        $svg .= '<defs>';
        $svg .= '<linearGradient id="cbg" x1="0%" y1="0%" x2="100%" y2="100%">';
        $svg .= '<stop offset="0%" stop-color="#022c22"/>';
        $svg .= '<stop offset="50%" stop-color="#064e3b"/>';
        $svg .= '<stop offset="100%" stop-color="#0f172a"/>';
        $svg .= '</linearGradient>';
        $svg .= '<pattern id="cgrid" width="12" height="12" patternUnits="userSpaceOnUse">';
        $svg .= '<path d="M 12 0 L 0 0 0 12" fill="none" stroke="rgba(16,185,129,0.12)" stroke-width="1"/>';
        $svg .= '</pattern>';
        $svg .= '</defs>';
        $svg .= '<rect width="100%" height="100%" rx="12" fill="url(#cbg)" stroke="#10b981" stroke-width="1.2" stroke-opacity="0.4"/>';
        $svg .= '<rect width="100%" height="100%" rx="12" fill="url(#cgrid)"/>';
        
        // Gentle wave accent lines
        $svg .= '<path d="M 10 34 Q 95 12 180 34" fill="none" stroke="rgba(52,211,153,0.3)" stroke-width="1.5"/>';
        $svg .= '<path d="M 10 22 Q 95 44 180 22" fill="none" stroke="rgba(250,204,21,0.25)" stroke-width="1.5"/>';
        
        // High-contrast clean upright colored digits
        $colors = ['#34d399', '#facc15', '#38bdf8', '#4ade80', '#fbbf24'];
        $chars = str_split($code);
        $count = count($chars);
        $step = ($width - 40) / $count;
        
        foreach ($chars as $idx => $char) {
            $x = 22 + ($idx * $step);
            $y = 38;
            $col = $colors[$idx % count($colors)];
            $svg .= '<text x="'.$x.'" y="'.$y.'" font-family="ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace" font-size="32" font-weight="900" fill="'.$col.'" text-anchor="middle" filter="drop-shadow(0px 2px 4px rgba(0,0,0,0.6))">'.$char.'</text>';
        }
        
        $svg .= '</svg>';
        return $svg;
    }
}

// Ajax Reload Captcha Endpoint
if (isset($_GET['action']) && $_GET['action'] === 'reload_captcha') {
    header('Content-Type: application/json');
    header('Cache-Control: no-cache, no-store, must-revalidate');
    $newCode = generateCaptchaDigits(5);
    $_SESSION['captcha'] = $newCode;
    $newToken = createCaptchaToken($newCode);
    echo json_encode([
        'status' => 'ok',
        'svg' => renderCaptchaSvg($newCode),
        'token' => $newToken
    ]);
    exit;
}

// Captcha Image Endpoint (Fallback if accessed directly)
if (isset($_GET['captcha']) && $_GET['captcha'] === 'render') {
    header('Content-Type: image/svg+xml');
    header('Cache-Control: no-cache, no-store, must-revalidate');
    if (empty($_SESSION['captcha'])) {
        $_SESSION['captcha'] = generateCaptchaDigits(5);
    }
    echo renderCaptchaSvg($_SESSION['captcha']);
    exit;
}

// Redirect if already logged in
if (isset($_SESSION['logged_in']) && $_SESSION['logged_in'] === true) {
    header('Location: dashboard.php');
    exit;
}

$clientIp = getClientIP();
$rateLimitStatus = checkRateLimit($clientIp);
$error = '';
$isLocked = $rateLimitStatus['locked'];
$lockRemainingSecs = $rateLimitStatus['remaining_seconds'];

// Handle POST Login
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST') {
    // Re-check rate limit on submit
    if ($isLocked) {
        $mins = ceil($lockRemainingSecs / 60);
        $error = "Terlalu banyak percobaan gagal! Akses ditolak sementara. Silakan tunggu $mins menit lagi.";
    } else {
        $user = trim($_POST['username'] ?? '');
        $pass = trim($_POST['password'] ?? '');
        $captchaInput = preg_replace('/\D/', '', trim($_POST['captcha'] ?? ''));
        $captchaToken = $_POST['captcha_token'] ?? '';
        $expectedCaptcha = preg_replace('/\D/', '', strval($_SESSION['captcha'] ?? ''));

        // Dual Verification: Via HMAC signed token OR via PHP Session
        $isCaptchaValid = false;
        if ($captchaInput !== '') {
            if (verifyCaptchaToken($captchaInput, $captchaToken)) {
                $isCaptchaValid = true;
            } elseif ($expectedCaptcha !== '' && $captchaInput === $expectedCaptcha) {
                $isCaptchaValid = true;
            }
        }

        // Verify Captcha Digits
        if (!$isCaptchaValid) {
            $error = "Kode angka captcha tidak sesuai! Masukkan 5 digit angka yang tertera di gambar.";
            // Generate fresh captcha
            $captchaCode = generateCaptchaDigits(5);
            $_SESSION['captcha'] = $captchaCode;
            $captchaToken = createCaptchaToken($captchaCode);
        } else {
            // Verify Credentials
            if ($user === 'admin' && $pass === 'admin@123') {
                resetRateLimit($clientIp);
                $_SESSION['logged_in'] = true;
                $_SESSION['user'] = 'admin';
                $_SESSION['login_time'] = time();
                header('Location: dashboard.php');
                exit;
            } else {
                $failedInfo = recordFailedLogin($clientIp);
                if ($failedInfo['locked']) {
                    $isLocked = true;
                    $lockRemainingSecs = $failedInfo['remaining_seconds'];
                    $mins = ceil($lockRemainingSecs / 60);
                    $error = "Login gagal 5 kali berturut-turut! Akun dikunci sementara selama $mins menit demi keamanan.";
                } else {
                    $sisa = $failedInfo['remaining_attempts'];
                    $error = "Username atau Password salah! (Percobaan tersisa: $sisa kali)";
                }
                // Generate fresh captcha
                $captchaCode = generateCaptchaDigits(5);
                $_SESSION['captcha'] = $captchaCode;
                $captchaToken = createCaptchaToken($captchaCode);
            }
        }
    }
}

// Ensure captcha exists for initial or current render
if (empty($_SESSION['captcha']) || empty($captchaCode)) {
    $captchaCode = generateCaptchaDigits(5);
    $_SESSION['captcha'] = $captchaCode;
} else {
    $captchaCode = $_SESSION['captcha'];
}

$captchaToken = createCaptchaToken($captchaCode);
$captchaSvgHtml = renderCaptchaSvg($captchaCode);
?>
<!DOCTYPE html>
<html lang="id" class="h-full">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Login • Jadwal Sholat TV Dashboard</title>
    
    <!-- PWA Settings -->
    <link rel="manifest" href="manifest.json">
    <meta name="theme-color" content="#064e3b">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <meta name="apple-mobile-web-app-title" content="MasjidTV">
    <link rel="apple-touch-icon" href="icons/apple-touch-icon.png">
    <link rel="icon" type="image/svg+xml" href="icons/icon.svg">
    <link rel="icon" type="image/png" href="icons/icon-192.png">

    <!-- Fonts & Tailwind CSS -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=Amiri:wght@400;700&display=swap" rel="stylesheet">
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    fontFamily: {
                        sans: ['Inter', 'sans-serif'],
                        arabic: ['Amiri', 'serif'],
                    },
                    colors: {
                        brand: {
                            50: '#ecfdf5',
                            100: '#d1fae5',
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
    <style>
        .hero-pattern {
            background-image: radial-gradient(rgba(16, 185, 129, 0.12) 1px, transparent 1px);
            background-size: 24px 24px;
        }
        .glow-card {
            box-shadow: 0 0 60px -15px rgba(16, 185, 129, 0.25);
        }
        @keyframes pulse-ring {
            0% { transform: scale(0.95); opacity: 0.8; }
            50% { transform: scale(1.15); opacity: 0.3; }
            100% { transform: scale(0.95); opacity: 0.8; }
        }
        .pulse-effect {
            animation: pulse-ring 3s infinite ease-in-out;
        }
    </style>
</head>
<body class="bg-slate-950 text-slate-100 min-h-screen flex flex-col justify-between hero-pattern antialiased selection:bg-emerald-500 selection:text-white">

    <!-- Ambient glowing backgrounds -->
    <div class="fixed inset-0 pointer-events-none overflow-hidden z-0">
        <div class="absolute -top-40 -left-40 w-96 h-96 bg-emerald-600/20 rounded-full blur-3xl"></div>
        <div class="absolute top-1/2 -right-40 w-96 h-96 bg-teal-600/15 rounded-full blur-3xl"></div>
        <div class="absolute -bottom-40 left-1/3 w-96 h-96 bg-emerald-800/20 rounded-full blur-3xl"></div>
    </div>

    <!-- Top Navigation / Back Link -->
    <header class="relative z-10 w-full max-w-7xl mx-auto px-4 sm:px-6 pt-6 flex justify-between items-center">
        <a href="../landingpage/" class="inline-flex items-center gap-2 text-xs sm:text-sm font-medium text-emerald-400/80 hover:text-emerald-300 transition-colors bg-slate-900/60 backdrop-blur-md px-3.5 py-1.5 rounded-full border border-emerald-500/20 hover:border-emerald-500/40">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
            <span>Landing Page</span>
        </a>

        <div class="inline-flex items-center gap-2 text-xs text-slate-400 bg-slate-900/60 backdrop-blur-md px-3 py-1.5 rounded-full border border-slate-800">
            <span class="w-2 h-2 rounded-full bg-emerald-500 pulse-effect"></span>
            <span>Server Online</span>
        </div>
    </header>

    <!-- Main Content Form -->
    <main class="relative z-10 flex-1 flex items-center justify-center px-4 py-8">
        <div class="w-full max-w-md">
            
            <!-- Card Container -->
            <div class="bg-slate-900/85 backdrop-blur-xl border border-slate-800/80 rounded-3xl p-6 sm:p-8 glow-card transition-all">
                
                <!-- Logo & Brand Header -->
                <div class="text-center mb-8">
                    <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-tr from-brand-900 via-brand-700 to-emerald-500 p-0.5 shadow-lg shadow-emerald-950/50 mb-4">
                        <div class="w-full h-full bg-slate-950 rounded-[14px] flex items-center justify-center text-3xl">
                            🕌
                        </div>
                    </div>
                    <h1 class="text-2xl sm:text-3xl font-extrabold tracking-tight text-white flex items-center justify-center gap-2">
                        Masjid<span class="text-emerald-400">TV</span>
                    </h1>
                    <p class="text-xs sm:text-sm text-slate-400 mt-1">Dashboard Monitoring Perangkat Sholat Digital</p>
                </div>

                <!-- Rate Limit Lockout Banner -->
                <?php if ($isLocked): ?>
                    <div class="mb-6 p-4 rounded-2xl bg-rose-950/60 border border-rose-500/40 text-rose-200 text-sm">
                        <div class="flex items-start gap-3">
                            <span class="text-xl">⛔</span>
                            <div>
                                <p class="font-bold text-rose-300">Akses Terkunci Sementara</p>
                                <p class="text-xs text-rose-200/90 mt-1">
                                    Terlalu banyak percobaan gagal. Silakan tunggu:
                                </p>
                                <div class="mt-2 inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-rose-900/60 border border-rose-500/30 text-rose-200 font-mono font-bold text-sm" id="countdown-badge">
                                    <span id="countdown-timer"><?php echo $lockRemainingSecs; ?></span> detik
                                </div>
                            </div>
                        </div>
                    </div>
                <?php endif; ?>

                <!-- Error Alert -->
                <?php if (!empty($error) && !$isLocked): ?>
                    <div class="mb-6 p-3.5 rounded-2xl bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs sm:text-sm flex items-center gap-3">
                        <svg class="w-5 h-5 flex-shrink-0 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
                        <span><?php echo htmlspecialchars($error); ?></span>
                    </div>
                <?php endif; ?>

                <!-- Form -->
                <form method="POST" action="index.php" class="space-y-5" id="loginForm">
                    <input type="hidden" name="captcha_token" id="captchaToken" value="<?php echo htmlspecialchars($captchaToken ?? ''); ?>">
                    
                    <!-- Username -->
                    <div>
                        <label for="username" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">Username</label>
                        <div class="relative">
                            <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
                            </div>
                            <input 
                                type="text" 
                                id="username" 
                                name="username" 
                                required 
                                autocomplete="username"
                                <?php echo $isLocked ? 'disabled' : ''; ?>
                                value="<?php echo htmlspecialchars($_POST['username'] ?? ''); ?>"
                                class="w-full bg-slate-950/70 border border-slate-700/80 rounded-xl pl-11 pr-4 py-3 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 transition-all disabled:opacity-50"
                                placeholder="Masukkan username admin"
                            >
                        </div>
                    </div>

                    <!-- Password -->
                    <div>
                        <label for="password" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">Password</label>
                        <div class="relative">
                            <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                            </div>
                            <input 
                                type="password" 
                                id="password" 
                                name="password" 
                                required 
                                autocomplete="current-password"
                                <?php echo $isLocked ? 'disabled' : ''; ?>
                                class="w-full bg-slate-950/70 border border-slate-700/80 rounded-xl pl-11 pr-11 py-3 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 transition-all disabled:opacity-50"
                                placeholder="••••••••"
                            >
                            <button 
                                type="button" 
                                id="togglePassword" 
                                class="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-500 hover:text-slate-300 transition"
                                title="Lihat password"
                            >
                                <svg id="eyeOpen" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
                                <svg id="eyeClosed" class="w-5 h-5 hidden" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l18 18"></path></svg>
                            </button>
                        </div>
                    </div>

                    <!-- Modern Captcha Section -->
                    <div>
                        <div class="flex items-center justify-between mb-2">
                            <label for="captcha" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider">Verifikasi Captcha</label>
                            <span class="text-[11px] text-emerald-400 font-medium">Anti-Bot Protection</span>
                        </div>
                        
                        <div class="grid grid-cols-12 gap-3 items-center">
                            <!-- Captcha Visual Box with Refresh -->
                            <div class="col-span-7 relative group">
                                <div id="captchaBox" onclick="reloadCaptcha()" title="Klik untuk ganti angka" class="h-14 rounded-xl overflow-hidden border border-emerald-500/30 bg-slate-950 flex items-center justify-center p-1 shadow-inner relative cursor-pointer transition-transform duration-200 active:scale-95">
                                    <?php echo $captchaSvgHtml; ?>
                                </div>
                                <button 
                                    type="button" 
                                    onclick="reloadCaptcha()" 
                                    title="Ganti angka captcha" 
                                    class="absolute -top-2 -right-2 bg-slate-800 text-emerald-400 p-1.5 rounded-full border border-emerald-500/40 hover:bg-emerald-600 hover:text-white transition shadow-md active:rotate-180"
                                >
                                    <svg id="refreshIcon" class="w-3.5 h-3.5 transition-transform duration-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
                                </button>
                            </div>

                            <!-- Captcha Input -->
                            <div class="col-span-5">
                                <input 
                                    type="text" 
                                    id="captcha" 
                                    name="captcha" 
                                    required 
                                    inputmode="numeric"
                                    pattern="[0-9]*"
                                    maxlength="5"
                                    autocomplete="off"
                                    <?php echo $isLocked ? 'disabled' : ''; ?>
                                    class="w-full bg-slate-950/70 border border-slate-700/80 rounded-xl px-3 py-3 text-center tracking-widest font-mono text-lg font-bold text-emerald-300 placeholder-slate-600 focus:outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 transition disabled:opacity-50"
                                    placeholder="ANGKA"
                                >
                            </div>
                        </div>
                        <p class="text-[11px] text-slate-500 mt-1.5 flex items-center gap-1">
                            <span>💡</span> Klik gambar atau tombol putar jika teks sulit terbaca.
                        </p>
                    </div>

                    <!-- Submit Button -->
                    <div class="pt-2">
                        <button 
                            type="submit" 
                            id="submitBtn"
                            <?php echo $isLocked ? 'disabled' : ''; ?>
                            class="w-full bg-gradient-to-r from-emerald-600 via-emerald-500 to-teal-500 hover:from-emerald-500 hover:to-teal-400 text-slate-950 font-bold py-3.5 px-4 rounded-xl shadow-lg shadow-emerald-950/50 hover:shadow-emerald-900/40 transition-all duration-200 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                        >
                            <svg class="w-5 h-5 text-slate-950" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1"></path></svg>
                            <span>MASUK DASHBOARD</span>
                        </button>
                    </div>

                    <!-- Security & Rate Limit Info Note -->
                    <div class="pt-2 text-center">
                        <p class="text-[11px] text-slate-500">
                            🛡️ Dilengkapi rate limiting maks 5x percobaan & enkripsi sesi.
                        </p>
                    </div>

                </form>

            </div>

            <!-- Install PWA Mobile Suggestion -->
            <div id="pwaInstallPrompt" class="mt-4 p-3 bg-slate-900/60 backdrop-blur-md rounded-2xl border border-emerald-500/20 text-xs text-slate-300 hidden items-center justify-between">
                <div class="flex items-center gap-2">
                    <span class="text-base">📱</span>
                    <span>Pasang di Layar Utama HP?</span>
                </div>
                <button id="pwaInstallBtn" class="bg-emerald-500 hover:bg-emerald-600 text-slate-950 px-3 py-1 rounded-lg font-bold text-xs transition">
                    Pasang
                </button>
            </div>

        </div>
    </main>

    <!-- Footer -->
    <footer class="relative z-10 w-full py-4 text-center text-xs text-slate-600">
        <p>&copy; <?php echo date('Y'); ?> Jadwal Sholat TV • Sistem Monitoring Digital Masjid</p>
    </footer>

    <!-- Scripts -->
    <script>
        // Password toggle
        const toggleBtn = document.getElementById('togglePassword');
        const passInput = document.getElementById('password');
        const eyeOpen = document.getElementById('eyeOpen');
        const eyeClosed = document.getElementById('eyeClosed');

        toggleBtn.addEventListener('click', () => {
            const isPass = passInput.type === 'password';
            passInput.type = isPass ? 'text' : 'password';
            eyeOpen.classList.toggle('hidden', isPass);
            eyeClosed.classList.toggle('hidden', !isPass);
        });

        // Captcha reload via async fetch
        async function reloadCaptcha() {
            const box = document.getElementById('captchaBox');
            const icon = document.getElementById('refreshIcon');
            const tokenInput = document.getElementById('captchaToken');
            if (icon) icon.classList.add('rotate-180');
            try {
                const res = await fetch('index.php?action=reload_captcha&_t=' + Date.now());
                const data = await res.json();
                if (data && data.svg && box) {
                    box.innerHTML = data.svg;
                    if (tokenInput && data.token) {
                        tokenInput.value = data.token;
                    }
                }
            } catch (err) {
                console.error('Failed to reload captcha', err);
            } finally {
                setTimeout(() => {
                    if (icon) icon.classList.remove('rotate-180');
                }, 500);
                const input = document.getElementById('captcha');
                if (input) {
                    input.value = '';
                    input.focus();
                }
            }
        }

        // Lockout countdown timer
        <?php if ($isLocked && $lockRemainingSecs > 0): ?>
        let remaining = <?php echo (int)$lockRemainingSecs; ?>;
        const timerElem = document.getElementById('countdown-timer');
        const submitBtn = document.getElementById('submitBtn');
        
        const interval = setInterval(() => {
            remaining--;
            if (timerElem) timerElem.textContent = remaining;
            if (remaining <= 0) {
                clearInterval(interval);
                location.reload();
            }
        }, 1000);
        <?php endif; ?>

        // PWA Service Worker Registration
        if ('serviceWorker' in navigator) {
            window.addEventListener('load', () => {
                navigator.serviceWorker.register('sw.js')
                    .then((reg) => console.log('MasjidTV PWA SW registered'))
                    .catch((err) => console.log('SW reg error:', err));
            });
        }

        // PWA Install prompt banner
        let deferredPrompt;
        const pwaPrompt = document.getElementById('pwaInstallPrompt');
        const pwaBtn = document.getElementById('pwaInstallBtn');

        window.addEventListener('beforeinstallprompt', (e) => {
            e.preventDefault();
            deferredPrompt = e;
            if (pwaPrompt) pwaPrompt.classList.remove('hidden');
            if (pwaPrompt) pwaPrompt.classList.add('flex');
        });

        if (pwaBtn) {
            pwaBtn.addEventListener('click', async () => {
                if (deferredPrompt) {
                    deferredPrompt.prompt();
                    const { outcome } = await deferredPrompt.userChoice;
                    deferredPrompt = null;
                    if (pwaPrompt) pwaPrompt.classList.add('hidden');
                }
            });
        }
    </script>
</body>
</html>
