package com.example.pp.chat.util;


import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PromptLoader {
    private final ResourceLoader loader;
    public PromptLoader(ResourceLoader loader) { this.loader = loader; }

    public String loadPrompt(String name) {
        Resource r = loader.getResource("classpath:prompts/" + name);
        try (var in = r.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("프롬프트 로드 실패: " + name, e);
        }
    }

    public String loadSchema(String name) {
        Resource r = loader.getResource("classpath:schema/" + name);
        try (var in = r.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("스키마 로드 실패: " + name, e);
        }
    }
}
