package iteration2;

import iteration1.BaseTest;
import models.*;
import models.assertions.ModelAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.ValidatedCrudRequester;
import requests.skelethon.steps.AdminSteps;
import requests.skelethon.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;
import java.util.stream.Stream;

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

        DepositMoneyRequestModel depositMoneyRequestModel = DepositMoneyRequestModel.builder()
                .id(createAccountResponseModel.getId())
                .balance(deposit)
                .build();

        DepositMoneyResponseModel depositMoneyResponseModel =
                new ValidatedCrudRequester<DepositMoneyResponseModel>(
                        RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                        Endpoint.ACCOUNTS_DEPOSIT,
                        ResponseSpecs.requestReturnsOK())
                        .post(depositMoneyRequestModel);

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

        // проверяем баланс аккаунта и количество транзакций
        softly.assertThat(account
                .getBalance()).isEqualTo(deposit);
        softly.assertThat(account
                .getTransactions()
                .size()).isEqualTo(1);
    }

    public static Stream<Arguments> depositAmountInvalidData() {
        return Stream.of(Arguments.of(-0.01, "Deposit amount must be at least 0.01"),
                Arguments.of(0.000001, "Deposit amount must be at least 0.01"),
                Arguments.of(5000.01, "Deposit amount cannot exceed 5000"));
    }

    @MethodSource("depositAmountInvalidData")
    @ParameterizedTest
    public void userCanNotDepositInvalidMoneyAccountExistTest(double deposit, String message) {

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
    }
}
