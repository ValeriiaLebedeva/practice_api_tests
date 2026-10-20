package ui.iteration2;

import com.codeborne.selenide.*;
import models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.steps.AdminSteps;
import requests.skelethon.steps.UserSteps;
import ui.iteration1.BaseUITest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static com.codeborne.selenide.Selenide.*;

public class DepositMoneyTest extends BaseUITest {

    private static final double INITIAL_BALANCE = 100;

    private static final String PLACEHOLDER_OPTION = "-- Choose an account --";
    private static final String MSG_INVALID_AMOUNT = "❌ Please enter a valid amount.";
    private static final String MSG_AMOUNT_TOO_BIG = "❌ Please deposit less or equal to 5000$.";
    private static final String MSG_SELECT_ACCOUNT = "❌ Please select an account.";
    private static final String MSG_DEPOSIT_FAILED = "❌ Failed to deposit. Please try again.";


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
        openDepositPage(2);

        // целевое действие на UI
        accountSelect().selectOptionByValue(String.valueOf(account1.getId()));
        amountInput().setValue(amount);
        depositButton().click();

        // проверки UI: алерт и переход на дашборд
        Selenide.confirm("✅ Successfully deposited $" + amount
                + " to account " + account1.getAccountNumber() + "!");
        webdriver().shouldHave(WebDriverConditions.urlContaining("/dashboard"));

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
        openDepositPage(2);
        optionOf(account1).shouldHave(Condition.exactText(optionText(account1, expectedBalance)));
        optionOf(account2).shouldHave(Condition.exactText(optionText(account2, 0)));
    }


    // ---------- D-2. Валидация суммы на фронте (параметризованный) ----------
    public static Stream<Arguments> invalidAmountData() {
        return Stream.of(
                Arguments.of("", MSG_INVALID_AMOUNT),
                Arguments.of("0", MSG_INVALID_AMOUNT),
                Arguments.of("-1", MSG_INVALID_AMOUNT),
                Arguments.of("5000.01", MSG_AMOUNT_TOO_BIG));
    }

    @ParameterizedTest
    @MethodSource("invalidAmountData")
    public void userCanNotDepositInvalidAmountTest(String amount, String expectedMessage) {
        CreateUserRequestModel user = AdminSteps.createUser();
        CreateAccountResponseModel account = UserSteps.createAccount(user);
        UserSteps.deposit(user, account.getId(), INITIAL_BALANCE);

        authAsUser(user);
        openDepositPage(1);

        accountSelect().selectOptionByValue(String.valueOf(account.getId()));
        amountInput().setValue(amount);
        depositButton().click();

        Selenide.confirm(expectedMessage);
        webdriver().shouldHave(WebDriverConditions.urlContaining("/deposit"));

        // запрос не должен был уйти: баланс и транзакции не изменились
        assertBalanceNotChanged(user, account, INITIAL_BALANCE, 1);
    }


    // ---------- D-3. Аккаунт не выбран ----------
    @Test
    public void userCanNotDepositWithoutSelectedAccountTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        CreateAccountResponseModel account = UserSteps.createAccount(user);

        authAsUser(user);
        openDepositPage(1);

        // ничего не выбираем и не вводим
        depositButton().click();

        Selenide.confirm(MSG_SELECT_ACCOUNT);

        assertBalanceNotChanged(user, account, 0, 0);
    }

    // ---------- D-4. Дропдаун показывает аккаунты и балансы ----------
    @Test
    public void depositPageShowsAccountsWithBalancesTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        CreateAccountResponseModel account1 = UserSteps.createAccount(user);
        CreateAccountResponseModel account2 = UserSteps.createAccount(user);
        UserSteps.deposit(user, account1.getId(), 100.5);

        authAsUser(user);
        openDepositPage(2);

        accountSelect().$$("option").shouldHave(CollectionCondition.exactTexts(
                PLACEHOLDER_OPTION,
                optionText(account1, 100.5),
                optionText(account2, 0)));
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
        openDepositPage(1);

        accountSelect().selectOptionByValue(String.valueOf(account.getId()));
        amountInput().setValue("0.001");
        depositButton().click();

        Selenide.confirm(MSG_DEPOSIT_FAILED);
        webdriver().shouldHave(WebDriverConditions.urlContaining("/deposit"));

        assertBalanceNotChanged(user, account, INITIAL_BALANCE, 1);
    }


    // вспомогательные методы
    private SelenideElement accountSelect() {
        return $(".account-selector");
    }

    private SelenideElement amountInput() {
        return $(".deposit-input");
    }

    // у кнопки Deposit единственный класс btn-primary на странице (Home это btn-outline-primary)
    private SelenideElement depositButton() {
        return $("button.btn-primary");
    }

    private SelenideElement optionOf(CreateAccountResponseModel account) {
        return accountSelect().$("option[value='" + account.getId() + "']");
    }

    // аккаунты подгружаются асинхронно: ждём плейсхолдер и все аккаунты пользователя
    private void openDepositPage(int accountsCount) {
        Selenide.open("/deposit");
        accountSelect().$$("option").shouldHave(CollectionCondition.size(accountsCount + 1));
    }

    // формат пункта дропдауна: ACC1 (Balance: $150.50)
    private static String optionText(CreateAccountResponseModel account, double balance) {
        return account.getAccountNumber() + " (Balance: $" + String.format(Locale.US, "%.2f", balance) + ")";
    }


    private void assertBalanceNotChanged(CreateUserRequestModel user, CreateAccountResponseModel account,
                                         double expectedBalance, int expectedTransactions) {
        Account acc = UserSteps.getAccountById(UserSteps.getAccountsList(user), account.getId());
        softly.assertThat(acc.getBalance()).isEqualTo(expectedBalance);
        softly.assertThat(acc.getTransactions().size()).isEqualTo(expectedTransactions);
    }

}
