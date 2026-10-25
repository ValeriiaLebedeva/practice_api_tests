package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.Keys;

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
    public EditProfilePage waitForCurrentName(String currentName) {
        nameInput.shouldHave(Condition.exactValue(currentName));
        return this;
    }

    // Очистка клавишами: setValue("") и clear() стирают текст в поле, но не создают событие ввода,
    // и React не обновляет своё состояние (в нём остаётся старое имя).
    // Браузер работает в контейнере Linux, поэтому здесь Ctrl, а не Cmd, даже если тесты запускаются с macOS.
    public EditProfilePage clearName() {
        nameInput.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        nameInput.shouldBe(Condition.empty);
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
