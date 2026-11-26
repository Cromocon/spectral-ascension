#!/usr/bin/env python3
"""Riassume l'ultimo report dello scenario runner e le righe [SA-SCENARIO] del log del client.

Uso:
  python3 -I tools/check_scenari.py                 # run/client/scenari-report/latest.json + run/client/logs/latest.log
  python3 -I tools/check_scenari.py REPORT.json     # un report specifico
  python3 -I tools/check_scenari.py --all           # elenca anche i passi riusciti
Esce con codice 1 se un passo è fallito, se il report è incompleto o se manca.
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RUN = os.path.join(ROOT, "run", "client")


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    show_all = "--all" in sys.argv[1:]
    path = args[0] if args else os.path.join(RUN, "scenari-report", "latest.json")
    if not os.path.isfile(path):
        print(f"Report non trovato: {path}")
        return 1
    with open(path, encoding="utf-8") as f:
        rep = json.load(f)

    bad = False
    s = rep.get("summary", {})
    print(f"Report: {rep.get('file', path)}")
    print(f"Modo: {rep.get('mode')}  inizio: {rep.get('started')}  completo: {rep.get('complete')}")
    if rep.get("error"):
        print(f"ERRORE: {rep['error']}")
        bad = True
    print(f"Scenari OK {s.get('scenarios_ok', '?')}/{s.get('scenarios', '?')}  passi OK {s.get('steps_ok', '?')}, FAIL {s.get('steps_fail', '?')}")
    if not rep.get("complete"):
        print("ATTENZIONE: report incompleto (gioco chiuso o interrotto prima della fine)")
        bad = True

    for sc in rep.get("scenarios", []):
        ok = sc.get("ok")
        bad |= not ok
        print()
        print(f"== {sc.get('name')}: {'OK' if ok else 'FAIL'}  "
              f"({sc.get('steps_ok', 0)} OK, {sc.get('steps_fail', 0)} FAIL, {sc.get('steps_skipped', 0)} saltati)")
        if sc.get("error"):
            print(f"   errore: {sc['error']}")
        if sc.get("aborted"):
            print(f"   interrotto: {sc['aborted']}")
        for st in sc.get("steps", []):
            if st.get("ok") and not show_all:
                continue
            mark = "OK  " if st.get("ok") else "FAIL"
            print(f"   #{st.get('n'):>2} {mark} {st.get('action')}: {st.get('detail', '')[:400]}")
        shots = [p for st in sc.get("steps", []) for p in st.get("screenshots", [])]
        if shots:
            print(f"   screenshot ({len(shots)}):")
            for p in shots:
                exists = "" if os.path.isfile(p) else "  [MANCA]"
                print(f"     {p}{exists}")

    log = os.path.join(RUN, "logs", "latest.log")
    if os.path.isfile(log):
        with open(log, encoding="utf-8", errors="replace") as f:
            lines = f.read().splitlines()
        tagged = [l for l in lines if "[SA-SCENARIO]" in l]
        errors = [l for l in lines if re.search(r"/(ERROR|FATAL)\]", l) and re.search(r"spectral_ascension|SA-SCENARIO|cromocon", l)]
        print()
        print(f"== Log {log}: {len(tagged)} righe [SA-SCENARIO], {len(errors)} errori che citano la mod")
        for l in tagged:
            if " FAIL " in l or "STOP" in l or "LOAD FAIL" in l or "errore" in l:
                print("   " + l[l.index("[SA-SCENARIO]"):][:300])
        for l in errors[:20]:
            print("   " + l[:300])
        bad |= bool(errors)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
