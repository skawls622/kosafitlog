package com.kosa.fitlog.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;

@ExtendWith(OutputCaptureExtension.class)
class DeepLTranslationServiceTests {
    private static final String URL = "https://api-free.deepl.com/v2/translate";
    private static final String ORIGINAL = "Sit down on a pull-down machine.";
    private MockRestServiceServer server;
    private DeepLTranslationService service;
    private String generatedCredential;

    @BeforeEach
    void setUp() {
        // 실제 키나 고정된 키를 사용하지 않으며, 모든 HTTP 요청은 Mock으로 차단한다.
        generatedCredential = UUID.randomUUID().toString();
        service = new DeepLTranslationService(mockBuilder(), generatedCredential);
    }

    private RestTemplateBuilder mockBuilder() {
        return new RestTemplateBuilder().additionalCustomizers(template ->
                server = MockRestServiceServer.bindTo(template).build());
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void sendsOneJsonRequestWithHeaderAuthenticationAndReturnsKorean() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "DeepL-Auth-Key " + generatedCredential))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"text\":[\"" + ORIGINAL
                        + "\"],\"source_lang\":\"EN\",\"target_lang\":\"KO\"}", true))
                .andRespond(withSuccess("{\"translations\":[{\"detected_source_language\":\"EN\","
                        + "\"text\":\"랫풀다운 머신에 앉습니다.\"}]}", MediaType.APPLICATION_JSON));

        assertThat(service.translateToKorean(ORIGINAL)).isEqualTo("랫풀다운 머신에 앉습니다.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void doesNotCallApiForMissingText(String text) {
        assertThat(service.translateToKorean(text)).isEqualTo(text);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void doesNotCallApiForMissingKey(String key) {
        service = new DeepLTranslationService(mockBuilder(), key);
        assertThat(service.translateToKorean(ORIGINAL)).isEqualTo(ORIGINAL);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 429, 500, 503})
    void httpErrorsReturnOriginalWithoutLoggingCredentials(int status, CapturedOutput output) {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.valueOf(status))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"" + generatedCredential + "\"}"));

        assertThat(service.translateToKorean(ORIGINAL)).isEqualTo(ORIGINAL);
        assertThat(output).contains("DeepL translation failed: HTTP " + status)
                .doesNotContain(generatedCredential, "DeepL-Auth-Key");
    }

    @Test
    void connectionFailureReturnsOriginal(CapturedOutput output) {
        server.expect(requestTo(URL)).andRespond(request -> {
            throw new IOException(generatedCredential);
        });
        assertThat(service.translateToKorean(ORIGINAL)).isEqualTo(ORIGINAL);
        assertThat(output).doesNotContain(generatedCredential);
    }

    @Test
    void timeoutReturnsOriginal(CapturedOutput output) {
        server.expect(requestTo(URL)).andRespond(request -> {
            throw new SocketTimeoutException(generatedCredential);
        });
        assertThat(service.translateToKorean(ORIGINAL)).isEqualTo(ORIGINAL);
        assertThat(output).doesNotContain(generatedCredential);
    }

    @Test
    void restClientExceptionReturnsOriginal(CapturedOutput output) {
        RestTemplateBuilder builder = mockBuilder().additionalCustomizers(template ->
                template.getInterceptors().add((request, body, execution) -> {
                    throw new RestClientException(generatedCredential);
                }));
        service = new DeepLTranslationService(builder, generatedCredential);
        assertThat(service.translateToKorean(ORIGINAL)).isEqualTo(ORIGINAL);
        assertThat(output).doesNotContain(generatedCredential);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "null", "{}", "[]", "{broken-json",
            "{\"translations\":null}", "{\"translations\":[]}",
            "{\"translations\":{\"text\":\"wrong shape\"}}",
            "{\"translations\":[null]}", "{\"translations\":[{}]}",
            "{\"translations\":[{\"text\":null}]}",
            "{\"translations\":[{\"text\":\"\"}]}",
            "{\"translations\":[{\"text\":\"   \"}]}",
            "{\"translations\":[{\"text\":123}]}",
            "{\"translations\":[{\"text\":{}}]}"
    })
    void missingOrMalformedResponseReturnsOriginal(String response) {
        server.expect(requestTo(URL)).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertThat(service.translateToKorean(ORIGINAL)).isEqualTo(ORIGINAL);
    }

    @Test
    void unexpectedContentTypeReturnsOriginal() {
        server.expect(requestTo(URL)).andRespond(withSuccess("<html>Unavailable</html>", MediaType.TEXT_HTML));
        assertThat(service.translateToKorean(ORIGINAL)).isEqualTo(ORIGINAL);
    }

    @Test
    void appliesConnectionAndReadTimeouts() {
        new DeepLTranslationService(new RestTemplateBuilder().additionalCustomizers(template -> {
            assertThat(ReflectionTestUtils.getField(template.getRequestFactory(), "connectTimeout"))
                    .isEqualTo(2000);
            assertThat(ReflectionTestUtils.getField(template.getRequestFactory(), "readTimeout"))
                    .isEqualTo(5000);
        }), generatedCredential);
    }

    @Test
    void batchPreservesPositionsAndOnlySendsNonblankTextsInOneRequest() {
        List<String> input = Arrays.asList(ORIGINAL, null, "  ", "Control the movement.");
        server.expect(requestTo(URL))
                .andExpect(content().json("{\"text\":[\"" + ORIGINAL
                        + "\",\"Control the movement.\"],\"source_lang\":\"EN\",\"target_lang\":\"KO\"}", true))
                .andRespond(withSuccess("{\"translations\":[{\"text\":\"머신에 앉습니다.\"},"
                        + "{\"text\":\"동작을 제어하세요.\"}]}", MediaType.APPLICATION_JSON));

        assertThat(service.translateAllToKorean(input))
                .containsExactly("머신에 앉습니다.", null, "  ", "동작을 제어하세요.");
        assertThat(input).containsExactly(ORIGINAL, null, "  ", "Control the movement.");
    }

    @Test
    void emptyBatchNeverCallsApi() {
        assertThat(service.translateAllToKorean(null)).isEmpty();
        assertThat(service.translateAllToKorean(Collections.emptyList())).isEmpty();
        assertThat(service.translateAllToKorean(Arrays.asList(null, "", " \n")))
                .containsExactly(null, "", " \n");
    }

    @Test
    void invalidBatchItemOnlyFallsBackForThatItem() {
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"translations\":[{\"text\":\"머신에 앉습니다.\"},null]}", MediaType.APPLICATION_JSON));
        assertThat(service.translateAllToKorean(Arrays.asList(ORIGINAL, "Control the movement.")))
                .containsExactly("머신에 앉습니다.", "Control the movement.");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"translations\":[]}",
            "{\"translations\":[{\"text\":\"too few\"}]}",
            "{\"translations\":[{\"text\":\"one\"},{\"text\":\"two\"},{\"text\":\"extra\"}]}"
    })
    void wrongResponseCountKeepsWholeBatchToAvoidMismatchingParagraphs(String response) {
        server.expect(requestTo(URL)).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertThat(service.translateAllToKorean(Arrays.asList(ORIGINAL, "Control the movement.")))
                .containsExactly(ORIGINAL, "Control the movement.");
    }

    @Test
    void batchHttpFailureKeepsAllOriginals() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThat(service.translateAllToKorean(Arrays.asList(ORIGINAL, "Control the movement.")))
                .containsExactly(ORIGINAL, "Control the movement.");
    }

    @Test
    void batchTimeoutKeepsAllOriginals() {
        server.expect(requestTo(URL)).andRespond(request -> { throw new SocketTimeoutException(); });
        assertThat(service.translateAllToKorean(Arrays.asList(ORIGINAL, "Control the movement.")))
                .containsExactly(ORIGINAL, "Control the movement.");
    }
}
