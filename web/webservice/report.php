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

// Auto-map city and province from cities_mapping.json
$city = trim($data['city'] ?? '');
$prov = trim($data['province'] ?? '');
$lat = isset($data['lat']) ? (float)$data['lat'] : -6.2;
$lng = isset($data['lng']) ? (float)$data['lng'] : 106.8;

$mappingFile = __DIR__ . '/../dashboard/cities_mapping.json';
if (file_exists($mappingFile)) {
    $map = @json_decode(file_get_contents($mappingFile), true);
    if ($map) {
        if (empty($prov)) {
            if (!empty($map['by_id'][$city])) {
                $prov = $map['by_id'][$city]['province'] ?? '';
                $city = $map['by_id'][$city]['name'] ?? $city;
            } else {
                $upper = strtoupper($city);
                if (!empty($map['by_name'][$upper])) {
                    $prov = $map['by_name'][$upper];
                } else {
                    $clean = strtoupper(preg_replace('/^(KAB\.|KABUPATEN|KOTA)\s+/i', '', $city));
                    if (!empty($map['by_name'][$clean])) {
                        $prov = $map['by_name'][$clean];
                    }
                }
            }
        }
        
        // If coordinates are default/missing and province known, use province centroid
        if ((abs($lat - (-6.2)) < 0.001 && abs($lng - 106.8) < 0.001) && !empty($prov) && !empty($map['prov_coords'][$prov])) {
            $base = $map['prov_coords'][$prov];
            $h = crc32($id);
            $lat = $base[0] + ((($h % 40) - 20) * 0.005);
            $lng = $base[1] + ((((int)($h / 40) % 40) - 20) * 0.005);
        }
    }
}

$data['city'] = $city;
$data['province'] = $prov;
$data['lat'] = $lat;
$data['lng'] = $lng;

try {
    $pdo = new PDO('mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4', DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION
    ]);
    
    $now = date('Y-m-d H:i:s');
    
    $stmt = $pdo->prepare("INSERT INTO devices (device_id, lat, lng, city, province, mosque, payload, last_seen) VALUES (?,?,?,?,?,?,?,?) 
        ON DUPLICATE KEY UPDATE lat=?, lng=?, city=?, province=?, mosque=?, payload=?, last_seen=?");
    $payload = json_encode($data);
    $stmt->execute([
        $id, $lat, $lng, $city, $prov, $data['mosque'] ?? '', $payload, $now,
        $lat, $lng, $city, $prov, $data['mosque'] ?? '', $payload, $now
    ]);
    
    $stmt2 = $pdo->prepare("INSERT INTO device_history (device_id, lat, lng, city, province, mosque, kas_balance, payload) VALUES (?,?,?,?,?,?,?,?)");
    $kasBal = isset($data['kas']['balance']) ? (int)$data['kas']['balance'] : 0;
    $stmt2->execute([$id, $lat, $lng, $city, $prov, $data['mosque'] ?? '', $kasBal, $payload]);
    
    echo json_encode(['status' => 'ok']);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['status' => 'error', 'msg' => $e->getMessage()]);
}
