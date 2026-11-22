package com.ravex.civilization.llm;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * API keys live OUTSIDE the source, the mod config and the world. Lookup order:
 *   1. env RAVEX_OPENAI_API_KEY   2. env OPENAI_API_KEY
 *   3. first line of  <minecraft>/config/ravex_civ_openai_key.txt
 */
public final class ApiKeys {
    public static String openAi() {
        String k = System.getenv("RAVEX_OPENAI_API_KEY");
        if (k == null || k.isBlank()) k = System.getenv("OPENAI_API_KEY");
        if (k == null || k.isBlank()) {
            try {
                Path p = FMLPaths.CONFIGDIR.get().resolve("ravex_civ_openai_key.txt");
                if (Files.isRegularFile(p)) {
                    var lines = Files.readAllLines(p);
                    if (!lines.isEmpty()) k = lines.get(0);
                }
            } catch (Exception ignored) {
            }
        }
        return k == null ? "" : k.trim();
    }

    private ApiKeys() {}
}
