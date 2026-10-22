#!/usr/bin/env python3
"""Helper verifikasi: set konfigurasi slide MULTI_PANEL via Web Admin API TV."""
import json
import sys
import urllib.request

BASE = "http://192.168.3.11:8080"
PIN = "1989"


def get_slides():
    with urllib.request.urlopen(f"{BASE}/api/slides?pin={PIN}", timeout=15) as r:
        return json.load(r)["slides"]


def save(slides):
    body = json.dumps({"pin": PIN, "slides": slides}).encode()
    req = urllib.request.Request(
        f"{BASE}/api/slides/save", data=body, headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req, timeout=15) as r:
        return json.load(r)


def apply(preset, left, right, third, agenda_mode, kas=None, agenda=None, cctv=None):
    slides = get_slides()
    mp = next(s for s in slides if s["id"] == "slide_multi_panel")
    mp.update(
        enabled=True,
        multiPanelPreset=preset,
        slotLeftType=left,
        slotRightType=right,
        slotThirdType=third,
        panelAgendaMode=agenda_mode,
    )
    if kas:
        mp.update(kas)
    if agenda:
        mp["agendaList"] = agenda
    if cctv:
        mp["cctvStreams"] = cctv
    print("save:", save(slides).get("status"))


if __name__ == "__main__":
    mode = sys.argv[1]
    cctv_src = next(
        (s["cctvStreams"] for s in get_slides() if s["type"] == "CCTV" and s["cctvStreams"]), []
    )
    agenda_src = next(
        (s["agendaList"] for s in get_slides() if s["type"] == "ANNOUNCEMENT" and s["agendaList"]), []
    )
    kas_src = next((s for s in get_slides() if s["type"] == "KAS"), {})
    kas_data = {
        "saldoAwal": kas_src.get("saldoAwal", 0),
        "pemasukan": kas_src.get("pemasukan", 0),
        "pengeluaran": kas_src.get("pengeluaran", 0),
        "catatanKas": kas_src.get("catatanKas", ""),
    }

    if mode == "prayer":
        apply("SPLIT_60_40", "PRAYER", "KAS", "KAS", 0, kas=kas_data)
    elif mode == "agenda":
        apply("SPLIT_60_40", "PRAYER", "AGENDA", "KAS", 1, agenda=agenda_src)
    elif mode == "agenda_hero":
        apply("SPLIT_60_40", "PRAYER", "AGENDA", "KAS", 0, agenda=agenda_src)
    elif mode == "kas_agenda":
        apply(
            "THREE_COLUMNS",
            "PRAYER",
            "AGENDA",
            "KAS",
            1,
            kas=kas_data,
            agenda=agenda_src,
        )
    elif mode == "cctv":
        apply("SPLIT_60_40", "PRAYER", "CCTV", "KAS", 0, cctv=cctv_src, kas=kas_data)
    else:
        raise SystemExit("mode tidak dikenal: " + mode)
