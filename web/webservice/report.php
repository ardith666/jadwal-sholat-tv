<?php
$db = 'devices.json';
$data = json_decode(file_get_contents('php://input'), true);

if ($data && isset($data['id'], $data['lat'], $data['lng'])) {
    $devices = file_exists($db) ? json_decode(file_get_contents($db), true) : [];
    $data['last_seen'] = date('Y-m-d H:i:s');
    $devices[$data['id']] = $data;
    file_put_contents($db, json_encode($devices));
    echo json_encode(['status' => 'ok']);
} else {
    http_response_code(400);
    echo json_encode(['status' => 'error']);
}
