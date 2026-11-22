# RAVEX AI Civilization (Milestone 1)

Minecraft Java **1.21.1**, **NeoForge**, singleplayer. Text-chat-only AI NPCs (no microphone/voice).

> **Status: source project, NOT yet compiled or tested.** See `DEVELOPMENT_STATUS.md` for exactly what is and is not verified.

## Build
Requirements: JDK 21. The Gradle wrapper JAR could not be generated offline, so use either:
- **IntelliJ IDEA**: File > Open > this folder (it imports the Gradle project), then run the `build` task; or
- Install Gradle 8.10+ once and run `gradle wrapper` then `./gradlew build` (Windows: `gradlew.bat build`).

The mod JAR appears in `build/libs/`. Drop it in your `mods/` folder (NeoForge 1.21.1). To test in a dev client: `./gradlew runClient`.

Versions are set in `gradle.properties` (`neo_version=21.1.172`). If Gradle says that version doesn't exist, pick the newest `21.1.x` from https://projects.neoforged.net/neoforged/neoforge and update it.

## AI setup (API key is never in source, config, or the world)
Provide your OpenAI key using ONE of:
1. Environment variable `RAVEX_OPENAI_API_KEY` (or `OPENAI_API_KEY`) before launching Minecraft, or
2. A file `config/ravex_civ_openai_key.txt` in your Minecraft folder, with the key on the first line.

Settings are in `config/ravex_civ-common.toml` (model, token limit, history size, request spacing, update interval, starter kit).
Without a key (or if a request fails) NPCs use a small keyword parser; its replies are always prefixed `[offline]`.

Only the `openai` provider is implemented. `LlmProvider` is the interface to add Anthropic/Ollama later.

## Using it
- Creative tab "RAVEX AI Civilization": **Yassine Spawn Egg** (one permanent Yassine; a second is refused) and **Random NPC Spawn Egg**.
- Talk in normal chat by naming an NPC: `Yassine, please collect 16 logs`, `ياسين تعال`, `Adam, suis-moi`.
- Or `/npc Yassine Hello` (Arabic names must be quoted: `/npc "ياسين" مرحبا`).
- `/npc list` shows health, position, mission, inventory. `/npc release_yassine` (op) clears Yassine's location lock if he is lost.

## Replace Yassine's skin
Put your own 64x64 skin at `src/main/resources/assets/ravex_civ/textures/entity/yassine.png` (the included one is a generated placeholder).

## Build in the cloud (nothing to install)
1. On github.com click **+ > New repository**, name it, create it.
2. Click **uploading an existing file**, unzip this project on your PC and drag ALL its contents (including the hidden `.github` folder) into the page, then **Commit changes**. (If the hidden folder is awkward to drag: use **Add file > Create new file**, type `.github/workflows/build.yml` as the name and paste the file's contents.)
3. Open the **Actions** tab > the latest "Build RAVEX AI Civilization" run.
4. Green = download the **ravex-ai-civilization-jar** artifact at the bottom of the run. Red = open the run, download **build-logs** and paste the errors to Claude to fix.
