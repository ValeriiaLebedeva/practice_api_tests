package ui.iteration1;

import api.iteration1.BaseTest;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import models.CreateUserRequestModel;
import models.LoginUserRequestModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.Map;

import static com.codeborne.selenide.Selenide.executeJavaScript;

public abstract class BaseUITest extends BaseTest {
    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.31.248:3000";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }

    // предшаг: логин пользователя через API и сохранение токена в localStorage (без логина через UI)
    protected void authAsUser(CreateUserRequestModel user) {
        String authHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequestModel.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .build())
                .extract()
                .header("Authorization");

        // localStorage привязан к origin, поэтому сначала открываем приложение
        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", authHeader);
    }

    @AfterEach
    public void closeBrowser() {
        Selenide.closeWebDriver();
    }
}

