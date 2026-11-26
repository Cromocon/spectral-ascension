"""Trova i jar di Spectrum e Allthemodium usati da gen_data.py e validate.py.

Ordine di ricerca:
  1. variabili d'ambiente SA_SPECTRUM_JAR e SA_ATM_JAR (percorso completo del jar);
  2. variabile SA_MODS_DIR: cartella mods di un'istanza (es. quella di CurseForge), dove cerca
     spectrum-*.jar e allthemodium-*.jar;
  3. cache di Gradle (~/.gradle/caches/modules-2/files-2.1, o $GRADLE_USER_HOME): Gradle scarica già
     questi jar per compilare la mod, basta aver lanciato almeno una volta ./gradlew build.
Se non trova nulla, esce con un errore che spiega cosa impostare.

Uso: from sa_paths import spectrum_jar, atm_jar   (dopo sys.path.insert della cartella tools)
"""
import glob
import os
import sys


def _gradle_cache():
    home = os.environ.get("GRADLE_USER_HOME") or os.path.join(os.path.expanduser("~"), ".gradle")
    return os.path.join(home, "caches", "modules-2", "files-2.1")


def _find(env_jar, mods_prefix, gradle_pattern, nome):
    p = os.environ.get(env_jar)
    if p:
        if not os.path.isfile(p):
            sys.exit(f"{env_jar}={p} non è un file")
        return p
    mods = os.environ.get("SA_MODS_DIR")
    if mods:
        trovati = sorted(glob.glob(os.path.join(mods, mods_prefix + "*.jar")))
        if not trovati:
            sys.exit(f"SA_MODS_DIR={mods}: nessun {mods_prefix}*.jar")
        return trovati[-1]
    trovati = sorted(glob.glob(os.path.join(_gradle_cache(), gradle_pattern)))
    if trovati:
        return trovati[-1]
    sys.exit(f"jar di {nome} non trovato.\n"
             f"  - lancia una volta './gradlew build' (Gradle lo scarica nella sua cache), oppure\n"
             f"  - imposta {env_jar}=/percorso/del/jar, oppure\n"
             f"  - imposta SA_MODS_DIR=/percorso/della/cartella/mods della tua istanza (es. CurseForge).")


def spectrum_jar():
    return _find("SA_SPECTRUM_JAR", "spectrum-",
                 "maven.modrinth/spectrum/*/*/spectrum-*.jar", "Spectrum")


def atm_jar():
    return _find("SA_ATM_JAR", "allthemodium-",
                 "curse.maven/allthemodium-*/*/*/allthemodium-*.jar", "Allthemodium")
