package com.kosa.fitlog.exercise.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 영어 원문의 명시적 구분자로만 나눈 화면용 문단. API DTO는 변경하지 않는다. */
public final class ExerciseInstructionSection {
    private static final Pattern MARKER = Pattern.compile("\\b(Tip|Caution|Variations?):");
    private static final Map<String, String> TITLES = Map.of(
            "Tip", "Tip", "Caution", "주의사항",
            "Variation", "변형 동작", "Variations", "변형 동작");

    private final String title;
    private final String text;

    public ExerciseInstructionSection(String title, String text) {
        this.title = title;
        this.text = text;
    }

    public String getTitle() { return title; }
    public String getText() { return text; }

    public static List<ExerciseInstructionSection> split(String instructions) {
        List<ExerciseInstructionSection> sections = new ArrayList<>();
        if (instructions == null || instructions.isBlank()) {
            return sections;
        }

        Matcher matcher = MARKER.matcher(instructions);
        int start = 0;
        String title = null;
        while (matcher.find()) {
            addSection(sections, title, instructions.substring(start, matcher.start()));
            title = TITLES.get(matcher.group(1));
            start = matcher.end();
        }
        addSection(sections, title, instructions.substring(start));
        return sections;
    }

    private static void addSection(List<ExerciseInstructionSection> sections, String title, String text) {
        if (!text.isBlank()) {
            sections.add(new ExerciseInstructionSection(title, text.strip()));
        }
    }
}
