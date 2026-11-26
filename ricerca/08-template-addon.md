# 08 — Template di partenza per l'addon (NeoForge 1.21.1)

Il template sta in [`template-addon/`](template-addon/). **Compila [V]**: `gradle build` è riuscito il 2026-11-16 con Gradle 8.14.5 e Java 21. Produce `spectral_ascension-0.1.0-1.21.1-neo.jar`, compilato contro le classi vere di Spectrum 1.12.9 e Fractal.

Anche tutte le dipendenze di runtime si risolvono [V], ma **il gioco non è stato avviato** (`runClient`): è il prossimo passo.

## Concetti in breve

- **Gradle.** È lo strumento di build, come npm + webpack: scarica le dipendenze, compila e produce il jar. Si configura con `build.gradle` (cosa fare), `settings.gradle` (nome e plugin del progetto) e `gradle.properties` (variabili).
- **ModDevGradle (MDG).** È il plugin ufficiale di NeoForge per Gradle. Scarica Minecraft, lo decompila e applica NeoForge, e crea le configurazioni per avviare il gioco dall'IDE (`runClient`, `runServer`). Usiamo la **2.0.140**, la stessa di Spectrum (`src:build.gradle:4`).
- **Parchment.** Dà nomi leggibili ai parametri del codice di Minecraft decompilato: `pLevel` diventa `level`.
- **`neoforge.mods.toml`.** È la "carta d'identità" della mod: id, versione, dipendenze, file dei mixin. Nel template sta in `src/main/templates/` e i `${...}` vengono riempiti da Gradle con i valori di `gradle.properties`.
- **Dipendenze `implementation` / `compileOnly` / `runtimeOnly`.**
  - `implementation`: serve per compilare ed è caricata quando avvii il gioco di test.
  - `compileOnly`: serve solo per compilare.
  - `runtimeOnly`: serve solo per avviare il gioco di test.
  - Nessuna di queste finisce **dentro** il nostro jar, perché il modpack ha già Spectrum, ATM ecc.
- **`DeferredRegister`.** È il modo NeoForge di registrare item, blocchi ecc.: dichiari gli oggetti come "promesse" e NeoForge li crea al momento giusto dell'avvio.
- **Mod bus e game bus.** NeoForge ha due canali di eventi. Il **mod bus** porta quelli di avvio e registrazione: lo ricevi nel costruttore della mod. Il **game bus** (`NeoForge.EVENT_BUS`) porta quelli di gioco: tooltip, tick, creativo di Spectrum.
- **Mixin.** È codice che modifica metodi di Minecraft o di altre mod quando vengono caricati. Ci servirà per il limite degli incantesimi ([03](03-incantesimi-livello-massimo-e-config.md)). Il template ha già il file di configurazione, ancora vuoto.

## Struttura

```
template-addon/
├── settings.gradle            plugin repo NeoForge + resolver del JDK
├── gradle.properties          versioni (allineate all'istanza ATM10) e dati della mod
├── build.gradle               MDG, repository maven, dipendenze, generazione mods.toml
└── src/main/
    ├── java/cromocon/spectralascension/
    │   ├── SpectralAscension.java      classe @Mod: collega registri ed eventi
    │   └── registry/ModItems.java      DeferredRegister + item di prova + sotto-tab di Spectrum
    ├── templates/META-INF/neoforge.mods.toml
    └── resources/
        ├── spectral_ascension.mixins.json
        ├── assets/spectral_ascension/lang/en_us.json
        └── data/spectral_ascension/recipe/pedestal/tier4/   (vuota: qui le ricette Pedestal COMPLEX)
```

Struttura dati che si aggiungerà man mano. I percorsi vengono da [01](01-pedestal-fusion-shrine-ricette.md), [04](04-progressione-e-guidebook.md) e [05](05-ecosistema-addon.md).

| Percorso nel jar | Cosa | Fonte |
| --- | --- | --- |
| `data/spectral_ascension/recipe/pedestal/tier4/*.json` | Set base al Pedestal (`"tier": "complex"`) | 01, 00 §D |
| `data/spectral_ascension/recipe/fusion_shrine/*.json` | Upgrade degli oggetti Spectrum | 01 |
| `data/spectral_ascension/advancement/unlocks/...` | Lucchetti di sblocco (uno per oggetto) | 04, 00 §A |
| `data/spectrum/modonomicon/books/guidebook/entries/<categoria>/*.json` | Voci nel guidebook di Spectrum (namespace `spectrum`, nel nostro jar) | 04 |
| `assets/spectral_ascension/lang/en_us.json` | Testi, incluso `"§oAdded by Spectral Ascension"` | 04 |
| `assets/spectral_ascension/models/item`, `textures/item` | Modelli e texture (`<materiale>_<oggetto>`, `_oversized` 32×32 per gli oggetti grandi) | 02 |
| `data/<ns>/tags/item/...` | Tag (es. aggiungere i nostri item ai tag di Spectrum) | 01, 02 |

