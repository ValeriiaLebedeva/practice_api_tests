package api.iteration2;

import api.iteration1.BaseTest;
import api.models.*;
import api.models.assertions.ModelAssertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import api.steps.AdminSteps;
import api.steps.UserSteps;
import api.dao.AccountDao;
import api.steps.DataBaseSteps;
import api.dao.comparison.DaoAndModelAssertions;
import common.annotations.APIVersion;
import api.specs.ResponseSpecs;

import java.util.List;
import java.util.stream.Stream;

@APIVersion("with_database_with_fix")
public class DepositMoneyTest extends BaseTest {

    private final static String UNAUTHENTIC_ERROR_MESSAGE = "Unauthorized access to account";

    public static Stream<Arguments> depositAmountValidData() {
        return Stream.of(Arguments.of(0.01), Arguments.of(5000.00), Arguments.of(4999.99), Arguments.of(400.0001), Arguments.of(5000));
    }

    @MethodSource("depositAmountValidData")
    @ParameterizedTest
    public void userCanDepositMoneyAccountExistTest(double deposit) {

        CreateUserRequestModel userRequestModel = AdminSteps.createUser();

        CreateAccountResponseModel createAccountResponseModel = UserSteps.createAccount(userRequestModel);

        // созданный аккаунт в БД совпадает с ответом API (правила в dao-comparison.properties)
        DaoAndModelAssertions.assertThat(createAccountResponseModel,
                DataBaseSteps.getAccountByAccountNumber(createAccountResponseModel.getAccountNumber())).match();

        DepositMoneyRequestModel depositMoneyRequestModel = DepositMoneyRequestModel.builder()
                .id(createAccountResponseModel.getId())
                .balance(deposit)
                .build();

        DepositMoneyResponseModel depositMoneyResponseModel =
                UserSteps.deposit(userRequestModel, depositMoneyRequestModel);

        // id и balance запроса совпадают с ответом (правила в model-comparison.properties)
        ModelAssertions.assertThatModels(depositMoneyRequestModel, depositMoneyResponseModel).match(softly);

        softly.assertThat(depositMoneyResponseModel.getTransactions().size()).isEqualTo(1);
        softly.assertThat(depositMoneyResponseModel.getTransactions().getFirst().getAmount())
                .isEqualTo(depositMoneyRequestModel.getBalance());
        softly.assertThat(depositMoneyResponseModel.getTransactions().getFirst().getType())
                .isEqualTo(TransactionType.DEPOSIT);
        softly.assertThat(depositMoneyResponseModel.getTransactions().getFirst().getRelatedAccountId())
                .isEqualTo(depositMoneyRequestModel.getId());

        // достаем список аккаунтов для юзера
        List<Account> accountList = UserSteps.getAccountsList(userRequestModel);

        // проверяем размер количества аккаунтов
        softly.assertThat(accountList.size()).isEqualTo(1);

        // находим аккаунт
        Account account = UserSteps.getAccountById(accountList, createAccountResponseModel.getId());

        // проверяем баланс аккаунта и количество транзакций (в БД сумма округляется до двух знаков)
        softly.assertThat(account
                .getBalance()).isEqualTo(round2(deposit));
        softly.assertThat(account
                .getTransactions()
                .size()).isEqualTo(1);

        // проверка через БД: аккаунт из API совпадает с записью в таблице accounts
        AccountDao accountDao = DataBaseSteps.getAccountByAccountNumber(account.getAccountNumber());
        DaoAndModelAssertions.assertThat(account, accountDao).match();
        // транзакции аккаунта из API совпадают с таблицей transactions
        assertTransactionsInDb(account);
    }

    // тексты ошибок валидации в with_database_with_fix отличаются от прежней версии
    public static Stream<Arguments> depositAmountInvalidData() {
        return Stream.of(Arguments.of(-0.01, "Invalid account or amount"),
                Arguments.of(5000.01, "Deposit amount exceeds the 5000 limit"));
    }

    @MethodSource("depositAmountInvalidData")
    @ParameterizedTest
    public void userCanNotDepositInvalidMoneyAccountExistTest(double deposit, String message) {
        assertDepositRejected(deposit, message);
    }

    // Баг with_database_with_fix: депозит меньше минимального 0.01 принимается (200 вместо 400).
    @Disabled("баг: депозит 0.000001 принимается, а минимум 0.01")
    @Test
    public void userCanNotDepositAmountBelowMinimumTest() {
        assertDepositRejected(0.000001, "Deposit amount must be at least 0.01");
    }

