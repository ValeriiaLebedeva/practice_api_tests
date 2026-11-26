package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.Keys;

import java.time.Duration;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class EditProfilePage extends BasePage<EditProfilePage> {

    // единственное текстовое поле на странице
    private final SelenideElement nameInput = $("input[type='text']");
    private final SelenideElement saveButton = $$("button").findBy(Condition.text("Save Changes"));

    @Override
    public String url() {
        return "/edit-profile";
    }

    // Страница при открытии запрашивает профиль и затем записывает имя в поле.
    // Пока поле не получило имя, вводить в него нельзя: поздний ответ затрёт введённое значение.
    // Если за 4 секунды имя не появилось, фронтенд не подтянул профиль: обновляем страницу один раз.
    public EditProfilePage waitForCurrentName(String currentName) {
        try {
            nameInput.shouldHave(Condition.exactValue(currentName), Duration.ofSeconds(4));
        } catch (AssertionError e) {
            Selenide.refresh();
            nameInput.shouldHave(Condition.exactValue(currentName));
        }
        return this;
    }

    // Очистка клавишами: setValue("") и clear() стирают текст в поле, но не создают событие ввода,
    // и React не обновляет своё состояние (в нём остаётся старое имя).
    // Браузер работает в контейнере Linux, поэтому здесь Ctrl, а не Cmd, даже если тесты запускаются с macOS.
    public EditProfilePage clearName() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            nameInput.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
            try {
                nameInput.shouldBe(Condition.empty, Duration.ofSeconds(2));
                return this;
            } catch (AssertionError e) {
                if (attempt == 3) {
                    throw e; // три попытки не помогли: это уже не случайный сбой клавиш
                }
            }
        }
        return this;
    }

    // ввод через клавиши: пустое значение означает только очистку
    public EditProfilePage enterName(String name) {
        clearName();
        if (!name.isEmpty()) {
            nameInput.sendKeys(name);
        }
        return this;
    }

    public EditProfilePage clickSave() {
        saveButton.click();
        return this;
    }

    public EditProfilePage changeName(String newName) {
        return enterName(newName).clickSave();
    }

    public EditProfilePage checkNameInputValue(String expectedName) {
        nameInput.shouldHave(Condition.exactValue(expectedName));
        return this;
    }

    public EditProfilePage checkNameInputIsEmpty() {
        nameInput.shouldBe(Condition.empty);
        return this;
    }

    public EditProfilePage checkNamePlaceholder(String expectedPlaceholder) {
        nameInput.shouldHave(Condition.attribute("placeholder", expectedPlaceholder));
        return this;
    }
}
