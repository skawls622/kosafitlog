package com.kosa.fitlog.exercise.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ExerciseDisplayLabelsTests {
    @Test
    void mapsKnownValuesForDisplay() {
        assertThat(ExerciseDisplayLabels.difficulty("intermediate")).isEqualTo("중급");
        assertThat(ExerciseDisplayLabels.type("strength")).isEqualTo("근력 운동");
        assertThat(ExerciseDisplayLabels.muscle("lats")).isEqualTo("광배근");
    }

    @Test
    void acceptsCaseAndWhitespaceForKnownValues() {
        assertThat(ExerciseDisplayLabels.difficulty(" Intermediate ")).isEqualTo("중급");
    }

    @Test
    void unknownValuesKeepOriginalSpelling() {
        String unknown = "some_unknown_value";
        assertThat(ExerciseDisplayLabels.difficulty(unknown)).isEqualTo(unknown);
        assertThat(ExerciseDisplayLabels.type(unknown)).isEqualTo(unknown);
        assertThat(ExerciseDisplayLabels.muscle(unknown)).isEqualTo(unknown);
        assertThat(ExerciseDisplayLabels.equipments(Collections.singletonList(unknown))).isEqualTo(unknown);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void missingLabelsCanBeHiddenSafely(String value) {
        assertThat(ExerciseDisplayLabels.difficulty(value)).isNull();
        assertThat(ExerciseDisplayLabels.type(value)).isNull();
        assertThat(ExerciseDisplayLabels.muscle(value)).isNull();
    }

    @Test
    void equipmentListUsesMiddleDotsAndPreservesUnknownNamesWithoutMutatingInput() {
        List<String> values = Arrays.asList("lat pulldown machine", null, " ", "lat pulldown bar", "custom handle");
        assertThat(ExerciseDisplayLabels.equipments(values))
                .isEqualTo("랫풀다운 머신 · 랫풀다운 바 · custom handle");
        assertThat(values).containsExactly("lat pulldown machine", null, " ", "lat pulldown bar", "custom handle");
    }

    @Test
    void noEquipmentProducesNoArrayString() {
        assertThat(ExerciseDisplayLabels.equipments(null)).isEmpty();
        assertThat(ExerciseDisplayLabels.equipments(Collections.emptyList())).isEmpty();
        assertThat(ExerciseDisplayLabels.equipments(Arrays.asList(null, " "))).isEmpty();
    }
}
