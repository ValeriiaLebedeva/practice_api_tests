package models.assertions;

import models.BaseModel;
import org.assertj.core.api.SoftAssertions;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Сравнивает модель запроса с моделью ответа по правилам из model-comparison.properties.
 *
 * Использование:
 *   ModelAssertions.assertThatModels(request, response).match();          // упадет сразу со списком расхождений
 *   ModelAssertions.assertThatModels(request, response).match(softly);     // добавит проверки в softly теста
 */
public final class ModelAssertions {
    private final BaseModel request;
    private final BaseModel response;

    private ModelAssertions(BaseModel request, BaseModel response) {
        this.request = request;
        this.response = response;
    }

    public static ModelAssertions assertThatModels(BaseModel request, BaseModel response) {
        return new ModelAssertions(request, response);
    }

    public void match() {
        SoftAssertions softly = new SoftAssertions();
        match(softly);
        softly.assertAll();
    }

    public void match(SoftAssertions softly) {
        ModelComparisonConfigLoader.ModelRules rules = ModelComparisonConfigLoader.getRules(request.getClass());

        String actualResponseClass = response.getClass().getSimpleName();
        if (!rules.responseClassName().equals(actualResponseClass)) {
            throw new AssertionError("Для " + request.getClass().getSimpleName()
                    + " ожидается ответ " + rules.responseClassName()
                    + ", а получен " + actualResponseClass);
        }

        for (ModelComparisonConfigLoader.FieldRule fieldRule : rules.fields()) {
            Object expected = readPath(request, fieldRule.requestPath());
            Object actual = readPath(response, fieldRule.responsePath());
            String description = String.format("%s.%s -> %s.%s [%s]: request=<%s>, response=<%s>",
                    request.getClass().getSimpleName(), fieldRule.requestPath(),
                    response.getClass().getSimpleName(), fieldRule.responsePath(),
                    fieldRule.rule(), expected, actual);

            switch (fieldRule.rule()) {
                case EQUALS -> softly.assertThat(areEqual(expected, actual)).as(description).isTrue();
                case NOT_EQUALS -> softly.assertThat(areEqual(expected, actual)).as(description).isFalse();
                case IGNORE -> {
                }
            }
        }
    }

    // числа сравниваем по значению, чтобы 100 (int) и 100.0 (double) считались равными
    private static boolean areEqual(Object expected, Object actual) {
        if (expected instanceof Number e && actual instanceof Number a) {
            return new BigDecimal(e.toString()).compareTo(new BigDecimal(a.toString())) == 0;
        }
        return Objects.equals(expected, actual);
    }

    // читает поле по пути вида "name" или "customer.name"
    private static Object readPath(Object source, String path) {
        Object current = source;
        for (String name : path.split("\\.")) {
            if (current == null) {
                return null;
            }
            current = readField(current, name);
        }
        return current;
    }

    private static Object readField(Object target, String fieldName) {
        Class<?> clazz = target.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot read field " + fieldName, e);
            }
        }
        throw new IllegalArgumentException("В классе " + target.getClass().getSimpleName()
                + " нет поля '" + fieldName + "' (проверьте model-comparison.properties)");
    }
}