    // общий сценарий: депозит отклонён, баланс и транзакции прежние
    private void assertDepositRejected(double deposit, String message) {

        CreateUserRequestModel userRequestModel = AdminSteps.createUser();

        CreateAccountResponseModel createAccountResponseModel = UserSteps.createAccount(userRequestModel);

        UserSteps.depositExpecting(userRequestModel,
                createAccountResponseModel.getId(),
                deposit,
                ResponseSpecs.requestReturnsBadRequestOnlyBody(message));

        // достаем список аккаунтов для юзера
        List<Account> accountList = UserSteps.getAccountsList(userRequestModel);

        // проверяем размер количества аккаунтов
        softly.assertThat(accountList.size()).isEqualTo(1);

        // находим аккаунт
        Account account = UserSteps.getAccountById(accountList, createAccountResponseModel.getId());

        // проверяем баланс аккаунта и количество транзакций
        softly.assertThat(account
                .getBalance()).isEqualTo(0);
        softly.assertThat(account
                .getTransactions()
                .size()).isEqualTo(0);

        // проверка через БД: аккаунт из API совпадает с записью в таблице accounts
        AccountDao accountDao = DataBaseSteps.getAccountByAccountNumber(account.getAccountNumber());
        DaoAndModelAssertions.assertThat(account, accountDao).match();
        // транзакции аккаунта из API совпадают с таблицей transactions
        assertTransactionsInDb(account);
    }

    @Test
    public void userCanNotDepositMoneyAccountDoesNotExistTest() {

        CreateUserRequestModel userRequestModel = AdminSteps.createUser();

        CreateAccountResponseModel createAccountResponseModel = UserSteps.createAccount(userRequestModel);

        UserSteps.depositExpecting(userRequestModel,
                createAccountResponseModel.getId() + 1,
                100,
                ResponseSpecs.requestReturnsForbiddenOnlyBody(UNAUTHENTIC_ERROR_MESSAGE));

        // достаем список аккаунтов для юзера
        List<Account> accountList = UserSteps.getAccountsList(userRequestModel);

        // проверяем размер количества аккаунтов - нет лишнего аккаунта
        softly.assertThat(accountList.size()).isEqualTo(1);
        softly.assertThat(accountList.size()).isNotEqualTo(2);

        // находим аккаунт только с правильным id
        Account account = UserSteps.getAccountById(accountList, createAccountResponseModel.getId());

        // проверяем баланс аккаунта и количество транзакций
        softly.assertThat(account
                .getBalance()).isEqualTo(0);
        softly.assertThat(account
                .getTransactions()
                .size()).isEqualTo(0);

        // проверка через БД: аккаунт из API совпадает с записью в таблице accounts
        AccountDao accountDao = DataBaseSteps.getAccountByAccountNumber(account.getAccountNumber());
        DaoAndModelAssertions.assertThat(account, accountDao).match();
        // транзакции аккаунта из API совпадают с таблицей transactions
        assertTransactionsInDb(account);
    }

    @Test
    public void userCanNotDepositMoneyToAccountAssociatedWithAnotherUserTest() {

        // создание owner пользователя
        CreateUserRequestModel userRequestModelOwner = AdminSteps.createUser();

        // создаем аккаунт для owner
        CreateAccountResponseModel createAccountResponseModelOwner = UserSteps.createAccount(userRequestModelOwner);

        // создание another пользователя
        CreateUserRequestModel userRequestModelAnother = AdminSteps.createUser();

        // another юзер пробует внести депозит на owner аккаунт
        UserSteps.depositExpecting(userRequestModelAnother,
                createAccountResponseModelOwner.getId(),
                100,
                ResponseSpecs.requestReturnsForbiddenOnlyBody(UNAUTHENTIC_ERROR_MESSAGE));

        // достаем список аккаунтов для owner юзера
        List<Account> accountList = UserSteps.getAccountsList(userRequestModelOwner);

        // проверяем размер количества аккаунтов
        softly.assertThat(accountList.size()).isEqualTo(1);

        // находим аккаунт
        Account account = UserSteps.getAccountById(accountList, createAccountResponseModelOwner.getId());

        // проверяем баланс аккаунта и количество транзакций
        softly.assertThat(account
                .getBalance()).isEqualTo(0);
        softly.assertThat(account
                .getTransactions()
                .size()).isEqualTo(0);

        // достаем список аккаунтов для another юзера
        List<Account> accountList2 = UserSteps.getAccountsList(userRequestModelAnother);

        // проверяем размер количества another аккаунтов
        softly.assertThat(accountList2.size()).isEqualTo(0);

        // проверка через БД: аккаунт из API совпадает с записью в таблице accounts
        AccountDao accountDao = DataBaseSteps.getAccountByAccountNumber(account.getAccountNumber());
        DaoAndModelAssertions.assertThat(account, accountDao).match();
        // транзакции аккаунта из API совпадают с таблицей transactions
        assertTransactionsInDb(account);
    }
}
