package common.extensions;

import api.configs.Config;
import com.codeborne.selenide.Configuration;
import common.annotations.UiEnvironment;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.Map;
import java.util.Optional;

/**
 * Настраивает Selenide перед каждым тестом: значения из Config, поверх них то, что задано в @UiEnvironment.
 * Должно стоять в BaseUITest первым: остальные расширения открывают браузер, и настройки к этому моменту
 * уже должны быть применены.
 * Настройки Selenide глобальные, поэтому применяются заново перед каждым тестом: переопределение
 * на одном методе не «протекает» в следующие тесты.
 */
public class UiEnvironmentExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        Optional<UiEnvironment> env = AnnotationSupport.findAnnotation(context.getElement(), UiEnvironment.class);

        Configuration.baseUrl = pick(env.map(UiEnvironment::baseUrl), "uiBaseUrl");
        Configuration.remote = pick(env.map(UiEnvironment::remote), "uiRemote");
        Configuration.browser = pick(env.map(UiEnvironment::browser), "browser");
        Configuration.browserSize = pick(env.map(UiEnvironment::browserSize), "browserSize");
        Configuration.headless = env.map(UiEnvironment::headless).orElse(true);

        boolean vnc = env.map(UiEnvironment::vnc).orElse(false);
        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", vnc, "enableLog", true));
    }

    // значение из аннотации, а если оно пустое, то из Config
    private static String pick(Optional<String> fromAnnotation, String configKey) {
        return fromAnnotation.filter(value -> !value.isBlank())
                .orElseGet(() -> Config.getProperty(configKey));
    }
}
