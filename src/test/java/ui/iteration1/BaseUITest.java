package ui.iteration1;

import api.configs.Config;
import api.iteration1.BaseTest;
import api.models.Account;
import api.models.CreateAccountResponseModel;
import api.skelethon.steps.UserSteps;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import api.models.CreateUserRequestModel;
import api.models.LoginUserRequestModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import api.skelethon.Endpoint;
import api.skelethon.requesters.CrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.Map;

import static com.codeborne.selenide.Selenide.executeJavaScript;

public abstract class BaseUITest extends BaseTest {
    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = Config.getProperty("uiRemote");
        Configuration.baseUrl = Config.getProperty("uiBaseUrl");
        Configuration.browser = Config.getProperty("browser");
        Configuration.browserSize = Config.getProperty("browserSize");

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }

    // предшаг: логин пользователя через API и сохранение токена в localStorage (без логина через UI)
    protected void authAsUser(CreateUserRequestModel user) {
        String userAuthHeader = RequestSpecs.getUserAuthHeader(user.getUsername(), user.getPassword());
        // localStorage привязан к origin, поэтому сначала открываем приложение
        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
    }

    // вспомогательные методы
    protected void assertAccountUnchanged(CreateUserRequestModel user, CreateAccountResponseModel account,
                                         double expectedBalance, int expectedTransactions) {
        Account acc = UserSteps.getAccountById(UserSteps.getAccountsList(user), account.getId());
        softly.assertThat(acc.getBalance()).isEqualTo(expectedBalance);
        softly.assertThat(acc.getTransactions().size()).isEqualTo(expectedTransactions);
    }

    @AfterEach
    public void closeBrowser() {
        Selenide.closeWebDriver();
    }
}