## Scelte del template e perché

| Scelta | Valore | Motivo |
| --- | --- | --- |
| NeoForge | `21.1.265`, range `[21.1.249,)` | 265 è la versione dell'istanza [V: `minecraftinstance.json`]; 249 è il minimo che chiede Spectrum |
| Spectrum | `maven.modrinth:spectrum:1.12.9-1.21.1-neo` | Il maven di DaFuqs si ferma a 1.12.5 [V, 06 §2.2] |
| Range Spectrum nel `mods.toml` | `[1.12.9-1.21.1-neo,1.13)` | L'API cambia tra 1.12.9 e 1.13.0 [V, 06 §3]: meglio rifiutare di avviarsi che crashare |
| Dipendenze di Spectrum | Revelationary, Modonomicon, Curios dichiarate a mano | Il POM di Modrinth è vuoto [V] |
| Fractal, Arrowhead | `compileOnly` | Sono già dentro il jar di Spectrum (jar-in-jar); servono solo per compilare (sotto-tab, balestre) |
| Allthemodium | `curse.maven:allthemodium-364466:9143638` + GeckoLib a runtime | La 3.2.1 non è su Modrinth [V, 07] |
| Allthemodium nel `mods.toml` | `required` | Il design usa materiali ATM in ogni ricetta. In alternativa: `optional` + condizioni `neoforge:mod_loaded` nelle ricette (07) |
| `ordering = "AFTER"` su Spectrum | come Spectral Decorations | Carichiamo dopo Spectrum e i nostri file vincono in caso di conflitto [V: `jar(SpectralDecorations):META-INF/neoforge.mods.toml`] |

## Cosa dimostra l'item di prova

`ModItems.TEMPLATE_ITEM` serve solo a verificare che il collegamento con Spectrum funzioni. Va cancellato quando arrivano gli oggetti veri.

- è **indistruttibile** come la bedrock: componente `DataComponents.UNBREAKABLE` + `fireResistant()` (02);
- implementa l'interfaccia di Spectrum **`Preenchanted`** con Sharpness VII (T6 = vanilla+2). Ricorda: gli incantesimi veri li mette la ricetta Fusion/Pedestal, l'interfaccia serve per creativo e tavolo degli incantesimi (02);
- compare nella **sotto-tab Equipment** del creativo di Spectrum, tramite `CreativeSubTabEvent` di Fractal e `ItemGroupIDs.SUBTAB_EQUIPMENT`, come fa Spectrum con le sue compat (`src:compat/gobber/GobberCompat.java:45-50`).

## Come usarlo

1. Copiare `template-addon/` nella cartella del progetto vero, ad esempio la radice di `Spectral Ascension/`, e inizializzare git.
2. Aprirlo in **IntelliJ IDEA** (consigliato per il modding): *Open* sulla cartella; IDEA importa Gradle da solo. Il primo import scarica Minecraft e NeoForge (~1 GB in `~/.gradle`, già fatto una volta durante la verifica).
3. Generare il *Gradle wrapper* (`gradlew`), così chiunque compila con la stessa versione: `gradle wrapper --gradle-version 8.14.5` (oppure da IDEA).
4. `./gradlew runClient` avvia un Minecraft di sviluppo con Spectrum, ATM e la nostra mod. **Non ancora provato**: potrebbe servire aggiungere a runtime altre dipendenze di Spectrum o ATM, che il log di avvio indicherà.
5. Per provarla nel modpack vero: `./gradlew build` → copiare `build/libs/spectral_ascension-*.jar` nella cartella `mods/` dell'istanza CurseForge di ATM10. È l'unica modifica permessa lì. Mai in «ATM10» o sul server.
6. In gioco: `/spectrum sanity spectral_ascension`, poi la [checklist](00-checklist-progressione.md).

## Identità della mod

- **Deciso:** mod id `spectral_ascension` (non si cambia dopo il rilascio: romperebbe i mondi), package `cromocon.spectralascension`, licenza LGPL-3.0 come Spectrum (`mod_license=LGPL-3.0-only`). Va aggiunto il file `LICENSE` con il testo LGPL-3.0 nel repository.
- **Autori** (`mod_authors`), oggi `TODO`. La descrizione dice già "made and maintained by us", come chiesto da DaFuqs.

## Fonti

- `src:build.gradle`, `src:gradle.properties`, `src:gradle/wrapper/gradle-wrapper.properties` (Spectrum al commit `890784ca`).
- `jar(SpectralDecorations):META-INF/neoforge.mods.toml`, `spectral_decorations.mixins.json`.
- `src:api/item/Preenchanted.java`, `src:api/item_group/ItemGroupIDs.java`, `src:compat/gobber/GobberCompat.java`.
- File 05 §2.8 e 06 §2 per repository e coordinate (verificate con richieste HTTP dai colleghi, poi con la build).
- Gradle 8.14.5 da https://services.gradle.org/distributions/ (checksum SHA-256 verificato).
