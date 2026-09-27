package com.kosa.fitlog.exercise.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kosa.fitlog.api.dto.ExerciseApiDTO;
import com.kosa.fitlog.api.service.DeepLTranslationService;
import com.kosa.fitlog.api.service.ExerciseApiService;
import com.kosa.fitlog.exercise.dto.ExerciseDTO;
import com.kosa.fitlog.exercise.service.ExerciseService;
import com.kosa.fitlog.member.dto.LoginMember;

@WebMvcTest(ExerciseInfoController.class)
@Import({DeepLTranslationService.class, ExerciseInfoControllerTests.HttpMockConfig.class})
class ExerciseInfoControllerTests {
    private static final String GENERATED_CREDENTIAL = UUID.randomUUID().toString();
    private static final String ORIGINAL = "Sit down on a pull-down machine.";
    private static final String SAFETY = "Control the movement.";
    private static final String TRANSLATED_SAFETY = "동작을 제어하세요.";
    private static final String TRANSLATED = "랫풀다운 머신에 앉습니다.\n"
            + "바를 잡고 가슴 위쪽으로 천천히 당깁니다. 반동을 주지 않고 자세를 유지합니다.";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MockServerRestTemplateCustomizer httpMock;
    @MockBean private ExerciseService exerciseService;
    @MockBean private ExerciseApiService apiService;
    private MockRestServiceServer server;
    private ExerciseDTO exercise;
    private ExerciseApiDTO apiExercise;
    private MockHttpSession session;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("deepl.api.key", () -> GENERATED_CREDENTIAL);
    }

    @BeforeEach
    void setUp() {
        server = httpMock.getServer();
        server.reset();
        session = new MockHttpSession();
        session.setAttribute("loginMember", new LoginMember(10L, "member10", "테스트 회원"));
        exercise = new ExerciseDTO();
        exercise.setExerciseId(7L);
        exercise.setExerciseName("랫풀다운");
        exercise.setBodyPart("등");
        exercise.setApiKeyword("lat pulldown");
        when(exerciseService.findById(7L)).thenReturn(exercise);

        apiExercise = new ExerciseApiDTO();
        apiExercise.setName("Lat pulldown");
        apiExercise.setType("strength");
        apiExercise.setDifficulty("intermediate");
        apiExercise.setMuscle("lats");
        apiExercise.setEquipments(Arrays.asList("lat pulldown machine", "lat pulldown bar"));
        apiExercise.setInstructions(ORIGINAL);
        apiExercise.setSafetyInfo(SAFETY);
        when(apiService.searchByName("lat pulldown")).thenReturn(Collections.singletonList(apiExercise));
    }

    @AfterEach
    void verifySingleOrNoTranslationCall() {
        server.verify();
    }

    @Test
    void passesTranslationAndKoreanLabelsWithoutOverwritingOriginalDto() throws Exception {
        expectTranslation(TRANSLATED);
        MvcResult result = mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().isOk()).andExpect(view().name("exercise/info"))
                .andExpect(model().attribute("exercise", exercise))
                .andExpect(model().attribute("apiExercise", apiExercise))
                .andExpect(model().attribute("translatedInstructions", TRANSLATED))
                .andExpect(model().attribute("translatedSafetyInfo", TRANSLATED_SAFETY))
                .andExpect(model().attribute("displayDifficulty", "중급"))
                .andExpect(model().attribute("displayType", "근력 운동"))
                .andExpect(model().attribute("displayMuscle", "광배근"))
                .andExpect(model().attribute("displayEquipments", "랫풀다운 머신 · 랫풀다운 바"))
                .andReturn();
        String html = html(result, "translated");
        assertThat(html).contains("랫풀다운", "등", TRANSLATED, "window.history.back()", TRANSLATED_SAFETY)
                .doesNotContain(ORIGINAL, SAFETY, GENERATED_CREDENTIAL, "[lat pulldown machine");
        assertThat(apiExercise.getInstructions()).isEqualTo(ORIGINAL);
        assertThat(apiExercise.getSafetyInfo()).isEqualTo(SAFETY);
        assertThat(apiExercise.getEquipments()).containsExactly("lat pulldown machine", "lat pulldown bar");
        verify(apiService).searchByName("lat pulldown");
    }

    @Test
    void deepLHttpFailureRendersEnglishOriginalWithStatus200() throws Exception {
        server.expect(requestTo("https://api-free.deepl.com/v2/translate"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        MvcResult result = mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("translatedInstructions", ORIGINAL))
                .andExpect(model().attribute("translatedSafetyInfo", SAFETY))
                .andExpect(content().string(containsString(ORIGINAL)))
                .andReturn();
        assertThat(html(result, "english-fallback")).contains("중급", "근력 운동", "광배근", SAFETY);
    }

    @Test
    void ninjasFailureKeepsExistingEmptyStateAndDoesNotCallDeepL() throws Exception {
        // ExerciseApiService의 기존 장애 계약은 빈 목록 반환이다.
        when(apiService.searchByName("lat pulldown")).thenReturn(Collections.emptyList());
        MvcResult result = mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("translatedInstructions"))
                .andExpect(content().string(containsString("운동 상세 정보를 불러오지 못했습니다.")))
                .andReturn();
        assertThat(html(result, "ninjas-fallback")).contains("랫풀다운", "등");
    }

    @Test
    void nullNinjasResponseIsAlsoSafe() throws Exception {
        when(apiService.searchByName("lat pulldown")).thenReturn(null);
        mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("운동 상세 정보를 불러오지 못했습니다.")));
    }

    @Test
    void nullRepresentativeExerciseDoesNotCallDeepL() throws Exception {
        when(apiService.searchByName("lat pulldown")).thenReturn(Collections.singletonList(null));
        mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("운동 상세 정보를 불러오지 못했습니다.")));
    }

    @Test
    void missingApiKeywordSkipsBothExternalApis() throws Exception {
        exercise.setApiKeyword(null);
        mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("운동 상세 정보를 불러오지 못했습니다.")));
        verifyNoInteractions(apiService);
    }

    @Test
    void missingLocalExerciseKeepsExistingRedirect() throws Exception {
        when(exerciseService.findById(7L)).thenReturn(null);
        mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/list"));
        verifyNoInteractions(apiService);
    }

    @Test
    void missingFieldsAreHiddenWithoutCallingDeepL() throws Exception {
        apiExercise.setDifficulty(null);
        apiExercise.setType(" ");
        apiExercise.setMuscle(null);
        apiExercise.setEquipments(null);
        apiExercise.setInstructions(null);
        apiExercise.setSafetyInfo(null);
        MvcResult result = mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().isOk()).andReturn();
        assertThat(html(result, "missing-fields")).contains("랫풀다운", "등")
                .doesNotContain("<dl", "<section", "불러오지 못했습니다");
    }

    @Test
    void unknownLabelsAndTranslationAreEscapedByThymeleaf() throws Exception {
        String text = "<script>alert('test')</script>";
        apiExercise.setDifficulty("some_unknown_value");
        apiExercise.setEquipments(Collections.singletonList("custom handle"));
        expectTranslation(text);
        MvcResult result = mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().isOk()).andReturn();
        assertThat(html(result, "escaped")).contains("some_unknown_value", "custom handle", "&lt;script&gt;")
                .doesNotContain("<script>");
    }

    @Test
    void unauthenticatedRequestKeepsLoginRedirectWithoutExternalCalls() throws Exception {
        mvc.perform(get("/exercise/info").param("exerciseId", "7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"));
        verifyNoInteractions(exerciseService, apiService);
    }

    @Test
    void explicitSourceSectionsAndSafetyAreTranslatedTogetherAndRenderedInOrder() throws Exception {
        String original = ORIGINAL + " Tip: Keep the torso still. Caution: Avoid swinging."
                + " Variation: Use a close grip. Variations: Use another handle.";
        apiExercise.setInstructions(original);
        List<String> translations = Arrays.asList(TRANSLATED, "몸통을 고정합니다.", "반동을 피합니다.",
                "좁은 그립을 사용합니다.", "다른 손잡이를 사용합니다.", TRANSLATED_SAFETY);
        expectTranslations(Arrays.asList(ORIGINAL, "Keep the torso still.", "Avoid swinging.",
                "Use a close grip.", "Use another handle.", SAFETY), translations);

        MvcResult result = mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().isOk()).andReturn();
        List<?> sections = (List<?>) result.getModelAndView().getModel().get("instructionSections");
        assertThat(sections).extracting("title")
                .containsExactly(null, "Tip", "주의사항", "변형 동작", "변형 동작");
        assertThat(sections).extracting("text").containsExactlyElementsOf(translations.subList(0, 5));
        assertThat(html(result, "sections")).contains(
                ">Tip</h3>", ">주의사항</h3>", ">변형 동작</h3>", TRANSLATED_SAFETY);
        assertThat(apiExercise.getInstructions()).isEqualTo(original);
        assertThat(apiExercise.getSafetyInfo()).isEqualTo(SAFETY);
    }

    @Test
    void sectionBodiesAndSafetyRemainReadableWhenDeepLFails() throws Exception {
        apiExercise.setInstructions(ORIGINAL + " Tip: Keep the torso still. Caution: Avoid swinging.");
        server.expect(requestTo("https://api-free.deepl.com/v2/translate"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        MvcResult result = mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().isOk()).andReturn();
        assertThat(html(result, "sections-fallback")).contains(ORIGINAL,
                "Keep the torso still.", "Avoid swinging.", SAFETY, ">Tip</h3>", ">주의사항</h3>");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t\n"})
    void missingSafetyIsNotSentToDeepLAndGuideIsHidden(String safety) throws Exception {
        apiExercise.setSafetyInfo(safety);
        expectTranslations(Collections.singletonList(ORIGINAL), Collections.singletonList(TRANSLATED));
        MvcResult result = mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().isOk()).andReturn();
        assertThat(html(result, "no-safety")).contains(TRANSLATED).doesNotContain(">안전 가이드</h2>");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t\n"})
    void safetyCanBeTranslatedWithoutInstructions(String instructions) throws Exception {
        apiExercise.setInstructions(instructions);
        expectTranslations(Collections.singletonList(SAFETY), Collections.singletonList(TRANSLATED_SAFETY));
        MvcResult result = mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().isOk()).andReturn();
        assertThat(html(result, "safety-only")).contains(TRANSLATED_SAFETY).doesNotContain(">운동 방법</h2>");
    }

    @Test
    void blankInstructionsAndSafetyDoNotTriggerAnyRequest() throws Exception {
        apiExercise.setInstructions(" \n");
        apiExercise.setSafetyInfo(" \t");
        MvcResult result = mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().isOk()).andReturn();
        assertThat(html(result, "blank-guidance"))
                .doesNotContain(">운동 방법</h2>", ">안전 가이드</h2>");
    }

    @Test
    void delimitersInTranslatedTextDoNotInventSectionsAbsentFromSource() throws Exception {
        expectTranslation("설명에 포함된 Tip: 문구는 원문 구분자가 아닙니다.");
        MvcResult result = mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().isOk()).andReturn();
        assertThat(html(result, "no-invented-sections")).doesNotContain("<h3");
    }

    @Test
    void malformedSafetyTranslationFallsBackToSafetyOriginalOnly() throws Exception {
        server.expect(requestTo("https://api-free.deepl.com/v2/translate"))
                .andRespond(withSuccess("{\"translations\":[{\"text\":\"머신에 앉습니다.\"},{\"text\":null}]}",
                        MediaType.APPLICATION_JSON));
        MvcResult result = mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("translatedSafetyInfo", SAFETY)).andReturn();
        assertThat(html(result, "partial-fallback")).contains("머신에 앉습니다.", SAFETY);
    }

    @Test
    void translatedSafetyIsEscapedAsPlainText() throws Exception {
        expectTranslations(Arrays.asList(ORIGINAL, SAFETY), Arrays.asList(TRANSLATED, "<script>alert(1)</script>"));
        MvcResult result = mvc.perform(get("/exercise/info").param("exerciseId", "7").session(session))
                .andExpect(status().isOk()).andReturn();
        assertThat(html(result, "safe-safety-html")).contains("&lt;script&gt;").doesNotContain("<script>");
    }

    private void expectTranslation(String text) throws Exception {
        expectTranslations(Arrays.asList(ORIGINAL, SAFETY), Arrays.asList(text, TRANSLATED_SAFETY));
    }

    private void expectTranslations(List<String> originals, List<String> translations) throws Exception {
        String response = objectMapper.writeValueAsString(Map.of("translations",
                translations.stream().map(text -> Map.of("text", text)).collect(Collectors.toList())));
        String request = objectMapper.writeValueAsString(Map.of(
                "text", originals, "source_lang", "EN", "target_lang", "KO"));
        server.expect(requestTo("https://api-free.deepl.com/v2/translate"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().json(request, true))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    private String html(MvcResult result, String name) throws Exception {
        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        if (Boolean.getBoolean("exercise.ui.preview")) {
            Files.createDirectories(Path.of("target/exercise-preview"));
            Files.writeString(Path.of("target/exercise-preview", name + ".html"), html, StandardCharsets.UTF_8);
        }
        return html;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class HttpMockConfig {
        @Bean
        MockServerRestTemplateCustomizer httpMock() {
            return new MockServerRestTemplateCustomizer();
        }

        @Bean
        RestTemplateBuilder restTemplateBuilder(MockServerRestTemplateCustomizer httpMock) {
            return new RestTemplateBuilder(httpMock);
        }
    }
}
