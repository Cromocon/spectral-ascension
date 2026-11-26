"""Legge il log del server di test e riporta ciò che riguarda la mod.

Uso: python3 -I tools/check_log.py [run/gametest/logs/latest.log]
Esce con codice 1 se trova errori o test falliti.
"""
import re
import sys

path = sys.argv[1] if len(sys.argv) > 1 else "run/gametest/logs/latest.log"
text = open(path, encoding="utf-8", errors="replace").read()
lines = text.splitlines()

ours = re.compile(r"spectral_ascension|cromocon\.spectralascension", re.I)
problem = re.compile(r"\b(ERROR|FATAL)\b|Exception|Failed to (load|parse)|Couldn't (load|parse)|Unknown|Invalid", re.I)

sections = {
    "Errori che citano la mod": [l for l in lines if ours.search(l) and problem.search(l)],
    "Avvisi [SANITY] di Spectrum sulla mod": [l for l in lines if "[SANITY" in l and ours.search(l)],
    "Esito GameTest": [l for l in lines if re.search(r"GameTest|tests? (passed|failed)|required tests", l)
                       and re.search(r"pass|fail|run|All", l, re.I)],
    "Errori generici (prime 25 righe)": [l for l in lines if re.search(r"\]: ?.*\b(ERROR|FATAL)\b|/ERROR\]|/FATAL\]", l)][:25],
}
bad = bool(sections["Errori che citano la mod"]) or any(re.search(r"fail", l, re.I) and not re.search(r"0 failed|passed", l, re.I)
                                                       for l in sections["Esito GameTest"])
for title, found in sections.items():
    print(f"== {title}: {len(found)}")
    for l in found[:40]:
        print("   ", l[:300])
sys.exit(1 if bad else 0)
