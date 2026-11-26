"""Confronta le risposte del test alla cieca con la chiave.

Uso: python3 -I tools/test_cieco/confronta.py <cartella_chiave> [<cartella_chiave_giro_precedente>]
Ogni cartella contiene chiave.json e risp_*.txt (righe "img_NNN | nome | sicurezza | seconda").
"""
import glob, json, os, sys
from collections import defaultdict


def load(d):
    key = {k["img"][:-4]: k for k in json.load(open(os.path.join(d, "chiave.json")))}
    ans = {}
    for f in glob.glob(os.path.join(d, "risp_*.txt")):
        for l in open(f):
            p = [x.strip() for x in l.split("|")]
            if p[0].startswith("img_"):
                ans[p[0]] = p[1:]
    return {os.path.basename(v["file"]): (v, ans.get(k, ["(manca)", "", ""])) for k, v in key.items()}


def fam(n):
    w = n.split()[0] if n else n
    return w if w in ("balestra", "arco", "canna") else n


g = load(sys.argv[1])
prev = load(sys.argv[2]) if len(sys.argv) > 2 else {}
st = defaultdict(lambda: [0, 0, 0])
for f, (v, a) in g.items():
    k = v["gruppo"] + (" ov" if "oversized" in f else "")
    st[k][0] += 1
    st[k][1] += a[0] == v["atteso"]
    st[k][2] += fam(a[0]) == fam(v["atteso"])
print("gruppo | totale | esatti | oggetto giusto")
for k, s in sorted(st.items()):
    print(k, "|", s[0], "|", s[1], "|", s[2])
print("\nERRORI DI OGGETTO")
for f in sorted(g):
    v, a = g[f]
    if fam(a[0]) != fam(v["atteso"]):
        p = prev.get(f, (None, ["-"]))[1][0]
        print(f"{f} | atteso {v['atteso']} | risposta {a[0]} ({a[1]}) | giro prima: {p}")
print("\nERRORI DI STATO")
for f in sorted(g):
    v, a = g[f]
    if fam(a[0]) == fam(v["atteso"]) and a[0] != v["atteso"]:
        print(f"{f} | atteso {v['atteso']} | risposta {a[0]}")
