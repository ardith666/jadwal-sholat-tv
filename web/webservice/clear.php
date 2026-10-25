<?php
require_once 'config.php';
header('Content-Type: application/json');
$token = $_SERVER['HTTP_X_API_TOKEN'] ?? '';
if ($token !== API_TOKEN) { http_response_code(401); echo json_encode(['status'=>'unauthorized']); exit; }
$file = __DIR__ . '/devices.json';
if (file_exists($file)) @unlink($file);
echo json_encode(['status'=>'ok']);
