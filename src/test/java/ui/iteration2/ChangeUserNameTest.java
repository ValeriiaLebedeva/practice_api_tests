package ui.iteration2;

import api.generators.RandomData;
import api.models.CreateUserRequestModel;
import api.skelethon.steps.AdminSteps;
import api.skelethon.steps.UserSteps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ui.iteration1.BaseUITest;
import ui.pages.BankAlert;
import ui.pages.EditProfilePage;
import ui.pages.UserDashboard;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class ChangeUserNameTest extends BaseUITest {

    // ---------- N-1. Успешное изменение имени ----------
    @Test
    public void userCanUpdateNameTest() {
        // предшаги через API: у пользователя уже есть имя
        CreateUserRequestModel user = AdminSteps.createUser();
        String oldName = UserSteps.setRandomProfileName(user);

        authAsUser(user);

        // целевое действие на UI
        String newName = RandomData.getProfileName();
        new EditProfilePage().open()
                .waitForCurrentName(oldName)
                .enterName(newName)
                .checkNameInputValue(newName)
                .clickSave()
                .checkAlertMessageAndAccept(BankAlert.NAME_UPDATED_SUCCESSFULLY);

        // проверка API
        softly.assertThat(UserSteps.getProfile(user).getName()).isEqualTo(newName);

        // проверка UI: форма после повторного открытия подставляет новое имя
        new EditProfilePage().open()
                .waitForCurrentName(newName);
    }

    // ---------- N-1б. После сохранения имени происходит переход на /dashboard ----------
    // В коде переход стоит последним шагом, после повторного запроса профиля и setUser.
    // Если любой из них падает, пользователь остаётся на /edit-profile. Тест проверяет только переход.
    @Test
    public void userIsRedirectedToDashboardAfterNameChangeTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        String oldName = UserSteps.setRandomProfileName(user);

        authAsUser(user);

        new EditProfilePage().open()
                .waitForCurrentName(oldName)
                .changeName(RandomData.getProfileName())
                .checkAlertMessageAndAccept(BankAlert.NAME_UPDATED_SUCCESSFULLY)
                .getPage(UserDashboard.class)
                .shouldBeOpened();
    }

    // ---------- N-1в. Новое имя отображается на дашборде ----------
    // Дашборд открываем напрямую и не ждём перехода, поэтому тест не зависит от редиректа.
    @Test
    public void newNameIsShownOnDashboardTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        String oldName = UserSteps.setRandomProfileName(user);

        authAsUser(user);

        String newName = RandomData.getProfileName();
        new EditProfilePage().open()
                .waitForCurrentName(oldName)
                .changeName(newName)
                // алерт показывается после того, как бэк сохранил имя, поэтому дашборд можно открывать сразу
                .checkAlertMessageAndAccept(BankAlert.NAME_UPDATED_SUCCESSFULLY)
                .getPage(UserDashboard.class)
                .open()
                .checkWelcomeName(newName);
    }

    // Отдельный тест на хедер: если хедер не обновляется (подозрение на дефект),
    // падает только он, а основной сценарий N-1 остаётся рабочим.
    @Test
    public void userNameInHeaderIsUpdatedAfterChangeTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        String oldName = UserSteps.setRandomProfileName(user);

        authAsUser(user);

        String newName = RandomData.getProfileName();
        // без перезагрузки страницы: хедер обновляется через setUser после сохранения
        new EditProfilePage().open()
                .waitForCurrentName(oldName)
                .changeName(newName)
                .checkAlertMessageAndAccept(BankAlert.NAME_UPDATED_SUCCESSFULLY)
                .checkHeaderName(newName);
    }

    // ---------- N-2. Ошибка бэка при невалидном имени ----------
    // Перебор невалидных значений делают API-тесты, на UI один представитель.
    @Test
    public void userCanNotUpdateNameWithInvalidValueTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        String oldName = UserSteps.setRandomProfileName(user);

        authAsUser(user);

        String invalidName = "4444 4444";
        new EditProfilePage().open()
                .waitForCurrentName(oldName)
                .changeName(invalidName)
                .checkAlertMessageAndAccept(BankAlert.NAME_MUST_CONTAIN_TWO_WORDS)
                .shouldBeOpened()
                .checkNameInputValue(invalidName)
                .getPage(UserDashboard.class)
                .open()
                .checkWelcomeName(oldName);

        softly.assertThat(UserSteps.getProfile(user).getName()).isEqualTo(oldName);
    }

    // ---------- N-3. Валидация на фронте (параметризованный) ----------
    public static Stream<Arguments> frontValidationData() {
        return Stream.of(
                Arguments.of("пустое поле", (UnaryOperator<String>) current -> "", BankAlert.NAME_INVALID),
                Arguments.of("только пробелы", (UnaryOperator<String>) current -> "   ", BankAlert.NAME_INVALID),
                Arguments.of("текущее имя", (UnaryOperator<String>) current -> current, BankAlert.NAME_SAME_AS_CURRENT),
                Arguments.of("текущее имя с пробелами по краям",
                        (UnaryOperator<String>) current -> "  " + current + "  ", BankAlert.NAME_SAME_AS_CURRENT));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("frontValidationData")
    public void userCanNotSaveNameRejectedByFrontendTest(String caseName, UnaryOperator<String> inputFor,
                                                         BankAlert expectedAlert) {
        CreateUserRequestModel user = AdminSteps.createUser();
        String oldName = UserSteps.setRandomProfileName(user);

        authAsUser(user);

        new EditProfilePage().open()
                .waitForCurrentName(oldName)
                .changeName(inputFor.apply(oldName))
                .checkAlertMessageAndAccept(expectedAlert)
                .shouldBeOpened();

        // запрос не должен был уйти: имя на бэке прежнее
        softly.assertThat(UserSteps.getProfile(user).getName()).isEqualTo(oldName);
    }

    // ---------- N-4. Форма подставляет текущее имя ----------
    // Плейсхолдер виден только в пустом поле, поэтому проверяем его после очистки.
    @Test
    public void editProfileFormShowsCurrentNameTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        String oldName = UserSteps.setRandomProfileName(user);

        authAsUser(user);

        new EditProfilePage().open()
                .waitForCurrentName(oldName)
                .clearName()
                .checkNameInputIsEmpty()
                .checkNamePlaceholder("Enter new name");
    }
}
