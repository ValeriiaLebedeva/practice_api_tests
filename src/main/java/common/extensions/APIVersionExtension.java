package common.extensions;

import api.configs.Config;
import common.annotations.APIVersion;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.Arrays;

/**
 * Пропускает тест, если текущая версия бэкенда не входит в @APIVersion.
 * Текущая версия берётся из системного свойства -DbackendVersion=..., а если его нет, из config.properties.
 * @APIVersion можно поставить на класс или на метод: JUnit проверяет условие и для класса, и для метода.
 */
public class APIVersionExtension implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), APIVersion.class)
                .map(version -> check(version.value(), currentVersion()))
                .orElse(ConditionEvaluationResult.enabled("@APIVersion не задан"));
    }

    private static ConditionEvaluationResult check(String[] supported, String current) {
        boolean matches = Arrays.asList(supported).contains(current);
        return matches
                ? ConditionEvaluationResult.enabled("Версия бэкенда " + current + " подходит")
                : ConditionEvaluationResult.disabled("Тест для версий " + Arrays.toString(supported)
                + ", текущая: " + current);
    }

    private static String currentVersion() {
        String fromSystem = System.getProperty("backendVersion");
        return fromSystem != null && !fromSystem.isBlank() ? fromSystem : Config.getProperty("backendVersion");
    }
}
