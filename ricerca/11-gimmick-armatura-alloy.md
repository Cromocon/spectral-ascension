# 11 - Gimmick di immunità dell'armatura Alloy (basato sull'Ink)

Ricerca di design, 2026-11-16. Obiettivo: dare al **set completo Alloy** un'immunità ai danni quasi totale, paragonabile alla MekaSuit, ma che sembri Spectrum e che si debba guadagnare.

**Decisioni dell'utente già prese** (vincolano tutto il file):

1. L'immunità la dà l'**Ink** di Spectrum. Le proposte sono quindi tutte varianti basate sull'Ink. Le alternative senza Ink restano solo come nota (§6).
2. I **punti armatura dell'addon arrivano al massimo a 50** (set Alloy). Il resto della protezione viene dall'Ink.

Legenda: **[V]** verificato su file o codice letto, **[D]** dedotto, da provare in gioco.
Percorsi abbreviati:
- `S/` = `scratchpad/src/spectrum-1.12.9/src/main/java/de/dafuqs/spectrum/`
- `J/` = `scratchpad/jars/spectrum-1.12.9-1.21.1-neo/`
- `NF/` = sorgenti NeoForge 21.1.265 (estratti da `scratchpad/template-test/build/moddev/artifacts/neoforge-21.1.265-sources.jar`)
- `LANG` = `J/assets/spectrum/lang/en_us.json`

`scratchpad` = cartella temporanea di lavoro (jar estratti, sorgenti clonati).

---

## 0. Concetti di modding usati in questo file

- **Pipeline del danno in NeoForge 1.21.1** [V, `NF/net/minecraft/world/entity/LivingEntity.java`]. Quando un'entità viene colpita, `hurt()` esegue questi passi nell'ordine:
  1. `isInvulnerableTo(source)` (r. 1143). Qui un giocatore in creativa ignora tutto, tranne i danni con il tag `minecraft:bypasses_invulnerability`, cioè `/kill` e il vuoto (`Entity.java:2681-2685`).
  2. **`LivingIncomingDamageEvent`** (r. 1153). Se un listener lo annulla, il colpo **non esiste**: niente danno, niente knockback, niente animazione e **niente frame di invulnerabilità**.
  3. Scudo (r. 1164).
  4. Frame di invulnerabilità (*i-frames*): per 10 tick un colpo che non supera il precedente viene ignorato (r. 1190-1205).
  5. `actuallyHurt()` (r. 1785-1805): riduzione dell'armatura, poi riduzione magica (Protection, Resistance), poi **`LivingDamageEvent.Pre`** (r. 1789), poi i cuori gialli di Absorption (r. 1790-1792), poi `setHealth`, infine `LivingDamageEvent.Post`.
