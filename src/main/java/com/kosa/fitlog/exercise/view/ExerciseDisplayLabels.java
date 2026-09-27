package com.kosa.fitlog.exercise.view;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

/** API 원본 DTO를 바꾸지 않는 화면 전용 표시값. 알 수 없는 값은 원문을 유지한다. */
public final class ExerciseDisplayLabels {

    // API Ninjas 공식 분류/응답 예시 및 현재 화면에서 사용하는 값만 매핑한다.
    // https://api-ninjas.com/api/exercises
    private static final Map<String, String> DIFFICULTIES = Map.of(
            "beginner", "초급", "intermediate", "중급", "expert", "고급");

    private static final Map<String, String> TYPES = Map.of(
            "strength", "근력 운동",
            "cardio", "유산소 운동",
            "stretching", "스트레칭",
            "olympic_weightlifting", "역도",
            "plyometrics", "플라이오메트릭 운동",
            "powerlifting", "파워리프팅",
            "strongman", "스트롱맨 운동");

    private static final Map<String, String> MUSCLES = Map.of(
            "lats", "광배근", "chest", "가슴", "biceps", "이두근",
            "triceps", "삼두근", "quadriceps", "대퇴사두근",
            "glutes", "둔근", "abdominals", "복근");

    private static final Map<String, String> EQUIPMENTS = Map.of(
            "lat pulldown machine", "랫풀다운 머신",
            "lat pulldown bar", "랫풀다운 바",
            "barbell", "바벨",
            "dumbbell", "덤벨",
            "dumbbells", "덤벨",
            "flat bench", "플랫 벤치",
            "incline bench", "인클라인 벤치");

    private ExerciseDisplayLabels() {
    }

    public static String difficulty(String value) {
        return label(DIFFICULTIES, value);
    }

    public static String type(String value) {
        return label(TYPES, value);
    }

    public static String muscle(String value) {
        return label(MUSCLES, value);
    }

    public static String equipments(List<String> values) {
        StringJoiner labels = new StringJoiner(" · ");
        if (values != null) {
            for (String value : values) {
                String display = label(EQUIPMENTS, value);
                if (display != null) {
                    labels.add(display);
                }
            }
        }
        return labels.toString();
    }

    private static String label(Map<String, String> labels, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return labels.getOrDefault(value.trim().toLowerCase(Locale.ROOT), value);
    }
}
