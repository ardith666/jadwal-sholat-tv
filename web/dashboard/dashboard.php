<?php
session_start();
if (!isset($_SESSION['logged_in'])) { header('Location: index.php'); exit; }
?>
<!DOCTYPE html>
<html lang="id" class="dark">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>MasjidTV • Dashboard</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <script>
    tailwind.config = {
      darkMode: 'class',
      theme: { extend: { colors: { bg:'#0f1026', card:'#15182a', border:'#24263d', accent:'#22c55e' } } }
    }
  </script>
  <link rel="stylesheet" href="https://cdn.datatables.net/1.13.7/css/jquery.dataTables.min.css">
  <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
  <script src="https://cdn.datatables.net/1.13.7/js/jquery.dataTables.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
  <style>
    .dataTables_wrapper .dataTables_filter input,.dataTables_wrapper .dataTables_length select{background:transparent;border:1px solid rgb(36 38 61);border-radius:.5rem;padding:.25rem .5rem}
    table.dataTable{width:100%!important}
  </style>
</head>
<body class="bg-slate-50 text-slate-800 dark:bg-bg dark:text-slate-100 min-h-screen">
  <header class="sticky top-0 z-10 backdrop-blur bg-white/70 dark:bg-card/70 border-b border-slate-200 dark:border-border">
    <div class="mx-auto max-w-7xl px-4 sm:px-6 flex items-center justify-between h-16">
      <div class="flex items-center gap-3">
        <div class="w-2 h-2 rounded-full bg-emerald-500 shadow-[0_0_0_6px_rgba(34,197,94,.08)]"></div>
        <div>
          <p class="text-sm font-semibold">MasjidTV Monitoring</p>
          <p class="text-xs text-slate-500 dark:text-slate-400">Realtime • MySQL</p>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <button id="themeToggle" class="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-border text-sm">Mode</button>
        <a href="logout.php" class="px-3 py-1.5 rounded-lg border border-red-200 text-red-600 dark:border-red-900/60 dark:text-red-400 text-sm">Keluar</a>
      </div>
    </div>
  </header>
  <main class="mx-auto max-w-7xl px-4 sm:px-6 py-6 space-y-6">
    <section class="grid grid-cols-2 md:grid-cols-5 gap-3">
      <div class="rounded-xl border border-slate-200 dark:border-border bg-white dark:bg-card p-4"><p class="text-xs text-slate-500">Perangkat</p><p class="mt-1 text-2xl font-semibold" id="k-total">0</p></div>
      <div class="rounded-xl border border-slate-200 dark:border-border bg-white dark:bg-card p-4"><p class="text-xs text-slate-500">Aktif 24h</p><p class="mt-1 text-2xl font-semibold text-emerald-500" id="k-active">0</p></div>
      <div class="rounded-xl border border-slate-200 dark:border-border bg-white dark:bg-card p-4"><p class="text-xs text-slate-500">Total Kas</p><p class="mt-1 text-2xl font-semibold" id="k-cash">Rp 0</p></div>
      <div class="rounded-xl border border-slate-200 dark:border-border bg-white dark:bg-card p-4"><p class="text-xs text-slate-500">Provinsi</p><p class="mt-1 text-2xl font-semibold" id="k-prov">0</p></div>
      <div class="rounded-xl border border-slate-200 dark:border-border bg-white dark:bg-card p-4"><p class="text-xs text-slate-500">History</p><p class="mt-1 text-2xl font-semibold" id="k-hist">0</p></div>
    </section>
    <section class="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <div class="lg:col-span-1 rounded-xl border border-slate-200 dark:border-border bg-white dark:bg-card overflow-hidden">
        <div class="p-4 border-b border-slate-200 dark:border-border"><p class="text-sm font-medium">Provinsi Terkoneksi</p></div>
        <div class="p-3 h-[280px]"><canvas id="provChart"></canvas></div>
      </div>
      <div class="lg:col-span-2 rounded-xl border border-slate-200 dark:border-border bg-white dark:bg-card overflow-hidden">
        <div class="p-4 border-b border-slate-200 dark:border-border flex items-center justify-between"><p class="text-sm font-medium">Timeline 500 Terbaru</p></div>
        <div class="divide-y divide-slate-100 dark:divide-border max-h-[280px] overflow-auto" id="timeline"></div>
      </div>
    </section>
    <section class="rounded-xl border border-slate-200 dark:border-border bg-white dark:bg-card overflow-hidden">
      <div class="p-4 border-b border-slate-200 dark:border-border flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
        <div><p class="text-sm font-medium">Daftar Perangkat</p><p class="text-xs text-slate-500">Diurutkan berdasarkan last seen</p></div>
      </div>
      <div class="p-3 overflow-x-auto">
        <table id="devTable" class="w-full text-sm">
          <thead><tr class="text-left text-slate-500"><th>Device ID</th><th>Masjid</th><th>Kota</th><th>Provinsi</th><th>Kas</th><th>Terakhir</th><th>Status</th></tr></thead>
          <tbody></tbody>
        </table>
      </div>
    </section>
  </main>
  <script>
    const html=document.documentElement;
    const t=document.getElementById('themeToggle');
    const s=localStorage.getItem('theme')||'dark'; html.classList.toggle('dark',s==='dark'); t.textContent=s==='dark'?'Light':'Dark';
    t.onclick=()=>{ const d=html.classList.toggle('dark'); localStorage.setItem('theme',d?'dark':'light'); t.textContent=d?'Light':'Dark'; };
    function rp(v){return 'Rp '+Number(v||0).toLocaleString('id-ID')}
    async function load(){
      const [d,st,h]=await Promise.all([
        fetch('../webservice/api.php?action=devices&t='+Date.now()).then(r=>r.json()),
        fetch('../webservice/api.php?action=stats&t='+Date.now()).then(r=>r.json()),
        fetch('../webservice/api.php?action=history&t='+Date.now()).then(r=>r.json())
      ]);
      document.getElementById('k-total').textContent=st.total;
      document.getElementById('k-active').textContent=st.active;
      document.getElementById('k-cash').textContent=rp(st.cash);
      document.getElementById('k-prov').textContent=st.prov;
      document.getElementById('k-hist').textContent=h.length;
      const now=Date.now();
      $('#devTable').DataTable({destroy:true,data:d,order:[[5,'desc']],pageLength:10,responsive:true,columns:[
        {data:'device_id',render:x=>x.slice(-10)},
        {data:'mosque',render:x=>x||'-'},
        {data:'city',render:x=>x||'-'},
        {data:'province',render:x=>x||'-'},
        {data:'payload',render:p=>'<span class="text-emerald-500">'+rp(p?.kas?.balance)+'</span>'},
        {data:'last_seen'},
        {render:(_,__,r)=> (now-new Date(r.last_seen)<86400000)?'<span class="inline-flex items-center gap-1 text-emerald-500"><span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>Online</span>':'<span class="text-slate-400">Offline</span>'}
      ]});
      const tl=document.getElementById('timeline'); tl.innerHTML='';
      h.slice(0,40).forEach(x=>{ tl.innerHTML+=`<div class="flex items-center justify-between px-4 py-2.5"><div><p class="text-sm">${x.mosque||x.device_id.slice(-8)} <span class="text-slate-500">• ${x.city||'-'}, ${x.province||'-'}</span></p><p class="text-xs text-slate-500">${x.created_at}</p></div><p class="text-sm text-emerald-500">${rp(x.kas_balance)}</p></div>`;});
      const prov={}; d.forEach(x=>{if(x.province) prov[x.province]=(prov[x.province]||0)+1;});
      new Chart(document.getElementById('provChart'),{type:'doughnut',data:{labels:Object.keys(prov),datasets:[{data:Object.values(prov),backgroundColor:['#22c55e','#38bdf8','#a78bfa','#f59e0b','#f43f5e']}]},options:{plugins:{legend:{position:'bottom',labels:{color:document.documentElement.classList.contains('dark')?'#cbd5f5':'#475569'}}}}});
    }
    load(); setInterval(load,60000);
  </script>
</body>
</html>
