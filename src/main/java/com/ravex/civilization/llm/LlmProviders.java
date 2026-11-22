package com.ravex.civilization.llm;

import com.ravex.civilization.RavexCiv;
import com.ravex.civilization.RavexConfig;

import java.util.HashSet;
import java.util.Set;

public final class LlmProviders {
    private static OpenAiProvider openAi;
    private static final Set<String> warned = new HashSet<>();

    /** Returns the configured provider, or null if the id is not implemented. */
    public static synchronized LlmProvider current() {
        String id = RavexConfig.PROVIDER.get().toLowerCase();
        if (id.equals("openai")) {
            if (openAi == null) openAi = new OpenAiProvider();
            return openAi;
        }
        if (warned.add(id)) RavexCiv.LOGGER.warn("LLM provider '{}' is not implemented yet; using offline mode.", id);
        return null;
    }

    private LlmProviders() {}
}
