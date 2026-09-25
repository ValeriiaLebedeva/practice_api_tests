package iteration2;

import generators.RandomData;
import iteration1.BaseTest;
import models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.DepositMoneyRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

public class DepositMoneyTest extends BaseTest {

    private final static String UNAUTHENTIC_ERROR_MESSAGE = "Unauthorized access to account";

    public static Stream<Arguments> depositAmountValidData() {
        return Stream.of(Arguments.of(0.01), Arguments.of(5000.00), Arguments.of(4999.99), Arguments.of(400.0001), Arguments.of(5000));
    }

    @MethodSource("depositAmountValidData")
    @ParameterizedTest
    public void userCanDepositMoneyAccountExistTest(double deposit) {

        CreateUserRequestModel userRequestModel = CreateUserRequestModel.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .execute(userRequestModel);

        CreateAccountResponseModel createAccountResponseModel =
                new CreateAccountRequester(RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                        ResponseSpecs.entityWasCreated())
                        .execute(null).extract().as(CreateAccountResponseModel.class);

        DepositMoneyRequestModel depositMoneyRequestModel = DepositMoneyRequestModel.builder()
                .id(createAccountResponseModel.getId())
                .balance(deposit)
                .build();

        DepositMoneyResponseModel depositMoneyResponseModel = new DepositMoneyRequester(RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsOK()).execute(depositMoneyRequestModel)
                .extract().as(DepositMoneyResponseModel.class);

        softly.assertThat(depositMoneyRequestModel.getBalance()).isEqualTo(depositMoneyResponseModel.getBalance());
        softly.assertThat(depositMoneyResponseModel.getTransactions().size()).isEqualTo(1);
        softly.assertThat(depositMoneyResponseModel.getTransactions().getFirst().getAmount())
                .isEqualTo(depositMoneyRequestModel.getBalance());
        softly.assertThat(depositMoneyResponseModel.getTransactions().getFirst().getType())
                .isEqualTo(TransactionType.DEPOSIT);
        softly.assertThat(depositMoneyResponseModel.getTransactions().getFirst().getRelatedAccountId())
                .isEqualTo(depositMoneyRequestModel.getId());
    }

    public static Stream<Arguments> depositAmountInvalidData() {
        return Stream.of(Arguments.of(-0.01, "Deposit amount must be at least 0.01"),
                Arguments.of(0.000001, "Deposit amount must be at least 0.01"),
                Arguments.of(5000.01, "Deposit amount cannot exceed 5000"));
    }

    @MethodSource("depositAmountInvalidData")
    @ParameterizedTest
    public void userCanNotDepositInvalidMoneyAccountExistTest(double deposit, String message) {

        CreateUserRequestModel userRequestModel = CreateUserRequestModel.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .execute(userRequestModel);

        CreateAccountResponseModel createAccountResponseModel =
                new CreateAccountRequester(RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                        ResponseSpecs.entityWasCreated())
                        .execute(null).extract().as(CreateAccountResponseModel.class);

        DepositMoneyRequestModel depositMoneyRequestModel = DepositMoneyRequestModel.builder()
                .id(createAccountResponseModel.getId())
                .balance(deposit)
                .build();

        new DepositMoneyRequester(RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsBadRequestOnlyBody(message)).execute(depositMoneyRequestModel);
    }

    @Test
    public void userCanNotDepositMoneyAccountDoesNotExistTest() {

        CreateUserRequestModel userRequestModel = CreateUserRequestModel.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .execute(userRequestModel);

        CreateAccountResponseModel createAccountResponseModel =
                new CreateAccountRequester(RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                        ResponseSpecs.entityWasCreated())
                        .execute(null).extract().as(CreateAccountResponseModel.class);

        DepositMoneyRequestModel depositMoneyRequestModel = DepositMoneyRequestModel.builder()
                .id(createAccountResponseModel.getId() + 1)
                .balance(100)
                .build();

        new DepositMoneyRequester(RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsForbiddenOnlyBody(UNAUTHENTIC_ERROR_MESSAGE)).execute(depositMoneyRequestModel);

    }

    @Test
    public void userCanNotDepositMoneyToAccountAssociatedWithAnotherUserTest() {

        // создание owner пользователя
        CreateUserRequestModel userRequestModelOwner = CreateUserRequestModel.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .execute(userRequestModelOwner);

        // создаем аккаунт для owner
        CreateAccountResponseModel createAccountResponseModelOwner =
                new CreateAccountRequester(RequestSpecs.authAsUser(userRequestModelOwner.getUsername(), userRequestModelOwner.getPassword()),
                        ResponseSpecs.entityWasCreated())
                        .execute(null).extract().as(CreateAccountResponseModel.class);

        // создание another пользователя
        CreateUserRequestModel userRequestModelAnother = CreateUserRequestModel.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .execute(userRequestModelAnother);

        // another юзер пробует внести депозит на owner аккаунт
        DepositMoneyRequestModel depositMoneyRequestModel = DepositMoneyRequestModel.builder()
                .id(createAccountResponseModelOwner.getId())
                .balance(100)
                .build();

        new DepositMoneyRequester(RequestSpecs.authAsUser(userRequestModelAnother.getUsername(), userRequestModelAnother.getPassword()),
                ResponseSpecs.requestReturnsForbiddenOnlyBody(UNAUTHENTIC_ERROR_MESSAGE)).execute(depositMoneyRequestModel);
    }
}
