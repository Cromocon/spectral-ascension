# Changelog

## 0.3.0

Texture update.

### New
- **Eternal Classics textures:** an optional built-in resource pack, **Spectral Ascension: Eternal Classics**, redraws all four tiers (Trichromatic, Umbral, Argent and Prismatic) in the style of the Eternal Classics resource pack for Spectrum. It is off by default: turn it on in Options > Resource Packs.

### Changes
- **Animations:** item and armor textures now animate much more slowly (one full cycle every 15 seconds, 30 seconds for Prismatic).

### Requirements
Unchanged: Minecraft 1.21.1, NeoForge 21.1.265 or newer, Spectrum 1.12.9 and Revelationary 1.5.2 (not part of ATM10), Allthemodium 3.2.1 or newer. Install on both client and server.

## 0.1.1

Bug fix release.

### Fixes
- **Prismatic Web:** Apothic Enchanting's Prismatic Web now removes curses from Spectral Ascension gear. Before, the curse came back and the Web consumed its levels for nothing.
- **Workstaff upgrades:** upgrading a workstaff to the next Spectral tier keeps the selected mining mode (Silk Touch or Resonance) instead of switching back to Fortune. Foundry stays paused while Silk Touch is selected, and switching back to Fortune restores the new tier's Fortune.
- **Azure Dike:** swapping a piece while keeping the full set no longer shortens the Azure Dike recharge delay after a hit.
- **Enchanter:** an idle Enchanter holding a book upgrade now starts on its own when its owner unlocks overenchanting, without touching its inventory.
- **Development:** `runClient` works from a clean clone of the repository.

Thanks to brz-master for reporting and fixing the first four issues.

### Requirements
Unchanged: Minecraft 1.21.1, NeoForge 21.1.265 or newer, Spectrum 1.12.9 and Revelationary 1.5.2 (not part of ATM10), Allthemodium 3.2.1 or newer. Install on both client and server.

## 0.1.0

First release of Spectral Ascension, an unofficial Spectrum addon for the All the Mods 10 late game.

### What's in it
- Four tiers of Spectrum equipment: **Trichromatic**, **Umbral**, **Argent** and **Prismatic**, 15 pieces each (tools, Spectrum's special weapons and armor).
- Each tier is unlocked by an advancement and documented in Spectrum's guidebook.
- Enchanting aligned with the ATM10 caps, with overenchanting at the Enchanter unlocked by finding an enchanted book or item above the vanilla maximum.
- Colorful shooting stars fall more often, and Spectrum ores generate in Allthemodium's Mining Dimension.
- Prismatic bow and crossbow arrows leave a trail of colored sparkles.

### Requirements
- Minecraft 1.21.1, NeoForge 21.1.265 or newer
- Spectrum 1.12.9 and Revelationary 1.5.2 (**not part of ATM10**, add them yourself)
- Allthemodium 3.2.1 or newer
- Install on both client and server.

Balance is still being tuned, and some values may change in future versions.
