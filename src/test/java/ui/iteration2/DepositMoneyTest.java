package ui.iteration2;

import api.models.Account;
import api.models.CreateAccountResponseModel;
import api.models.CreateUserRequestModel;
import api.models.TransactionType;
import api.skelethon.steps.AdminSteps;
import api.skelethon.steps.UserSteps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ui.iteration1.BaseUITest;
import ui.pages.BankAlert;
import ui.pages.DepositPage;
import ui.pages.UserDashboard;
import ui.pages.components.AccountSelect;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;


public class DepositMoneyTest extends BaseUITest {

    private static final double INITIAL_BALANCE = 100;

    // ---------- D-1. Успешный депозит (параметризованный) ----------
    public static Stream<Arguments> depositAmountValidData() {
        return Stream.of(Arguments.of("50.5"), Arguments.of("5000"));
    }

    @MethodSource("depositAmountValidData")
    @ParameterizedTest
    public void userCanDepositMoneyTest(String amount) {
        // предшаги через API
        CreateUserRequestModel user = AdminSteps.createUser();
        CreateAccountResponseModel account1 = UserSteps.createAccount(user);
        CreateAccountResponseModel account2 = UserSteps.createAccount(user);
        UserSteps.deposit(user, account1.getId(), INITIAL_BALANCE);

        authAsUser(user);

        // целевое действие на UI, алерт и переход на дашборд
        new DepositPage().open()
                .waitForAccountsLoaded(2)
                .deposit(account1.getId(), amount)
                .checkAlertMessageAndAccept(BankAlert.DEPOSIT_SUCCESSFUL.format(amount, account1.getAccountNumber()))
                .getPage(UserDashboard.class)
                .shouldBeOpened();

        // проверки API: депозит ушёл именно на выбранный аккаунт
        double expectedBalance = BigDecimal.valueOf(INITIAL_BALANCE).add(new BigDecimal(amount)).doubleValue();
        List<Account> accounts = UserSteps.getAccountsList(user);
        Account acc1 = UserSteps.getAccountById(accounts, account1.getId());
        Account acc2 = UserSteps.getAccountById(accounts, account2.getId());

        softly.assertThat(acc1.getBalance()).isEqualTo(expectedBalance);
        softly.assertThat(acc1.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT).count()).isEqualTo(2L);
        softly.assertThat(acc2.getBalance()).isEqualTo(0.0);
        softly.assertThat(acc2.getTransactions()).isEmpty();

        // проверка UI: страница депозита после повторного открытия показывает новый баланс
        new DepositPage().open()
                .waitForAccountsLoaded(2)
                .accounts()
                .checkOption(account1.getId(), account1.getAccountNumber(), expectedBalance)
                .checkOption(account2.getId(), account2.getAccountNumber(), 0);
    }

    // ---------- D-2. Валидация суммы на фронте (параметризованный) ----------
    public static Stream<Arguments> invalidAmountData() {
        return Stream.of(
                Arguments.of("", BankAlert.DEPOSIT_INVALID_AMOUNT),
                Arguments.of("0", BankAlert.DEPOSIT_INVALID_AMOUNT),
                Arguments.of("-1", BankAlert.DEPOSIT_INVALID_AMOUNT),
                Arguments.of("5000.01", BankAlert.DEPOSIT_AMOUNT_TOO_BIG));
    }

    @ParameterizedTest
    @MethodSource("invalidAmountData")
    public void userCanNotDepositInvalidAmountTest(String amount, BankAlert expectedAlert) {
        CreateUserRequestModel user = AdminSteps.createUser();
        CreateAccountResponseModel account = UserSteps.createAccount(user);
        UserSteps.deposit(user, account.getId(), INITIAL_BALANCE);

        authAsUser(user);

        new DepositPage().open()
                .waitForAccountsLoaded(1)
                .deposit(account.getId(), amount)
                .checkAlertMessageAndAccept(expectedAlert)
                .shouldBeOpened();

        // запрос не должен был уйти: баланс и транзакции не изменились
        assertAccountUnchanged(user, account, INITIAL_BALANCE, 1);
    }

    // ---------- D-3. Аккаунт не выбран ----------
    @Test
    public void userCanNotDepositWithoutSelectedAccountTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        CreateAccountResponseModel account = UserSteps.createAccount(user);

        authAsUser(user);

        // ничего не выбираем и не вводим
        new DepositPage().open()
                .waitForAccountsLoaded(1)
                .clickDeposit()
                .checkAlertMessageAndAccept(BankAlert.DEPOSIT_SELECT_ACCOUNT);

        assertAccountUnchanged(user, account, 0, 0);
    }

    // ---------- D-4. Дропдаун показывает аккаунты и балансы ----------
    @Test
    public void depositPageShowsAccountsWithBalancesTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        CreateAccountResponseModel account1 = UserSteps.createAccount(user);
        CreateAccountResponseModel account2 = UserSteps.createAccount(user);
        UserSteps.deposit(user, account1.getId(), 100.5);

        authAsUser(user);

        new DepositPage().open()
                .waitForAccountsLoaded(2)
                .accounts()
                .checkAllOptions(
                        AccountSelect.optionText(account1.getAccountNumber(), 100.5),
                        AccountSelect.optionText(account2.getAccountNumber(), 0));
    }

    // ---------- D-5. Бэк отклонил депозит: пользователь видит общий алерт ----------
    // 0.001: фронт пропускает (больше 0 и не больше 5000), бэк отклоняет (минимум 0.01).
    // Мок не нужен: ошибка настоящая.
    @Test
    public void userSeesGenericErrorWhenBackendRejectsDepositTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        CreateAccountResponseModel account = UserSteps.createAccount(user);
        UserSteps.deposit(user, account.getId(), INITIAL_BALANCE);

        authAsUser(user);

        new DepositPage().open()
                .waitForAccountsLoaded(1)
                .deposit(account.getId(), "0.001")
                .checkAlertMessageAndAccept(BankAlert.DEPOSIT_FAILED)
                .shouldBeOpened();

        assertAccountUnchanged(user, account, INITIAL_BALANCE, 1);
    }
}
