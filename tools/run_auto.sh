#!/usr/bin/env bash
# Lancia ./gradlew runClientAuto dentro un compositor KDE virtuale (nessuna finestra sul desktop dell'utente).
# Uso: tools/run_auto.sh [--scenario NOMI] [--timeout SECONDI] [--window] [--size LxA]
#   --scenario A,B  scenari da eseguire (default: all = tutti); il gioco si chiude da solo alla fine
#   --timeout N   chiude il gioco dopo N secondi (default 600; 0 = nessun limite)
#   --jei         JEI + SpectrumJEI senza EMI (-PjeiOnly), come nell'istanza ATM10
#   --window      ripiego: niente compositor virtuale, finestra normale sul desktop
#   --size 1600x900  (schermo virtuale; la finestra del gioco è 1280x720)
# Log del gioco: run/client/logs/latest.log   Output gradle: run/client/auto.out
# Report scenari: run/client/scenari-report/latest.json (riassunto: python3 -I tools/check_scenari.py)
set -u
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
TIMEOUT=${SA_TIMEOUT:-600}; WINDOW=0; SIZE=1600x900; INNER=${SA_INNER:-0}; SCENARIO=${SA_SCENARIO:-all}; JEI=${SA_JEI:-0}
while [ $# -gt 0 ]; do
  case "$1" in
    --timeout) TIMEOUT="$2"; shift 2;;
    --scenario) SCENARIO="$2"; shift 2;;
    --window) WINDOW=1; shift;;
    --size) SIZE="$2"; shift 2;;
    --jei) JEI=1; shift;;       # JEI + SpectrumJEI senza EMI (come ATM10): -PjeiOnly
    --inner) INNER=1; shift;;
    *) echo "argomento sconosciuto: $1" >&2; exit 2;;
  esac
done
JEIARG=""; [ "$JEI" = 1 ] && JEIARG="-PjeiOnly"
OUT="$ROOT/run/client/auto.out"
mkdir -p "$ROOT/run/client"

kill_game() {
  # il client gira in un JVM figlio del daemon gradle: lo si riconosce dalla system property
  pkill -f 'fml.modFolders=spectral_ascension' 2>/dev/null
  sleep 3
  pkill -9 -f 'fml.modFolders=spectral_ascension' 2>/dev/null
  true
}

run_game() {
  # crea il mondo se manca (lo script è git-ignored: in un clone pulito non c'è)
  if [ -f tools/reset_test_world.py ]; then python3 -I tools/reset_test_world.py >/dev/null || return 1; fi
  local rc
  if [ "$TIMEOUT" -gt 0 ]; then
    timeout --foreground "$TIMEOUT" ./gradlew runClientAuto -PsaScenario="$SCENARIO" $JEIARG >"$OUT" 2>&1; rc=$?
  else
    ./gradlew runClientAuto -PsaScenario="$SCENARIO" $JEIARG >"$OUT" 2>&1; rc=$?
  fi
  [ $rc -eq 124 ] && echo "run_auto: timeout di ${TIMEOUT}s raggiunto, chiudo il gioco." >&2
  kill_game
  return $rc
}

if [ $INNER -eq 1 ]; then   # eseguito dentro il compositor virtuale
  echo "run_auto: WAYLAND_DISPLAY=${WAYLAND_DISPLAY:-} DISPLAY=${DISPLAY:-}" >&2
  run_game; rc=$?
  echo "$rc" >"${SA_DONE:-/dev/null}"   # avvisa lo script esterno: il compositor non esce da solo
  exit $rc
fi

if [ $WINDOW -eq 1 ]; then
  trap kill_game EXIT INT TERM
  run_game; exit $?
fi

W="${SIZE%x*}"; H="${SIZE#*x}"
# Il compositor esce quando termina lo script di sessione (--inner); socket proprio, non tocca wayland-1.
export SA_JEI="$JEI" SA_INNER=1 SA_TIMEOUT="$TIMEOUT" SA_SCENARIO="$SCENARIO"   # kwin accetta solo un percorso: i parametri passano per ambiente
export ALSOFT_DRIVERS=null   # niente audio: il client automatico non deve suonare sulle casse dell'utente (OpenAL Soft con driver nullo)
# kwin spezza i percorsi con spazi: si passa un wrapper temporaneo con percorso senza spazi.
WRAP="$(mktemp /tmp/sa-inner-XXXXXX.sh)"
export SA_DONE="$WRAP.done"; rm -f "$SA_DONE"
printf '#!/usr/bin/env bash\nexec "%s"\n' "$ROOT/tools/run_auto.sh" >"$WRAP"; chmod +x "$WRAP"
setsid kwin_wayland --virtual --width "$W" --height "$H" --socket sa-virtual --xwayland \
  "$WRAP" &
KPID=$!
trap 'rm -f "${WRAP:-}" "${SA_DONE:-}"; kill_game; kill $KPID 2>/dev/null; pkill -f "kwin_wayland --virtual --width $W --height $H --socket sa-virtual" 2>/dev/null' EXIT INT TERM
while [ ! -s "$SA_DONE" ] && kill -0 $KPID 2>/dev/null; do sleep 1; done
rc=$(cat "$SA_DONE" 2>/dev/null || echo 1)
rm -f "$SA_DONE"
kill_game
exit "$rc"
