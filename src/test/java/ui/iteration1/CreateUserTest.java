package ui.iteration1;

import api.generators.RandomModelGenerator;
import api.models.CreateUserRequestModel;
import api.models.CreateUserResponseModel;
import api.models.assertions.ModelAssertions;
import api.skelethon.steps.AdminSteps;
import com.codeborne.selenide.Condition;
import org.junit.jupiter.api.Test;
import ui.pages.AdminPanel;
import ui.pages.BankAlert;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateUserTest extends BaseUITest {

    @Test
    public void adminCanCreateUserTest() {
        // ШАГ 1: админ залогинился в банке
        CreateUserRequestModel admin = CreateUserRequestModel.getAdmin();
        authAsUser(admin);

        // ШАГ 2: админ создает юзера в банке
        CreateUserRequestModel newUser = RandomModelGenerator.generate(CreateUserRequestModel.class);

        // ШАГ 3: проверка, что алерт "✅ User created successfully!"
        // ШАГ 4: проверка, что юзер отображается на UI
        new AdminPanel().open().createUser(newUser.getUsername(), newUser.getPassword())
                .checkAlertMessageAndAccept(BankAlert.USER_CREATED_SUCCESSFULLY.getMessage())
                .getAllUsers().findBy(Condition.exactText(newUser.getUsername() + "\nUSER")).shouldBe(Condition.visible);

        // ШАГ 5: проверка, что юзер создан на API
        CreateUserResponseModel createdUser = AdminSteps.getAllUsers().stream()
                .filter(user -> user.getUsername().equals(newUser.getUsername()))
                .findFirst().get();

        ModelAssertions.assertThatModels(newUser, createdUser).match();
    }

    @Test
    public void adminCannotCreateUserWithInvalidDataTest() {
        // ШАГ 1: админ залогинился в банке
        CreateUserRequestModel admin = CreateUserRequestModel.getAdmin();
        authAsUser(admin);

        // ШАГ 2: админ создает юзера в банке
        CreateUserRequestModel newUser = RandomModelGenerator.generate(CreateUserRequestModel.class);
        newUser.setUsername("a");


        // ШАГ 3: проверка, что алерт "Username must be between 3 and 15 characters"
        // ШАГ 4: проверка, что юзер НЕ отображается на UI
        new AdminPanel().open().createUser(newUser.getUsername(), newUser.getPassword())
                .checkAlertMessageAndAccept(BankAlert.USERNAME_MUST_BE_BETWEEN_3_AND_15_CHARACTERS.getMessage())
                .getAllUsers().findBy(Condition.exactText(newUser.getUsername() + "\nUSER")).shouldNotBe(Condition.exist);


        // ШАГ 5: проверка, что юзер НЕ создан на API
        long usersWithSameUsernameAsNewUser = AdminSteps.getAllUsers().stream()
                .filter(user -> user.getUsername().equals(newUser.getUsername()))
                .count();

        assertThat(usersWithSameUsernameAsNewUser).isZero();
    }
}
