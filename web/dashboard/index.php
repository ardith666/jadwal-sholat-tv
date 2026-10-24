<?php
session_start();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $user = $_POST['username'] ?? '';
    $pass = $_POST['password'] ?? '';
    $captcha = $_POST['captcha'] ?? '';

    if ($user === 'admin' && $pass === 'admin@123' && $captcha === ($_SESSION['captcha'] ?? '')) {
        $_SESSION['logged_in'] = true;
        header('Location: dashboard.php');
        exit;
    }
    $error = "Login failed!";
}

$digits = '';
for ($i = 0; $i < 6; $i++) $digits .= rand(0, 9);
$_SESSION['captcha'] = $digits;

function renderDotted($num) {
    $dots = [
        '0' => "XXX\nX.X\nX.X\nX.X\nXXX",
        '1' => "..X\n..X\n..X\n..X\n..X",
        '2' => "XXX\n..X\nXXX\nX..\nXXX",
        '3' => "XXX\n..X\nXXX\n..X\nXXX",
        '4' => "X.X\nX.X\nXXX\n..X\n..X",
        '5' => "XXX\nX..\nXXX\n..X\nXXX",
        '6' => "XXX\nX..\nXXX\nX.X\nXXX",
        '7' => "XXX\n..X\n..X\n..X\n..X",
        '8' => "XXX\nX.X\nXXX\nX.X\nXXX",
        '9' => "XXX\nX.X\nXXX\n..X\nXXX"
    ];
    $out = explode("\n", $dots[$num]);
    return array_map(fn($l) => str_replace(['X','.'], ['●','·'], $l), $out);
}

$captcha_art = "";
$lines = ["","","","",""];
foreach(str_split($digits) as $d) {
    $art = renderDotted($d);
    for($i=0; $i<5; $i++) $lines[$i] .= $art[$i] . "  ";
}
$captcha_display = implode("\n", $lines);
?>
<!DOCTYPE html>
<html>
<head>
    <title>Login</title>
    <style>
        body { font-family: monospace; background: #121212; color: #0f0; display: flex; justify-content: center; align-items: center; height: 100vh; }
        .login-box { border: 1px solid #0f0; padding: 20px; }
        pre { background: #000; padding: 10px; line-height: 1.2; letter-spacing: 2px; }
        input { background: #000; border: 1px solid #0f0; color: #0f0; padding: 5px; margin: 5px 0; width: 100%; box-sizing: border-box; }
        button { background: #0f0; color: #000; border: none; padding: 10px; width: 100%; cursor: pointer; font-weight: bold; }
    </style>
</head>
<body>
    <div class="login-box">
        <h2>TV DASHBOARD LOGIN</h2>
        <?php if(isset($error)) echo "<p style='color:red'>$error</p>"; ?>
        <form method="POST">
            <input type="text" name="username" placeholder="Username" required>
            <input type="password" name="password" placeholder="Password" required>
            <p>Enter Captcha:</p>
            <pre><?php echo $captcha_display; ?></pre>
            <input type="text" name="captcha" placeholder="6 Digit Captcha" required>
            <button type="submit">LOGIN</button>
        </form>
    </div>
</body>
</html>
