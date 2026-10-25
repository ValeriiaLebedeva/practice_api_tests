package ui.iteration1;

import api.models.Account;
import api.models.CreateUserRequestModel;
import api.skelethon.steps.AdminSteps;
import api.skelethon.steps.UserSteps;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.UserDashboard;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateAccountTest extends BaseUITest {


    @Test
    public void userCanCreateAccountTest() {
        // ШАГИ ПО НАСТРОЙКЕ ОКРУЖЕНИЯ
        // ШАГ 1: админ логинится в банке
        // ШАГ 2: админ создает юзера
        // ШАГ 3: юзер логинится в банке

        CreateUserRequestModel user = AdminSteps.createUser();
        authAsUser(user);

        // ШАГИ ТЕСТА
        // ШАГ 4: юзер создает аккаунт
        new UserDashboard().open().createNewAccount();

        // ШАГ 5: проверка, что аккаунт создался на UI
        // ШАГ 6: проверка, что аккаунт был создан на API
        List<Account> createdAccounts = UserSteps.getAccountsList(user);
        assertThat(createdAccounts).hasSize(1);

        new UserDashboard().checkAlertMessageAndAccept
                (BankAlert.NEW_ACCOUNT_CREATED.getMessage() + createdAccounts.getFirst().getAccountNumber());

        assertThat(createdAccounts.getFirst().getBalance()).isZero();
    }
}
