# DEVELOPMENT STATUS

## Milestone 1 - authored 2026-11-22

### Build and test results (honest)
- **Compiled: NO.** The authoring environment had a JRE only (no `javac`, no Gradle) and no network, so NeoForge/Minecraft could not be downloaded.
- **Run in Minecraft: NO.** Nothing here has been executed.
- **Checks actually done:** brace/paren balance and project-internal imports verified by script on all 25 Java files. Minecraft/NeoForge API calls were written from knowledge of 1.21.1 and are **unverified** - expect a few compile errors on first build (most likely: exact NeoForge/ModDevGradle versions, a method name or signature).
- No JAR is included because none could be built.

### Written (untested) in this milestone
- NeoForge 1.21.1 project, Gradle (ModDevGradle), mods.toml, lang, models.
- `NpcEntity` (humanoid, player-style renderer + armor layer), 7 generated placeholder skins (Yassine + 6 random), item textures.
- Two spawn items: Yassine (unique; refused if one exists; duplicate guard on entity join) and Random NPC (random name from pool, skin, personality, small starter kit).
- Chat: name mention detection (whole-word; English/French/Arabic spellings), `/npc <name> <msg>`, `/npc list`, `/npc release_yassine`.
- Real LLM: OpenAI chat-completions, async (`HttpClient.sendAsync`), result applied on the server thread; JSON reply `{reply, action, remember}`; cost controls (token cap, history window, per-NPC request spacing). Provider interface for future Anthropic/Ollama.
- Offline fallback keyword parser (labelled `[offline]`).
- Persistent memory: per-NPC identity + conversation history + facts in world data; Yassine also mirrored to `<world>/ravex_civ/yassine_memory.json` and restored from it if world data lacks it.
- Missions: follow, stop, protect (stay near player; fights monsters), collect_wood (walk to a log, mine for a tool-speed-based time, real drops into inventory).
- Basic combat: melee, auto-equip sword/axe and armor from inventory (items are moved, not duplicated), retaliates when hurt, attacks nearby monsters (not creepers/endermen/wardens).

### NOT implemented yet (do not treat as working)
- Mining ore, crafting, furnaces/chests, cooking, farming, hunting, house building, shelters, returning home/storing items.
- Hunger management, bows/crossbows/shields, Yassine's Mace / Wind Charge / Elytra+Firework combat.
- NPC-to-NPC communication, teams, relationships.
- Nether/End progression.
- Management GUI (only `/npc list` text for now).
- Anthropic and Ollama providers.
- Parts of collect_wood: no tree-top access, no leaf clearing, no stuck recovery beyond skipping a log.

### Known risks / bugs (suspected, unverified)
- Hostile mobs only target NPCs if they are hurt first or the NPC attacks them; NPCs are not yet targeted proactively.
- Pathing to logs uses vanilla navigation; trunks surrounded by leaves may be skipped.
- Conversation memory beyond `historyTurnsSent` turns is only kept via `remember` facts (no summarisation yet).

### Exact next steps
1. Build it (IntelliJ import or `gradle wrapper && ./gradlew build`), fix compile errors, then `runClient`.
2. Verify: both eggs spawn, Yassine uniqueness, skins render, chat triggers, offline commands, collect_wood, save/reload memory.
3. Milestone 2: chests + crafting table/furnace use, crafting tools, return-home and store items, hunger and food.
4. Milestone 3: NPC-to-NPC messaging, teams, GUI screen.
5. Milestone 4: bow/shield combat; Yassine Mace + Wind Charge; then building, Nether, End.

## Update (same day) - cloud build added
- Added `.github/workflows/build.yml` (Java 21 Temurin, Gradle 8.10.2 via setup-gradle, `gradle clean build`, uploads JAR / source ZIP / logs, failure summary). **The workflow itself has not been run yet**, and no GitHub repository access was available to me.
- Still NOT done from the full feature request: everything under "NOT implemented yet" above. The project is Milestone 1 plus CI, not the complete mod. No JAR exists until the first Actions run succeeds.

## Update - release workflow + changelog
- Added `.github/workflows/release.yml` and `CHANGELOG.md`. Neither workflow has run yet. Feature status is unchanged from Milestone 1.
