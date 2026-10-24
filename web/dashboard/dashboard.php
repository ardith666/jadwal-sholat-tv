<?php
session_start();
if (!isset($_SESSION['logged_in'])) { header('Location: index.php'); exit; }
?>
<!DOCTYPE html>
<html>
<head>
    <title>TV Analytics Dashboard</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        body { margin: 0; background: #f0f2f5; font-family: 'Inter', sans-serif; display: flex; flex-direction: column; height: 100vh; color: #1a1a1b; }
        .header { background: #1c1e21; color: white; padding: 12px 24px; display: flex; justify-content: space-between; align-items: center; border-bottom: 3px solid #27ae60; }
        .container { display: grid; grid-template-columns: 1fr 380px; grid-template-rows: auto 1fr; gap: 15px; padding: 15px; flex: 1; overflow: hidden; }
        .stats-grid { grid-column: 1 / 3; display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; }
        .card { background: white; padding: 15px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); border: 1px solid #e1e4e8; }
        .card h4 { margin: 0 0 8px 0; color: #65676b; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; }
        .card .val { font-size: 24px; font-weight: 800; color: #1c1e21; }
        .card .sub { font-size: 11px; color: #8a8d91; margin-top: 4px; }
        #map { border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }
        .sidebar { display: flex; flex-direction: column; gap: 15px; overflow-y: auto; }
        .chart-container { background: white; padding: 15px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }
        .device-item { padding: 12px; border-bottom: 1px solid #f0f2f5; cursor: pointer; transition: background 0.2s; }
        .device-item:hover { background: #f8f9fa; }
        .device-item b { display: block; color: #27ae60; font-size: 14px; }
        .device-item span { font-size: 11px; color: #8a8d91; }
        .kas-info { color: #2ecc71; font-weight: bold; font-size: 13px; margin-top: 4px; }
        .logout { color: #fff; text-decoration: none; font-size: 12px; background: #e74c3c; padding: 6px 12px; border-radius: 6px; }
    </style>
</head>
<body>
    <div class="header">
        <h2 style="margin:0; font-size: 18px;">🕌 TV NETWORK INSIGHTS</h2>
        <a href="logout.php" class="logout">LOGOUT</a>
    </div>
    <div class="container">
        <div class="stats-grid">
            <div class="card"><h4>Total Assets</h4><div class="val" id="stat-total">0</div><div class="sub">Total TV Managed</div></div>
            <div class="card"><h4>Est. Total Liquidity</h4><div class="val" id="stat-cash" style="color: #27ae60;">Rp 0</div><div class="sub">Accumulated Mosque Fund</div></div>
            <div class="card"><h4>Health Score</h4><div class="val" id="stat-health">0%</div><div class="sub">Active nodes vs Total</div></div>
            <div class="card"><h4>Dominant City</h4><div class="val" id="stat-region">-</div><div class="sub">Highest node density</div></div>
        </div>
        <div id="map"></div>
        <div class="sidebar">
            <div class="chart-container">
                <canvas id="cityChart" height="200"></canvas>
            </div>
            <div class="card" style="flex: 1; overflow-y: auto; padding: 0;">
                <h4 style="padding: 15px 15px 5px 15px;">Live Nodes Activity</h4>
                <div id="list"></div>
            </div>
        </div>
    </div>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script>
        const map = L.map('map').setView([-2.5, 118], 5);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);
        let cityChart;

        function formatIDR(val) {
            return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(val);
        }

        async function update() {
            const res = await fetch('../webservice/devices.json?' + Date.now());
            if (!res.ok) return;
            const devices = await res.json();
            const deviceList = Object.values(devices);
            
            const now = new Date();
            let totalCash = 0;
            let activeCount = 0;
            const regions = {};

            const list = document.getElementById('list');
            list.innerHTML = '';
            
            deviceList.forEach(d => {
                const lastSeen = new Date(d.last_seen);
                const isActive = (now - lastSeen) < 86400000;
                if(isActive) activeCount++;
                
                const cashVal = d.kas ? d.kas.balance : 0;
                totalCash += cashVal;
                
                regions[d.city] = (regions[d.city] || 0) + 1;

                // Marker with Rich Popup
                let schedHtml = d.schedule ? d.schedule.map(s => `<li>${s.n}: ${s.t}</li>`).join('') : 'No schedule';
                L.marker([d.lat, d.lng]).addTo(map).bindPopup(`
                    <div style="font-family: sans-serif; min-width: 150px;">
                        <b style="color:#27ae60">${d.mosque || 'Unnamed Mosque'}</b><br>
                        <small>${d.city}</small><hr>
                        <b>Kas:</b> ${formatIDR(cashVal)}<br>
                        <b>Next Prayer:</b><br><ul style="margin:5px 0; padding-left:15px; font-size:11px;">${schedHtml}</ul>
                    </div>
                `);

                list.innerHTML += `
                    <div class="device-item">
                        <b>${d.mosque || 'Unknown Mosque'}</b>
                        <span>${d.city} • Seen: ${d.last_seen}</span>
                        <div class="kas-info">Saldo: ${formatIDR(cashVal)}</div>
                    </div>
                `;
            });

            document.getElementById('stat-total').innerText = deviceList.length;
            document.getElementById('stat-cash').innerText = formatIDR(totalCash);
            document.getElementById('stat-health').innerText = deviceList.length ? Math.round((activeCount/deviceList.length)*100) + '%' : '0%';
            
            const topRegion = Object.keys(regions).reduce((a, b) => regions[a] > regions[b] ? a : b, '-');
            document.getElementById('stat-region').innerText = topRegion;

            const ctx = document.getElementById('cityChart').getContext('2d');
            if(cityChart) cityChart.destroy();
            cityChart = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: Object.keys(regions),
                    datasets: [{ label: 'Nodes', data: Object.values(regions), backgroundColor: '#3498db' }]
                },
                options: { 
                    indexAxis: 'y',
                    plugins: { legend: { display: false }, title: { display: true, text: 'Distribution' } },
                    scales: { x: { beginAtZero: true, grid: { display: false } } }
                }
            });
        }
        update();
        setInterval(update, 60000);
    </script>
</body>
</html>
