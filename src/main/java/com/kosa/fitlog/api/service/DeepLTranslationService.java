package com.kosa.fitlog.api.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class DeepLTranslationService {

    private static final Logger log = LoggerFactory.getLogger(DeepLTranslationService.class);
    private static final String TRANSLATE_URL = "https://api-free.deepl.com/v2/translate";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public DeepLTranslationService(RestTemplateBuilder builder,
            @Value("${deepl.api.key}") String apiKey) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.apiKey = apiKey;
    }

    public String translateToKorean(String text) {
        return translateAllToKorean(Collections.singletonList(text)).get(0);
    }

    /** 비어 있지 않은 문자열만 한 번에 번역하며, 반환 목록의 순서와 크기를 유지한다. */
    public List<String> translateAllToKorean(List<String> texts) {
        if (texts == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(texts);
        if (apiKey == null || apiKey.isBlank()) {
            return result;
        }

        List<String> requestTexts = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (text != null && !text.isBlank()) {
                requestTexts.add(text);
                positions.add(i);
            }
        }
        if (requestTexts.isEmpty()) {
            return result;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, "DeepL-Auth-Key " + apiKey);

        Map<String, Object> body = Map.of(
                "text", requestTexts,
                "source_lang", "EN",
                "target_lang", "KO");

        try {
            JsonNode response = restTemplate.postForObject(TRANSLATE_URL,
                    new HttpEntity<>(body, headers), JsonNode.class);

            // 응답의 타입까지 확인하여 숫자/객체 등을 번역문으로 표시하지 않는다.
            JsonNode translations = response == null ? null : response.path("translations");
            if (translations == null || !translations.isArray()
                    || translations.size() != requestTexts.size()) {
                // 개수가 다르면 문단과 번역문을 잘못 연결할 수 있으므로 전체 원문을 유지한다.
                log.warn("DeepL translation failed: invalid response");
                return result;
            }
            for (int i = 0; i < translations.size(); i++) {
                JsonNode translatedText = translations.get(i).path("text");
                if (translatedText.isTextual() && !translatedText.asText().isBlank()) {
                    result.set(positions.get(i), translatedText.asText());
                } else {
                    log.warn("DeepL translation failed: invalid translation item");
                }
            }
        } catch (RestClientResponseException exception) {
            log.warn("DeepL translation failed: HTTP {}", exception.getRawStatusCode());
        } catch (RestClientException exception) {
            // 예외 메시지/응답 본문/헤더는 인증 정보를 포함할 수 있으므로 기록하지 않는다.
            log.warn("DeepL translation failed: request or response error");
        }
        return result;
    }
}
