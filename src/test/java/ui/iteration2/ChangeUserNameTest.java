package ui.iteration2;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverConditions;
import generators.RandomData;
import models.CreateUserRequestModel;
import models.UpdateCustomerProfileRequestModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.Keys;
import requests.skelethon.steps.AdminSteps;
import requests.skelethon.steps.UserSteps;
import ui.iteration1.BaseUITest;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static com.codeborne.selenide.Selenide.*;

public class ChangeUserNameTest extends BaseUITest {


    private static final String MSG_UPDATED = "✅ Name updated successfully!";
    private static final String MSG_INVALID_NAME = "❌ Please enter a valid name.";
    private static final String MSG_SAME_NAME = "⚠️ New name is the same as the current one.";
    // сообщение бэка выводится в алерте как есть, без эмодзи
    private static final String MSG_BACKEND_INVALID_NAME = "Name must contain two words with letters only";

    // ---------- N-1. Успешное изменение имени ----------
    @Test
    public void userCanUpdateNameTest() {
        // предшаги через API: у пользователя уже есть имя
        CreateUserRequestModel user = AdminSteps.createUser();
        String oldName = setProfileName(user);

        authAsUser(user);
        openEditProfile(oldName);

        // целевое действие на UI
        String newName = RandomData.getProfileName();
        enterName(newName);
        nameInput().shouldHave(Condition.exactValue(newName));
        saveButton().click();

        // проверка UI: алерт об успехе
        Selenide.confirm(MSG_UPDATED);

        // проверка API
        softly.assertThat(UserSteps.getProfile(user).getName()).isEqualTo(newName);

        // проверка UI: форма после повторного открытия подставляет новое имя
        openEditProfile(newName);
    }

    // ---------- N-1б. После сохранения имени происходит переход на /dashboard ----------
    // В коде переход стоит последним шагом, после повторного запроса профиля и setUser.
    // Если любой из них падает, пользователь остаётся на /edit-profile. Тест проверяет только переход.
    @Test
    public void userIsRedirectedToDashboardAfterNameChangeTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        String oldName = setProfileName(user);

        authAsUser(user);
        openEditProfile(oldName);

        enterName(RandomData.getProfileName());
        saveButton().click();

