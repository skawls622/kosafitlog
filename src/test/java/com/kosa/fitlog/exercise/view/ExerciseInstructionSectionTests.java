package com.kosa.fitlog.exercise.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ExerciseInstructionSectionTests {
    @Test
    void splitsOnlyExplicitMarkersAndKeepsSourceOrder() {
        List<ExerciseInstructionSection> sections = ExerciseInstructionSection.split(
                "Sit down. Tip: Keep still. Caution: Avoid swinging."
                + " Variation: Use a close grip. Variations: Use another handle.");
        assertThat(sections).extracting(ExerciseInstructionSection::getTitle)
                .containsExactly(null, "Tip", "주의사항", "변형 동작", "변형 동작");
        assertThat(sections).extracting(ExerciseInstructionSection::getText)
                .containsExactly("Sit down.", "Keep still.", "Avoid swinging.",
                        "Use a close grip.", "Use another handle.");
    }

    @Test
    void ordinarySentencesAndNewlinesStayInOneUnlabelledSection() {
        String text = "A tip without a delimiter.\n\nNotes: No new section. Fingertip: Still no section.";
        List<ExerciseInstructionSection> sections = ExerciseInstructionSection.split(text);
        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).getTitle()).isNull();
        assertThat(sections.get(0).getText()).isEqualTo(text);
    }

    @Test
    void markersAtStartOrOnNewLinesDoNotCreateEmptyParagraphs() {
        List<ExerciseInstructionSection> sections = ExerciseInstructionSection.split(
                "Tip: \n Caution: Stay in control.\nVariations:\nTry another grip.");
        assertThat(sections).extracting(ExerciseInstructionSection::getTitle)
                .containsExactly("주의사항", "변형 동작");
        assertThat(sections).extracting(ExerciseInstructionSection::getText)
                .containsExactly("Stay in control.", "Try another grip.");
    }

    @Test
    void repeatedMarkersRemainSeparateInOriginalOrder() {
        assertThat(ExerciseInstructionSection.split("Tip: First. Tip: Second."))
                .extracting(ExerciseInstructionSection::getText).containsExactly("First.", "Second.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t\n", "Tip:", "Caution:  Variations:"})
    void absentTextProducesNoSection(String instructions) {
        assertThat(ExerciseInstructionSection.split(instructions)).isEmpty();
    }
}
