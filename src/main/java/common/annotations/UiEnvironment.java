package common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Окружение UI-тестов: адрес приложения, Selenoid, браузер, размер окна.
 * Пустое значение означает «взять из Config» (uiBaseUrl, uiRemote, browser, browserSize).
 * Ставится на тестовый метод.
 * Без аннотации тест работает с настройками из Config.
 * Работу делает UiEnvironmentExtension, подключённое на BaseUITest.
 *
 * Примеры:
 * @UiEnvironment(browser = "firefox")
 * @UiEnvironment(baseUrl = "http://stage:3000", browserSize = "1366x768")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UiEnvironment {
    String baseUrl() default "";

    String remote() default "";

    String browser() default "";

    String browserSize() default "";

    // видео-просмотр сессии в Selenoid (VNC)
    boolean vnc() default true;
}