- **Evento**: un metodo con `@SubscribeEvent` che NeoForge chiama in quel punto. La **priorità** (`EventPriority.HIGH/NORMAL/LOW`) decide l'ordine tra le mod che ascoltano lo stesso evento.
- **Damage type e tag**: ogni danno ha un tipo registrato (es. `minecraft:lava`). I **tag** sono gruppi di tipi in JSON, estendibili da qualunque datapack. `#minecraft:bypasses_invulnerability` = danni che ignorano anche la creativa.
- **Attachment**: dato salvato su un'entità, sul giocatore o su un chunk, dichiarato con `AttachmentType` e sincronizzabile col client. Azure Dike è un attachment.
- **Data component**: dato salvato su un `ItemStack` (in 1.21 sostituisce l'NBT). Tutto l'Ink degli item sta nel componente `SpectrumDataComponentTypes.INK_STORAGE` [V, `S/items/trinkets/InkDrainCurioItem.java:77`].
- **Mixin**: modifica il bytecode di un metodo, anche di un'altra mod, quando il gioco si carica. Si usa quando non esiste un evento. È potente ma fragile: se l'autore cambia quel metodo, il mixin si rompe.

---

## 1. Riassunto: cosa offre già Spectrum per difesa e assorbimento

1. **Azure Dike** [V]: barriera blu, salvata nell'attachment `AzureDikeAttachmentType` (`S/attachment_types/AzureDikeAttachmentType.java:24-226`).
   - Assorbe danni in `LivingDamageEvent.Pre`, quindi **dopo armatura e Protection** (`S/registries/SpectrumEventListeners.java:526-534`). Non assorbe i danni con il tag `spectrum:bypasses_dike` (`J/data/spectrum/tags/damage_type/bypasses_dike.json`: `#bypasses_invulnerability`, `#is_drowning`, `#no_impact`, `#bypasses_effects`, `out_of_world`, `generic_kill`).
   - Si ricarica di 1 punto ogni 40 tick, e il conteggio riparte da 200 tick dopo ogni colpo (r. 125-126, 206-218).
   - **Il massimo si ricalcola solo dagli slot Curios** (r. 59-87): un'armatura oggi **non può** dare Dike.
2. **Fonti di Dike** [V]:
   - Ring: 6 punti, ricarica ×2.
   - Belt: 8 punti, recupero dopo un colpo ×2,5.
   - **Shieldgrasp Amulet**: si riempie di **Blue Ink**. Dà `2 + 2·⌊log₈(ink/100)⌋` punti (fino a 12 con `MAX_INK` = 3 276 800) (`S/items/trinkets/AzureDikeAmuletItem.java:17-20, 50-67`; `InkDrainCurioItem.java:26`).
   - **Azuresque Dike Core**: non dà Dike proprio, ma raddoppia quello delle altre fonti (`S/items/trinkets/AzureDikeCoreItem.java`, `maxAzureDikeMultiplier = 2`).
   - Il Dike alimenta anche il Puff Circlet: annulla le cadute per 2 punti e devia i proiettili per 4 (`SpectrumEventListeners.java:502-523, 845-875`; `PuffCircletItem.java:11-12`).
3. **Ink** [V]: energia a 16 colori, 1 Pigment = 100 Ink (`S/registries/SpectrumItems.java:488`).
   - Gli item la prelevano con `InkPowered.tryDrainEnergy`, cercando in mani → Curios → inventario (`S/api/ink/InkPowered.java:132-172`).
   - Pigment Palette: 409 600 Ink **per colore** (`SpectrumItems.java:490`, `IndividualCappedInkStorage`). Ink Flask: 409 600 di un solo colore. Ink Assortment: 6 400 per colore.
4. **Altro** [V]:
   - Bedrock armor: solo Protection V preimpostata, niente gimmick.
   - Gemstone armor: effetti di stato quando vieni colpito (`S/items/armor/GemstoneArmorItem.java:22-50`).
   - Ashen Circlet: immune al fuoco (`SpectrumEventListeners.java:481-494`).
   - Totem Pendant: salva dalla morte in `LivingDeathEvent` (r. 384-409).
   - **Divinity**: grossi bonus, ma se muori sei in modalità hardcore (r. 412-415; `HardcoreDeathAttachmentType.java:36-38`).
5. **Cosa manca** [V]: Spectrum **non ha nulla che assorba danni a consumo di Ink**. L'unico ponte tra Ink e protezione è lo Shieldgrasp Amulet, e solo per alzare il massimo del Dike. Il nostro gimmick riempie quindi un vuoto reale, senza duplicare una meccanica esistente.

---

## 2. Lore: gli agganci che giustificano il gimmick

| Tema | Chiave lang (`LANG`) | Testo (estratto) | Uso nel design |
|---|---|---|---|
| **Blu = protezione** | `book.spectrum.guidebook.pigment.page3.text` | "Blue represents *Safety* and *Permanence*" | colore naturale per una barriera |
| Ciano | `…pigment.page5.text` | "*Matter* and *Integrity*" | danni fisici |
| Magenta | `…pigment.page11.text` | "*Time*, *Kindness* and *Tolerance*" | danni nel tempo (DoT) |
| Giallo | `…pigment.page17.text` | "*Energy* and *Joy*" | fuoco, esplosioni, fulmini |
| Nero | `…pigment.page2.text` | "the nothingness of the *Void*, *Power* and *Mystery*" | magia, wither, danni "oscuri" |
| **Bianco** | `…pigment.page16.text` | "*LIFE*, *Light* and *Purity*" | colpo letale, "vita" |
| Azure Dike | `…azure_dike_equipment.page0.text`, `page1.text` | Azurite + "protective properties of Blue Pigment"; "magical aura … shielding me from incoming damage" | la barriera di Spectrum è *blu* |
| Dike + Ink | `…azure_dike_equipment.page4.text` | "Infusing it with Blue Ink … increases the amount of Azure Dike" | precedente canonico: l'Ink alimenta il Dike |
| **Il passo mancante** | `…azuresque_dike_core.page0.text`, **`page1.text`** | "*…a tiny one towards replicating the structures. The vestiges are far too faint, and the amount of Azurite and Downstone required would be unattainable.*" | **gancio principale**: l'Alloy è la quantità di materia "irraggiungibile" che mancava al Core |
| Rovine | `…preservation_ruins.page1.text` | "Whoever built these structures … was miles ahead. … *I am still alive.*" | chi ci ha preceduto sapeva rendere le cose indistruttibili |
| Moonstone Core | `…moonstone_cores.page0.text` | "capable of channeling huge amounts of Ink with ease" | ingrediente per far scorrere l'Ink nell'armatura |
| **Limite all'onnipotenza** | `…moonstone_shards.page1.text` | "I have never been remotely omnipotent, never will be. Not with all the magic in the world. Nobody is." | la lore **vieta** un'immunità gratuita e totale: serve un costo e un'eccezione (vuoto, `/kill`) |
| Bianco = tutto lo spettro | `…color_mixing_white.page0.text` | "*The whole color spectrum*" | l'Alloy prismatico unisce tutti i colori |
| Progressione | `advancements.spectrum.spectrum_midgame/lategame/endgame.*` | "Paint it Black" / "Turn to Purity" / "A Final Encore" | T7 nero → T8 bianco → Alloy |
| Ascension | `effect.spectrum.ascension.desc`, `…ascension.page0.text` | "A glorious, gathering of forces before Divinity kicks in" | nome dell'addon (§6) |

Nota [V]: la *Spectral Shard* ("Somewhere over the Rainbow", `advancements.spectrum.create_spectral_shard.title`) esiste come item (`SpectrumItems.java:72`), ma il suo sblocco dipende da `spectrum:__wip`, quindi oggi non si ottiene (`J/data/spectrum/advancement/unlocks/resources/spectral_shard.json`). Lo stesso vale per l'**Artist's Palette**, il contenitore di Ink "elementale" (`unlocks/trinkets/artists_palette.json`). Sono contenuti previsti da DaFuqs per il gioco finale: **non vanno usati né imitati** senza chiederglielo (§5).

---

## 3. Numeri comuni: armatura 50 + formula di Apothic

**Formula di Apothic Attributes** [V come config, doc 10]: danno dopo l'armatura = `D · a/(a+armor)`, con `a = 10` se `D < 20`, altrimenti `a = 10 + (D−20)/2`. Protection: `× (1 − min(0,025·punti, 0,85))`. La toughness non riduce più il danno, serve solo contro armor pierce e shred.

Con **armor 50** (Alloy) e senza Protection [D, calcolo]:

| Colpo grezzo D | a | Frazione che passa | Danno dopo l'armatura |
|---|---|---|---|
| 10 (zombie forte) | 10 | 16,7 % | 1,67 |
| 20 | 10 | 16,7 % | 3,33 |
| 40 (mob modded) | 20 | 28,6 % | 11,4 |
| 100 (boss) | 50 | 50 % | 50 |
| 500 (boss ATM/Draconic) | 250 | 83 % | 417 |

Con Protection a 20 punti (−50 %) i valori si dimezzano. **Conseguenza**: con 50 di armatura i colpi piccoli sono già quasi nulli, ma quelli grossi passano in gran parte. È proprio lì che l'Ink deve lavorare.

**Dove agganciarsi**: tutte le proposte usano **`LivingDamageEvent.Pre`**, come il Dike. Ci sono quattro motivi:

- il danno arriva **già ridotto da armatura e Protection**, quindi i 50 punti contano e l'Ink paga solo il resto;
- i frame di invulnerabilità sono già stati applicati. Con `LivingIncomingDamageEvent` annullato non partono mai, e lava, fuoco o cactus colpirebbero **ogni tick**, facendo pagare 20 volte al secondo [V, `LivingEntity.java:1153` contro 1190-1205];
- il knockback e il suono del colpo restano, quindi il giocatore "sente" il colpo;
- possiamo girare a priorità `LOW`, **dopo** il Dike di Spectrum (che usa la priorità di default `NORMAL` [V, r. 526]). In questo modo il Dike gratuito assorbe per primo e l'Ink paga solo il resto.

**Riferimento MekaSuit** [V doc 10]: 250 000 J per punto, 40 MJ di base = **160 punti** di danno per carica di base. Il danno si misura **prima** della sua armatura [D: Mekanism assorbe all'arrivo del colpo]. Un colpo da 100 consuma quindi 25 MJ, il 62 % della carica di base.

**Infinity (Avaritia)**: **non è in ATM10** [V, `ls mods/`]. A memoria [D], il set completo annulla tutto tranne `/kill` e il vuoto, senza costo. Resta un riferimento di "tetto", non un modello: la lore Moonstone lo esclude.

---

## 4. Proposte (tutte basate sull'Ink)

### Proposta A — **Azuresque Bulwark** (Blue Ink + Azure Dike)

**Idea.** L'Alloy realizza ciò che l'Azuresque Dike Core non poteva: replicare il campo di forza delle Preservation Ruins attorno al corpo. L'armatura dà un **grosso Dike** che si ricarica gratis. Quando il Dike finisce, **lo ricostruisce all'istante bruciando Blue Ink**.

**Lore.** Il blu è *Safety and Permanence* (`pigment.page3`). C'è già il precedente "Blue Ink → Dike" (`azure_dike_equipment.page4`, Shieldgrasp). Il Core dice che servivano Azurite e Downstone in quantità "unattainable" (`azuresque_dike_core.page1.text`): i metalli ATM sono quella materia.

**Sblocco.**
- Advancement nascosto `spectral_ascension:unlocks/alloy_armor`. Richiede l'armatura T8, `spectrum:unlocks/trinkets/azuresque_dike_core` (che a sua volta vuole Moonstone Core, Downstone, Aether Vestiges e almeno 30 di Dike [V `J/data/spectrum/advancement/unlocks/trinkets/azuresque_dike_core.json`, `hidden/get_substantial_azure_dike_charge.json`]) e `spectrum:milestones/unlock_ink_use`.
- Ricetta: **Fusion Shrine**. Pezzo T8 + lingotto Alloy ATM + Azuresque Dike Core (o Raw Azurite e Downstone Fragments) + Moonstone Core, con un secchio di Liquid Crystal. Il Core consumato nella ricetta è un costo vero.
- Advancement di prova: trigger `spectrum:azure_dike_charge_change` con `new_charges ≥ 100` ("Unattainable, attained").

**Meccanica (valori di partenza, tutti in config).**
- Set completo: +**40 Dike** (10 a pezzo), contato come una fonte di `AzureDikeItem`, quindi raddoppiato dal Core (80). Ricarica ×4: 1 punto ogni 10 tick, 2 al secondo.
- Overflow: se dopo il Dike resta danno `d`, l'armatura preleva **250 Blue Ink per punto** (2,5 Pigment, in simmetria con i 250 kJ della MekaSuit) e azzera il danno. Se l'Ink non basta, assorbe in proporzione a quello che c'è.
- **Non blocca**: i tag `spectrum:bypasses_dike` (vuoto, `/kill`, annegamento, fame, `spectrum:set_health`) e un nostro tag `spectral_ascension:pierces_bulwark`, vuoto di default, per i pack che vogliono escludere danni di altre mod.
- Autonomia: Pigment Palette blu piena = 409 600 / 250 = **1 638 punti dopo l'armatura**, circa 10 MekaSuit di base. Contro un boss da 100: 50 punti a colpo = 12 500 Ink, cioè **~33 colpi** per Palette, più il Dike. Contro mob da 10: 1,67 punti a colpo, quasi sempre coperti dal Dike gratuito.

**Confronto.** Rispetto alla MekaSuit costa meno sui colpi piccoli (armatura e Dike fanno da filtro) e in modo simile sui grandi. La "batteria" (Pigment) è rinnovabile ma va prodotta. Non arriva mai al livello dell'Infinity: vuoto, `/kill`, annegamento e fame passano.

**Implementazione.**
1. `AlloyArmorItem extends BedrockArmorItem` (doc 02). Il Dike va iniettato in `AzureDikeAttachmentType.recalculate`, che guarda solo i Curios (r. 64-78). Serve un **mixin** con `remap = false`, perché il target è una classe di Spectrum: un `@WrapOperation` sulla chiamata `attachment.set(...)` (r. 83) che aggiunge `armorDike × maxAzureDikeMultiplier`. Quel moltiplicatore è una variabile locale: si cattura con `@Local` di MixinExtras [D, da provare].
2. Su `LivingEquipmentChangeEvent` (cambio di un pezzo d'armatura) chiamiamo `AzureDikeAttachmentType.recalculate(entity)`, che è `public static` [V r. 59].
3. Listener `LivingDamageEvent.Pre`, `priority = LOW`. Controlla: giocatore, set completo, `!source.is(BYPASSES_DIKE)`, `getNewDamage() > 0`.
   - Calcola `cost = ceil(d·250)` e `avail = InkPowered.getAvailableInk(player, BLUE)` [V r. 227-254].
   - Paga con `InkPowered.tryDrainEnergy(player, BLUE, min(cost, avail))`.
   - Attenzione: in caso di fallimento **preleva lo stesso quello che trova** e restituisce `false` [V commento r. 120-126]. Per questo prima si legge `avail`.
   - Imposta il nuovo danno con `setNewDamage(d − pagato/250)`. Effetti: particelle blu e bianche e il suono `PUFF_CIRCLET_PFFT`, come fa il Puff Circlet (r. 502-523).
4. Ci sono gratis: HUD del Dike, Dike Gates (`DikeGateBlock.java:50-51`), sinergia col Puff Circlet.

**Rischi.**
- **HUD**: ogni 20 punti di Dike disegna una "tanica" larga 6 px (`S/render/HudRenderers.java:146-152`). Fino a ~200 punti va bene; oltre esce dallo schermo. Tenere il massimo ≤ 160 o fare un HUD nostro.
- **Mixin su Spectrum**: si rompe se DaFuqs cambia `recalculate` (API instabile, doc 06). Serve un `versionRange` chiuso.
- **PvP**: un giocatore con una Palette piena è quasi intoccabile. Config `pvp_ink_multiplier` (es. ×4 se l'attaccante è un giocatore) o `pvp_enabled=false`.
- **Exploit**: la creativa non paga mai (`InkPowered.java:133`). La Creative Ink Assortment va lasciata fuori dalla survival.
- **Performance**: `getAvailableInk` più `tryDrainEnergy` scorrono circa 40 slot più i Curios **per colpo**, non per tick. Trascurabile [D].
- **Ordine con altre mod**: Draconic Evolution (presente) ha uno scudo proprio. Se agisce in `Incoming`, assorbe prima di noi e risparmia il nostro Ink: accettabile.

---

### Proposta B — **Prismatic Refraction** (colore secondo il tipo di danno)

**Idea.** L'armatura prismatica "rifrange" ogni colpo nel colore che gli corrisponde. Paghi in quel colore, quindi devi mantenere **tutte** le Ink piene. È la versione più "Spectrum" (teoria dei colori), ma anche la più complessa.

**Mappa colori** (in datapack, tramite tag `spectral_ascension:refracts/<colore>`):

| Colore | Damage type (tag vanilla) | Lore |
|---|---|---|
| Cyan | mischia, proiettili (`#is_projectile`), caduta (`#is_fall`), blocchi caduti | Matter & Integrity |
| Yellow | `#is_fire`, `#is_explosion`, `#is_lightning`, `#is_freezing` | Energy |
| Magenta | danni nel tempo: wither, veleno, `spectrum:deadly_poison`, `spectrum:dragonrot` | Time & Tolerance |
| Black | magia (`magic`, `indirect_magic`), `sonic_boom`, danni "void-like" di altre mod | Void & Mystery |
| White | **supplemento** se il colpo sarebbe letale: + `d × 250` White Ink | LIFE |
| fallback | tipo non mappato → prelievo diviso su C/M/Y (⅓ ciascuno) | — |

**Lore.** Le chiavi `pigment.page2/5/11/16/17`. Il ciclo T6 CMY → T7 nero → T8 bianco → Alloy "tutto lo spettro" (`color_mixing_white.page0.text`).

**Sblocco.** Come A, ma serve anche `spectrum:lategame/collect_moonstone` (il bianco si sblocca con la Moonstone, [V `S/api/ink/color/InkColors.java:15`]). Ingredienti extra: polveri delle cinque gemme in Fusion.

**Meccanica.** **100 Ink del colore per punto** dopo l'armatura (meno di A perché i colori sono 5). Il bianco si aggiunge solo sui colpi letali. Con 5 Pigment Palette (409 600 per colore), sono circa **4 096 punti per colore**.

**Implementazione.** Stesso listener `Pre/LOW` di A. Un'utilità `colorFor(DamageSource)` legge i tag, in ordine fisso. Per il "letale" si confronta `d >= player.getHealth() + player.getAbsorptionAmount()`. Tooltip con `InkPowered.addInkPoweredTooltip` e i 5 colori (`getUsedColors`) [V r. 48-66]. Non serve il mixin sul Dike, che si può aggiungere come in A.

**Confronto.** Più costosa da mantenere della MekaSuit (5 "batterie" invece di una), ma più economica sui colpi piccoli. Il giocatore impara cosa lo colpisce: è un gameplay attivo.

**Rischi.**
- Le mod di ATM10 hanno centinaia di damage type non mappati: finiscono nel fallback, e va bene.
- È più difficile spiegarla nel guidebook. Va pensata una pagina con la tabella.
- Il prelievo da più colori deve essere atomico: prima si controllano tutti con `getAvailableInk`, poi si preleva.
- I rischi di PvP e creativa sono gli stessi di A.

---

### Proposta C — **Spectral Equilibrium** (costo fisso su tutti e 5 i colori elementali)

**Idea.** Variante semplice di B: ogni punto assorbito costa **20 Ink di ciascuno** dei 5 colori elementali (C, M, Y, nero, bianco = 100 Ink al punto). Sono proprio i colori "elementali" del codice di Spectrum (`InkColorTags.ELEMENTAL_COLORS`, da cui si mescolano tutti gli altri [V `S/api/ink/color/InkColorMixes.java:17-28`]).

**Lore.** Tutti i colori in equilibrio = luce bianca (`color_mixing_white`). Il verde è *Balance & Harmony* (`pigment.page7`) e si può usare nel nome o nella descrizione.

**Meccanica.** Se manca anche **un solo** colore, l'assorbimento si ferma: "l'equilibrio si rompe". Cinque Pigment Palette danno circa 20 480 punti, sui livelli di B. Costo: 100 Ink al punto, oppure 250 per allinearsi ad A.

**Pro e contro.** Pro: semplice come A ma identità prismatica come B, e nessuna tabella da mantenere. Contro: meno gameplay di B, e il giocatore deve gestire 5 Palette.

**Implementazione e rischi.** Come B, senza la mappa dei colori.

---

### Proposta D (modulo) — **Moonstone Absolution** (White Ink salva-vita)

**Idea.** Si aggiunge ad A, B o C. Se un colpo **che non ha il tag di bypass** porterebbe a morte e l'Ink normale non basta, l'armatura brucia una grossa quantità di **White Ink** (es. 20 000 = 200 Pigment), ti lascia a 1 cuore e va in ricarica per 5 minuti (cooldown come data component sul pettorale).

**Lore.** Bianco = *LIFE* (`pigment.page16`). La Moonstone è "*A Symbol of Healing*" (`advancements.spectrum.place_moonstone_in_preservation_ruins.title`). C'è il precedente del Totem Pendant di Spectrum.

**Implementazione.** `LivingDeathEvent`, che si può annullare, con lo stesso schema del Totem Pendant (`SpectrumEventListeners.java:384-409`): `setHealth(1)`, effetti, `setCanceled(true)`. Va escluso `#bypasses_invulnerability`, altrimenti salverebbe dal vuoto in loop.

**Rischi.** Ordine con il Totem Pendant e con i totem di altre mod (decidere quale scatta per primo). Il cooldown va salvato sull'item, non sul giocatore, per evitare di aggirarlo togliendo e rimettendo il pezzo.

---

## 5. Raccomandazione

**A (Azuresque Bulwark) + D (Moonstone Absolution) come modulo.** Teniamo C come variante "prismatica" se l'utente vuole che l'Alloy consumi tutti i colori.

Motivi:

1. **Lore** [V]: il passaggio "Ink blu → Dike" esiste già in Spectrum (Shieldgrasp, `azure_dike_equipment.page4`). L'Azuresque Core dichiara esplicitamente che replicare le rovine richiederebbe materia "unattainable" (`azuresque_dike_core.page1.text`). A è il seguito naturale di quella frase, non una cosa attaccata sopra.
2. **Riuso**: HUD, sincronizzazione, Dike Gates, Puff Circlet, advancement `azure_dike_charge_change` e il tag `bypasses_dike` sono già pronti. Per uno studente al primo mod vuol dire un listener, un mixin piccolo e una config.
3. **Equilibrio con armatura 50**: il Dike gratuito copre i colpi piccoli (che dopo 50 di armatura valgono 1-3 punti). L'Ink blu paga i colpi grossi, che con la formula di Apothic passano al 50-80 %. Il costo cresce dove deve crescere, come la MekaSuit sui colpi da boss.
4. **Costo guadagnato**: produrre Blue Ink (Pigment → Color Picker) e consumare un Azuresque Dike Core in Fusion.
5. **Coerenza coi tier**: suggerisco che T6/T7/T8 diano un piccolo Dike (es. 2/3/4 a pezzo, senza overflow di Ink). Così l'Alloy è un salto di livello e non una meccanica nuova dal nulla. La decisione è dell'utente.

Valori di partenza: Dike +40 (×2 col Core), ricarica 10 tick/punto, **250 Blue Ink per punto** dopo l'armatura, Absolution 20 000 White Ink con 5 minuti di cooldown. Tutto in `spectral_ascension-common.toml`, perché ATM10 bilancia le mod via config.

---

## 6. Nota breve: alternative senza Ink (scartate dalla decisione 1)

- **Divinity Pact / "Spectral Ascension"**: con il set Alloy e l'effetto Divinity (Circlet of Arrogance, Freigeist), quasi immunità, ma morire = morte hardcore di Spectrum [V `DivinityMobEffect.java`, `HardcoreDeathAttachmentType.java:36-38`, `SpectrumEventListeners.java:412-415`; lore `circlet_of_arrogance.page1.text` "*Pride comes before a fall.*"]. Ha un legame forte col nome dell'addon, ma è troppo punitivo per ATM10. Al massimo è un bonus opzionale in futuro.
- **Dike puro senza Ink**: solo A senza overflow. Troppo debole contro i boss.

---

## 7. Domande aperte

**Per l'utente**
1. Quale colore: Blue da solo (A), mappa per tipo (B) o tutti e 5 (C)?
2. Costo per punto: 250 (simmetria con la MekaSuit) o 100? Il calcolo si fa sul danno dopo l'armatura (consigliato) o prima?
3. Il Dike va dato anche a T6/T7/T8 (piccolo, senza Ink)?
4. PvP: moltiplicatore di costo, disattivazione, o niente?
5. Moonstone Absolution sì o no? Prima o dopo il Totem Pendant?
6. La ricetta deve consumare l'Azuresque Dike Core?

**Per DaFuqs**
1. Accetta un mixin su `AzureDikeAttachmentType.recalculate`, o è disposto ad aggiungere un hook (es. far guardare a `recalculate` anche gli slot armatura con `instanceof AzureDikeItem`)? Sarebbe una modifica di 5 righe in Spectrum.
2. Il tooltip del Core "Doubles damage that cannot be absorbed" (`item.spectrum.azuresque_dike_core.tooltip3`) **non è implementato** in 1.12.9 [V: nessun uso di `AZURESQUE_DIKE_CORE` oltre registrazione e creative tab]. È un bug noto? Se arriverà, si applicherà anche al nostro Dike da armatura?
3. Artist's Palette e Spectral Shard sono bloccati da `spectrum:__wip`: che ruolo avranno? Possiamo agganciarci in futuro (es. Alloy alimentata dall'Artist's Palette)?
4. `InkPowered.tryDrainEnergy` preleva parzialmente anche quando fallisce (r. 120-126): è voluto? Va bene chiamarlo dall'addon con la lettura preventiva di `getAvailableInk`?
5. Ha in mente una meccanica di "immunità" per il gioco finale di Spectrum che rischiamo di anticipare o contraddire?

---

## 8. File letti

- `S/attachment_types/AzureDikeAttachmentType.java` (tutto), `S/api/item/AzureDikeItem.java`, `S/items/trinkets/{AzureDikeCoreItem,AzureDikeRingItem,AzureDikeBeltItem,AzureDikeAmuletItem,InkDrainCurioItem,CircletOfArroganceItem,PuffCircletItem}.java`
- `S/registries/SpectrumEventListeners.java` r. 200-215, 365-420, 476-534, 715-735, 843-904, 925-955; `S/registries/SpectrumMobEffects.java` r. 28, 49-50, 107-115; `S/registries/SpectrumItems.java` r. 72, 476-492; `S/registries/SpectrumFoodComponents.java` r. 195-210
- `S/mob_effect/{AscensionMobEffect,DivinityMobEffect,ImmunityMobEffect}.java`, `S/attachment_types/HardcoreDeathAttachmentType.java`, `S/items/armor/GemstoneArmorItem.java`, `S/mixin/LivingEntityMixin.java` (solo i punti di aggancio)
- `S/api/ink/InkPowered.java` (tutto), `S/api/ink/color/{InkColors,InkColorMixes}.java`, `S/api/ink/storage/TotalCappedElementalMixingInkStorage.java`, `S/items/ink/{PigmentPaletteItem,ArtistsPaletteItem,InkFlaskItem}.java` (intestazioni)
- `S/render/HudRenderers.java` r. 72-152
- `J/data/spectrum/tags/damage_type/{bypasses_dike,uses_set_health}.json`, `J/data/minecraft/tags/damage_type/{bypasses_armor,bypasses_effects,bypasses_enchantments,bypasses_resistance,no_impact}.json`, `J/data/curios/tags/item/*.json`, `J/data/spectrum/curios/slots/ring.json`
- `J/data/spectrum/advancement/`: `midgame/{get_azure_dike_charge,apply_divinity,have_half_a_heart_with_divinity}`, `hidden/get_substantial_azure_dike_charge`, `unlocks/trinkets/{azuresque_dike_core,azure_dike_*,shieldgrasp_amulet,circlet_of_arrogance,artists_palette}`, `unlocks/resources/spectral_shard`, `endgame/finish_progression`, `lategame/{spectrum_lategame,collect_moonstone_core}`
- `J/data/spectrum/modonomicon/books/guidebook/entries/equipment/trinkets/azuresque_dike_core.json`
- `LANG`: chiavi citate in §2 e §4
- `NF/net/minecraft/world/entity/LivingEntity.java` r. 1142-1205, 1785-1806; `NF/.../Entity.java` r. 2681-2685
- Documenti: `ricerca/README.md`, `02-…` (armatura bedrock), `06-…` (API), `10-…` (MekaSuit, Apothic, AttributeFix); `ls` dei mod dell'istanza ATM10
