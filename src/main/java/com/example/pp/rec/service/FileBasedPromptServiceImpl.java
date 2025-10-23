package com.example.pp.rec.service;

import com.example.pp.rec.model.model;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class FileBasedPromptServiceImpl implements model.PromptService {

    private final ResourceLoader resourceLoader;

    public FileBasedPromptServiceImpl(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public String buildPrompt(String promptName, Map<String, Object> data) {
        try {
            String filePath = "classpath:prompts/" + promptName + ".txt";
            Resource resource = resourceLoader.getResource(filePath);
            Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
            String template = FileCopyUtils.copyToString(reader);

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = "${" + entry.getKey() + "}";
                String value = String.valueOf(entry.getValue());
                template = template.replace(key, value);
            }
            return template;
        } catch (Exception e) {
            throw new RuntimeException("프롬프트 템플릿 로딩 실패: " + promptName, e);
        }
    }
}