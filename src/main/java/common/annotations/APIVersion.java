package common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Для каких версий образа бэкенда написан тест (например, "with_validation_fix", "with_database").
 * Если текущая версия (backendVersion в config.properties) не входит в список, тест пропускается.
 * Тесты без аннотации выполняются на любой версии. Работу делает APIVersionExtension.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface APIVersion {
    String[] value();
}