        Selenide.confirm(MSG_UPDATED);
        webdriver().shouldHave(WebDriverConditions.urlContaining("/dashboard"));
    }

    // ---------- N-1в. Новое имя отображается на дашборде ----------
    // Дашборд открываем напрямую и не ждём перехода, поэтому тест не зависит от редиректа.
    @Test
    public void newNameIsShownOnDashboardTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        String oldName = setProfileName(user);

        authAsUser(user);
        openEditProfile(oldName);

        String newName = RandomData.getProfileName();
        enterName(newName);
        saveButton().click();

        // алерт показывается после того, как бэк сохранил имя, поэтому дашборд можно открывать сразу
        Selenide.confirm(MSG_UPDATED);

        Selenide.open("/dashboard");
        welcomeText().shouldHave(Condition.exactText("Welcome, " + newName + "!"));
    }

    // Отдельный тест на хедер: если хедер не обновляется (подозрение на дефект),
    // падает только он, а основной сценарий N-1 остаётся рабочим.
    @Test
    public void userNameInHeaderIsUpdatedAfterChangeTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        String oldName = setProfileName(user);

        authAsUser(user);
        openEditProfile(oldName);

        String newName = RandomData.getProfileName();
        enterName(newName);
        saveButton().click();

        Selenide.confirm(MSG_UPDATED);

        // без перезагрузки страницы: хедер обновляется через setUser после сохранения
        headerName().shouldHave(Condition.exactText(newName));
    }

    // ---------- N-2. Ошибка бэка при невалидном имени ----------
    // Перебор невалидных значений делают API-тесты, на UI один представитель.
    @Test
    public void userCanNotUpdateNameWithInvalidValueTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        String oldName = setProfileName(user);

        authAsUser(user);
        openEditProfile(oldName);

        String invalidName = "4444 4444";
        enterName(invalidName);
        saveButton().click();

        Selenide.confirm(MSG_BACKEND_INVALID_NAME);
        webdriver().shouldHave(WebDriverConditions.urlContaining("/edit-profile"));
        nameInput().shouldHave(Condition.exactValue(invalidName));

        softly.assertThat(UserSteps.getProfile(user).getName()).isEqualTo(oldName);

        // на дашборде имя осталось прежним
        Selenide.open("/dashboard");
        welcomeText().shouldHave(Condition.exactText("Welcome, " + oldName + "!"));
    }

    // ---------- N-3. Валидация на фронте (параметризованный) ----------
    public static Stream<Arguments> frontValidationData() {
        return Stream.of(
                Arguments.of("пустое поле", (UnaryOperator<String>) current -> "", MSG_INVALID_NAME),
                Arguments.of("только пробелы", (UnaryOperator<String>) current -> "   ", MSG_INVALID_NAME),
                Arguments.of("текущее имя", (UnaryOperator<String>) current -> current, MSG_SAME_NAME),
                Arguments.of("текущее имя с пробелами по краям",
                        (UnaryOperator<String>) current -> "  " + current + "  ", MSG_SAME_NAME));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("frontValidationData")
    public void userCanNotSaveNameRejectedByFrontendTest(String caseName, UnaryOperator<String> input,
                                                         String expectedMessage) {
        CreateUserRequestModel user = AdminSteps.createUser();
        String oldName = setProfileName(user);

        authAsUser(user);
        openEditProfile(oldName);

        enterName(input.apply(oldName));
        saveButton().click();

        Selenide.confirm(expectedMessage);
        webdriver().shouldHave(WebDriverConditions.urlContaining("/edit-profile"));

        // запрос не должен был уйти: имя на бэке прежнее
        softly.assertThat(UserSteps.getProfile(user).getName()).isEqualTo(oldName);
    }

    // ---------- N-4. Форма подставляет текущее имя ----------
    // Плейсхолдер виден только в пустом поле, поэтому проверяем его после очистки.
    @Test
    public void editProfileFormShowsCurrentNameTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        String oldName = setProfileName(user);

        authAsUser(user);
        openEditProfile(oldName);

        clearName();
        nameInput().shouldBe(Condition.empty);
        nameInput().shouldHave(Condition.attribute("placeholder", "Enter new name"));
    }

    // ---------- локаторы ----------
    // единственное текстовое поле на странице
    private SelenideElement nameInput() {
        return $("input[type='text']");
    }

    private SelenideElement saveButton() {
        return $$("button").findBy(Condition.text("Save Changes"));
    }

    private SelenideElement welcomeText() {
        return $(".welcome-text");
    }

    private SelenideElement headerName() {
        return $(".user-name");
    }

    // ---------- вспомогательные методы ----------

    // Страница при открытии запрашивает профиль и затем записывает имя в поле.
    // Пока поле не получило имя, вводить в него нельзя: поздний ответ затрёт введённое значение.
    // Поэтому после открытия ждём, пока в поле появится ожидаемое имя.
    private void openEditProfile(String expectedCurrentName) {
        Selenide.open("/edit-profile");
        nameInput().shouldHave(Condition.exactValue(expectedCurrentName));
    }

    // Очистка клавишами: setValue("") и clear() стирают текст в поле, но не создают событие ввода,
    // и React не обновляет своё состояние (в нём остаётся старое имя).
    // Браузер работает в контейнере Linux, поэтому здесь Ctrl, а не Cmd, даже если тесты запускаются с macOS.
    private void clearName() {
        nameInput().sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        nameInput().shouldBe(Condition.empty);
    }

    // ввод значения через клавиши: после очистки печатаем текст (пустое значение означает только очистку)
    private void enterName(String value) {
        clearName();
        if (!value.isEmpty()) {
            nameInput().sendKeys(value);
        }
    }

    // предшаг через API: задаём пользователю имя и возвращаем его
    private String setProfileName(CreateUserRequestModel user) {
        String name = RandomData.getProfileName();
        UserSteps.updateProfile(user, UpdateCustomerProfileRequestModel.builder().name(name).build());
        return name;
    }
}
