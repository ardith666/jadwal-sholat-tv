<?php
require_once 'config.php';

header('Content-Type: application/json');

$raw = file_get_contents('php://input');
$data = json_decode($raw, true);

$token = $_SERVER['HTTP_X_API_TOKEN'] ?? ($_GET['token'] ?? '');
if ($token !== API_TOKEN) {
    http_response_code(401);
    echo json_encode(['status' => 'unauthorized']);
    exit;
}

if (!$data || empty($data['id'])) {
    http_response_code(400);
    echo json_encode(['status' => 'error']);
    exit;
}

$id = $data['id'];

try {
    $pdo = new PDO('mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4', DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION
    ]);
    
    $now = date('Y-m-d H:i:s');
    
    $stmt = $pdo->prepare("INSERT INTO devices (device_id, lat, lng, city, province, mosque, payload, last_seen) VALUES (?,?,?,?,?,?,?,?) 
        ON DUPLICATE KEY UPDATE lat=?, lng=?, city=?, province=?, mosque=?, payload=?, last_seen=?");
    $payload = json_encode($data);
    $stmt->execute([
        $id, $data['lat'] ?? -6.2, $data['lng'] ?? 106.8, $data['city'] ?? '', $data['province'] ?? '', $data['mosque'] ?? '', $payload, $now,
        $data['lat'] ?? -6.2, $data['lng'] ?? 106.8, $data['city'] ?? '', $data['province'] ?? '', $data['mosque'] ?? '', $payload, $now
    ]);
    
    $stmt2 = $pdo->prepare("INSERT INTO device_history (device_id, lat, lng, city, province, mosque, kas_balance, payload) VALUES (?,?,?,?,?,?,?,?)");
    $kasBal = isset($data['kas']['balance']) ? (int)$data['kas']['balance'] : 0;
    $stmt2->execute([$id, $data['lat'] ?? -6.2, $data['lng'] ?? 106.8, $data['city'] ?? '', $data['province'] ?? '', $data['mosque'] ?? '', $kasBal, $payload]);
    
    echo json_encode(['status' => 'ok']);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['status' => 'error', 'msg' => $e->getMessage()]);
}
