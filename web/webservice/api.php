<?php
require_once 'config.php';
session_start();
if (!isset($_SESSION['logged_in'])) { http_response_code(401); echo json_encode(['error'=>'unauth']); exit; }

header('Content-Type: application/json');

$pdo = new PDO('mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4', DB_USER, DB_PASS, [
    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION
]);

$action = $_GET['action'] ?? '';

if ($action === 'devices') {
    $stmt = $pdo->query("SELECT device_id, lat, lng, city, province, mosque, payload, last_seen, created_at, updated_at FROM devices ORDER BY last_seen DESC");
    $res = [];
    while($r = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $r['payload'] = json_decode($r['payload'], true);
        $res[] = $r;
    }
    echo json_encode($res);
    exit;
}

if ($action === 'history') {
    $stmt = $pdo->query("SELECT id, device_id, city, province, mosque, kas_balance, created_at FROM device_history ORDER BY id DESC LIMIT 500");
    echo json_encode($stmt->fetchAll(PDO::FETCH_ASSOC));
    exit;
}

if ($action === 'stats') {
    $total = $pdo->query("SELECT COUNT(*) c FROM devices")->fetch()['c'];
    $cash = $pdo->query("SELECT COALESCE(SUM(kas_balance),0) s FROM devices d JOIN (SELECT device_id, kas_balance FROM device_history dh1 WHERE id=(SELECT MAX(id) FROM device_history dh2 WHERE dh2.device_id=dh1.device_id)) x ON d.device_id=x.device_id")->fetch()['s'];
    $active = $pdo->query("SELECT COUNT(*) c FROM devices WHERE last_seen >= DATE_SUB(NOW(), INTERVAL 24 HOUR)")->fetch()['c'];
    $prov = $pdo->query("SELECT COUNT(DISTINCT province) c FROM devices WHERE province != ''")->fetch()['c'];
    echo json_encode(['total'=>$total,'cash'=>$cash,'active'=>$active,'prov'=>$prov]);
    exit;
}

echo json_encode(['error'=>'bad_action']);
