package ui.iteration1;

import api.iteration1.BaseTest;
import api.models.Account;
import api.models.CreateAccountResponseModel;
import api.models.CreateUserRequestModel;
import api.steps.UserSteps;
import com.codeborne.selenide.Selenide;
import common.extensions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;

// Все расширения подключены здесь и срабатывают для каждого теста наследников.
// Аннотации на тестах (@UiEnvironment, @Browsers, @AdminSession, @UserSession, @WithAccount, @WithProfileName)
// только метки: каждое расширение проверяет, есть ли его метка, и если нет, ничего не делает.
// Порядок важен: окружение -> проверка браузера -> админ -> пользователь -> предусловия пользователя.
@ExtendWith(UiEnvironmentExtension.class)
@ExtendWith(BrowserMatchExtension.class)
@ExtendWith(AdminSessionExtension.class)
@ExtendWith(UserSessionExtension.class)
@ExtendWith(WithAccountExtension.class)
@ExtendWith(WithProfileNameExtension.class)
public abstract class BaseUITest extends BaseTest {

    // чистое состояние браузера для каждого теста: иначе остаётся токен прошлого теста
    @AfterEach
    public void closeBrowser() {
        Selenide.closeWebDriver();
    }

    // проверка по API: запрос не должен был уйти, баланс и число транзакций не изменились
    protected void assertAccountUnchanged(CreateUserRequestModel user, CreateAccountResponseModel account,
                                          double expectedBalance, int expectedTransactions) {
        Account acc = UserSteps.getAccountById(UserSteps.getAccountsList(user), account.getId());
        softly.assertThat(acc.getBalance()).isEqualTo(expectedBalance);
        softly.assertThat(acc.getTransactions().size()).isEqualTo(expectedTransactions);
    }
}

