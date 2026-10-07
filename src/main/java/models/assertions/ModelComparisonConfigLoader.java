package models.assertions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Читает model-comparison.properties и превращает его в правила сравнения.
 *
 * Формат файла:
 *   <RequestClass>=<ResponseClass>
 *   <RequestClass>.<поле запроса>=<поле ответа>[:RULE]
 *
 * Пути могут быть вложенными: customer.name
 * RULE: EQUALS (по умолчанию), NOT_EQUALS, IGNORE
 */
public final class ModelComparisonConfigLoader {
    private static final String FILE_NAME = "model-comparison.properties";
    private static final Map<String, ModelRules> RULES = load();

    private ModelComparisonConfigLoader() {
    }

    public record FieldRule(String requestPath, String responsePath, ComparisonRule rule) {
    }

    public record ModelRules(String responseClassName, List<FieldRule> fields) {
    }

    public static ModelRules getRules(Class<?> requestClass) {
        ModelRules rules = RULES.get(requestClass.getSimpleName());
        if (rules == null) {
            throw new IllegalStateException("В " + FILE_NAME + " нет правил для "
                    + requestClass.getSimpleName() + " (ожидается строка '"
                    + requestClass.getSimpleName() + "=<ResponseClass>')");
        }
        return rules;
    }

    private static Map<String, ModelRules> load() {
        Properties props = new Properties();
        try (InputStream in = ModelComparisonConfigLoader.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
            if (in == null) {
                throw new IllegalStateException(FILE_NAME + " not found in resources");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Fail to load " + FILE_NAME, e);
        }

        Map<String, String> responseClasses = new HashMap<>();
        Map<String, List<FieldRule>> fieldRules = new HashMap<>();

        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key).trim();
            int dot = key.indexOf('.');

            if (dot < 0) {
                responseClasses.put(key.trim(), value);
                continue;
            }

            String className = key.substring(0, dot).trim();
            String requestPath = key.substring(dot + 1).trim();

            String[] parts = value.split(":", 2);
            String responsePath = parts[0].trim();
            ComparisonRule rule = parts.length > 1
                    ? ComparisonRule.valueOf(parts[1].trim().toUpperCase())
                    : ComparisonRule.EQUALS;

            fieldRules.computeIfAbsent(className, k -> new ArrayList<>())
                    .add(new FieldRule(requestPath, responsePath, rule));
        }

        Map<String, ModelRules> result = new HashMap<>();
        for (Map.Entry<String, String> entry : responseClasses.entrySet()) {
            result.put(entry.getKey(), new ModelRules(
                    entry.getValue(),
                    fieldRules.getOrDefault(entry.getKey(), List.of())));
        }
        return result;
    }
}
