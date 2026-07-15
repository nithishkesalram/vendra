package com.procureai.ai.llm;

import org.springframework.stereotype.Component;

@Component
public class HeuristicLlmClient implements LlmClient {

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return "Heuristic response generated locally. Configure an OpenAI-backed LlmClient to call a hosted model.";
    }
}
