package iteration2;

import generators.RandomData;
import io.restassured.common.mapper.TypeRef;
import iteration1.BaseTest;
import models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;
import java.util.stream.Stream;

public class TransferMoneyTest extends BaseTest {

    private final static String SUCCESSFUL_MESSAGE = "Transfer successful";
    private final static String BAD_REQUEST_MESSAGE_INVALID_TRANSFER = "Invalid transfer: insufficient funds or invalid accounts";
    private final static String BAD_REQUEST_MESSAGE_MIN_VALUE = "Transfer amount must be at least 0.01";
    private final static String BAD_REQUEST_MESSAGE_MAX_VALUE = "Transfer amount cannot exceed 10000";
    private final static String UNAUTHENTIC_ERROR_MESSAGE = "Unauthorized access to account";


    public static Stream<Arguments> transferAmountValidData() {
        return Stream.of(Arguments.of(100.5, 100),
                Arguments.of(100.5, 100.5),
                Arguments.of(200, 100),
                Arguments.of(100, 0.1234),
                Arguments.of(100.5, 10.6),
                Arguments.of(100.77, 10.66));
    }

    @MethodSource("transferAmountValidData")
    @ParameterizedTest
    public void transferFromUserAccount1ToUserAccount2Test(double deposit, double transferAmount) {

        // создаем юзера
        CreateUserRequestModel userRequestModel = CreateUserRequestModel.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .execute(userRequestModel);

        // создаем аккаунт 1
        CreateAccountResponseModel accountSender =
                new CreateAccountRequester(
                        RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                        ResponseSpecs.entityWasCreated())
                        .execute()
                        .extract()
                        .as(CreateAccountResponseModel.class);

        // добавляем депозит на аккаунт 1
        DepositMoneyRequestModel depositMoneyRequestModel = DepositMoneyRequestModel.builder()
                .id(accountSender.getId())
                .balance(deposit)
                .build();

        new DepositMoneyRequester(RequestSpecs.authAsUser(
                userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsOK()).execute(depositMoneyRequestModel);

        // создаем аккаунт 2
        CreateAccountResponseModel accountReceiver =
                new CreateAccountRequester(
                        RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                        ResponseSpecs.entityWasCreated())
                        .execute()
                        .extract()
                        .as(CreateAccountResponseModel.class);

        // выполняем трансфер с аккаунта 1 на аккаунт 2
        TransferMoneyRequestModel transferMoneyRequestModel = TransferMoneyRequestModel.builder()
                .senderAccountId(accountSender.getId())
                .receiverAccountId(accountReceiver.getId())
                .amount(transferAmount)
                .build();

        TransferMoneyResponseModel transferMoneyResponseModel = new TransferMoneyRequester(
                RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsOK()).execute(transferMoneyRequestModel)
                .extract()
                .as(TransferMoneyResponseModel.class);

        // проверяем тело респонса
        softly.assertThat(accountSender.getId()).isEqualTo(transferMoneyResponseModel.getSenderAccountId());
        softly.assertThat(accountReceiver.getId()).isEqualTo(transferMoneyResponseModel.getReceiverAccountId());
        softly.assertThat(transferAmount).isEqualTo(transferMoneyResponseModel.getAmount());
        softly.assertThat(SUCCESSFUL_MESSAGE).isEqualTo(transferMoneyResponseModel.getMessage());

        // достаем список аккаунтов для юзера
        List<Account> accountList = new GetCustomerAccountRequester(
                RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .execute()
                .extract()
                .as(new TypeRef<List<Account>>() {
                });

        // проверяем размер количества аккаунтов
        softly.assertThat(accountList.size()).isEqualTo(2);

        // находим аккаунт - отправитель
        Account accountSenderResponse = accountList
                .stream()
                .filter(acc -> acc.getId() == accountSender.getId())
                .findFirst()
                .get();

        // проверяем баланс аккаунта отправителя и количество транзакций
        softly.assertThat(accountSenderResponse
                .getBalance()).isEqualTo(deposit - transferAmount);
        softly.assertThat(accountSenderResponse
                .getTransactions()
                .size()).isEqualTo(2);

        // находим транзакцию с типом DEPOSIT
        Transaction depositTransaction = accountSenderResponse.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT)
                .findFirst()
                .orElseThrow();

        // находим транзакцию с типом TRANSFER_OUT
        Transaction transferOutTransaction = accountSenderResponse.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_OUT)
                .findFirst()
                .orElseThrow();

        // проверяем историю транзакций DEPOSIT/TRANSFER_OUT аккаунта отправителя
        softly.assertThat(depositTransaction
                .getAmount()).isEqualTo(deposit);
        softly.assertThat(depositTransaction
                .getRelatedAccountId()).isEqualTo(accountSender.getId());
        softly.assertThat(transferOutTransaction
                .getAmount()).isEqualTo(transferAmount);
        softly.assertThat(transferOutTransaction
                .getRelatedAccountId()).isEqualTo(accountReceiver.getId());

        // находим аккаунт - получатель
        Account accountReceiverResponse = accountList
                .stream()
                .filter(acc -> acc.getId() == accountReceiver.getId())
                .findFirst()
                .get();

        // проверяем баланс аккаунта получателя и количество транзакций
        softly.assertThat(accountReceiverResponse
                .getBalance()).isEqualTo(transferAmount);
        softly.assertThat(accountReceiverResponse
                .getTransactions()
                .size()).isEqualTo(1);

        // находим транзакцию с типом TRANSFER_IN
        Transaction transferInTransaction = accountReceiverResponse.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_IN)
                .findFirst()
                .orElseThrow();

        // проверяем историю транзакции TRANSFER_IN аккаунта получателя
        softly.assertThat(transferInTransaction
                .getAmount()).isEqualTo(transferAmount);
        softly.assertThat(transferInTransaction
                .getRelatedAccountId()).isEqualTo(accountSender.getId());
    }

    @Test
    public void transferFromUser1Account1ToUser2Account2Test() {
        double deposit = 100.5;
        double transferAmount = 100.0;

        // создаем юзера 1
        CreateUserRequestModel userRequestModel1 = CreateUserRequestModel.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .execute(userRequestModel1);

        // создаем аккаунт 1 для юзера 1
        CreateAccountResponseModel accountSender =
                new CreateAccountRequester(
                        RequestSpecs.authAsUser(userRequestModel1.getUsername(), userRequestModel1.getPassword()),
                        ResponseSpecs.entityWasCreated())
                        .execute()
                        .extract()
                        .as(CreateAccountResponseModel.class);

        // добавляем депозит на аккаунт 1 для юзера 1
        DepositMoneyRequestModel depositMoneyRequestModel = DepositMoneyRequestModel.builder()
                .id(accountSender.getId())
                .balance(deposit)
                .build();

        new DepositMoneyRequester(RequestSpecs.authAsUser(
                userRequestModel1.getUsername(), userRequestModel1.getPassword()),
                ResponseSpecs.requestReturnsOK()).execute(depositMoneyRequestModel);

        // создаем юзера 2
        CreateUserRequestModel userRequestModel2 = CreateUserRequestModel.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .execute(userRequestModel2);

        // создаем аккаунт 2 для юзера 2
        CreateAccountResponseModel accountReceiver =
                new CreateAccountRequester(
                        RequestSpecs.authAsUser(userRequestModel2.getUsername(), userRequestModel2.getPassword()),
                        ResponseSpecs.entityWasCreated())
                        .execute()
                        .extract()
                        .as(CreateAccountResponseModel.class);

        // выполняем трансфер с аккаунта 1 юзера 1 на аккаунт 2 юзера 2
        TransferMoneyRequestModel transferMoneyRequestModel = TransferMoneyRequestModel.builder()
                .senderAccountId(accountSender.getId())
                .receiverAccountId(accountReceiver.getId())
                .amount(transferAmount)
                .build();

        TransferMoneyResponseModel transferMoneyResponseModel = new TransferMoneyRequester(
                RequestSpecs.authAsUser(userRequestModel1.getUsername(), userRequestModel1.getPassword()),
                ResponseSpecs.requestReturnsOK()).execute(transferMoneyRequestModel)
                .extract()
                .as(TransferMoneyResponseModel.class);

        // проверяем тело респонса
        softly.assertThat(accountSender.getId()).isEqualTo(transferMoneyResponseModel.getSenderAccountId());
        softly.assertThat(accountReceiver.getId()).isEqualTo(transferMoneyResponseModel.getReceiverAccountId());
        softly.assertThat(transferAmount).isEqualTo(transferMoneyResponseModel.getAmount());
        softly.assertThat(SUCCESSFUL_MESSAGE).isEqualTo(transferMoneyResponseModel.getMessage());

        // получаем список аккаунтов для юзера 1
        List<Account> accountList1 = new GetCustomerAccountRequester(
                RequestSpecs.authAsUser(userRequestModel1.getUsername(), userRequestModel1.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .execute()
                .extract()
                .as(new TypeRef<List<Account>>() {
                });

        // проверяем количество аккаунтов для юзера 1
        softly.assertThat(accountList1.size()).isEqualTo(1);

        // находим аккаунт - отправитель
        Account accountSenderResponse = accountList1
                .stream()
                .filter(acc -> acc.getId() == accountSender.getId())
                .findFirst()
                .get();

        // проверяем баланс аккаунта отправителя и количество транзакций
        softly.assertThat(accountSenderResponse
                .getBalance()).isEqualTo(deposit - transferAmount);
        softly.assertThat(accountSenderResponse
                .getTransactions()
                .size()).isEqualTo(2);

        // находим транзакцию с типом DEPOSIT
        Transaction depositTransaction = accountSenderResponse.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT)
                .findFirst()
                .orElseThrow();

        // находим транзакцию с типом TRANSFER_OUT
        Transaction transferOutTransaction = accountSenderResponse.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_OUT)
                .findFirst()
                .orElseThrow();

        // проверяем историю транзакций DEPOSIT/TRANSFER_OUT аккаунта отправителя
        softly.assertThat(depositTransaction
                .getAmount()).isEqualTo(deposit);
        softly.assertThat(depositTransaction
                .getRelatedAccountId()).isEqualTo(accountSender.getId());
        softly.assertThat(transferOutTransaction
                .getAmount()).isEqualTo(transferAmount);
        softly.assertThat(transferOutTransaction
                .getRelatedAccountId()).isEqualTo(accountReceiver.getId());

        // получаем список аккаунтов для юзера 2
        List<Account> accountList2 = new GetCustomerAccountRequester(
                RequestSpecs.authAsUser(userRequestModel2.getUsername(), userRequestModel2.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .execute()
                .extract()
                .as(new TypeRef<List<Account>>() {
                });

        // проверяем количество аккаунтов для юзера 2
        softly.assertThat(accountList2.size()).isEqualTo(1);

        // находим аккаунт - получатель
        Account accountReceiverResponse = accountList2
                .stream()
                .filter(acc -> acc.getId() == accountReceiver.getId())
                .findFirst()
                .get();

        // проверяем баланс аккаунта получателя и количество транзакций

        softly.assertThat(accountReceiverResponse
                .getBalance()).isEqualTo(transferAmount);
        softly.assertThat(accountReceiverResponse
                .getTransactions()
                .size()).isEqualTo(1);

        // находим транзакцию с типом TRANSFER_IN
        Transaction transferInTransaction = accountReceiverResponse.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_IN)
                .findFirst()
                .orElseThrow();

        // проверяем историю транзакции TRANSFER_IN аккаунта получателя
        softly.assertThat(transferInTransaction
                .getAmount()).isEqualTo(transferAmount);
        softly.assertThat(transferInTransaction
                .getRelatedAccountId()).isEqualTo(accountSender.getId());
        softly.assertThat(transferInTransaction
                .getTimestamp()).isNotEqualTo(null);
    }

    public static Stream<Arguments> transferAmountInvalidData() {
        return Stream.of(Arguments.of(100, 100.5, BAD_REQUEST_MESSAGE_INVALID_TRANSFER)
                , Arguments.of(100, -0.01, BAD_REQUEST_MESSAGE_MIN_VALUE),
                Arguments.of(100, 0, BAD_REQUEST_MESSAGE_MIN_VALUE),
                Arguments.of(100, 0.001, BAD_REQUEST_MESSAGE_MIN_VALUE),
                Arguments.of(100, 10000.01, BAD_REQUEST_MESSAGE_MAX_VALUE),
                Arguments.of(100, 10000.00001, BAD_REQUEST_MESSAGE_MAX_VALUE));
    }

    @MethodSource("transferAmountInvalidData")
    @ParameterizedTest
    public void transferAmountNegativeCase(double deposit, double transfer, String message) {

        // создаем юзера
        CreateUserRequestModel userRequestModel = CreateUserRequestModel.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .execute(userRequestModel);

        // создаем аккаунт 1
        CreateAccountResponseModel accountSender =
                new CreateAccountRequester(
                        RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                        ResponseSpecs.entityWasCreated())
                        .execute()
                        .extract()
                        .as(CreateAccountResponseModel.class);

        DepositMoneyRequestModel depositMoneyRequestModel = DepositMoneyRequestModel.builder()
                .id(accountSender.getId())
                .balance(deposit)
                .build();

        // добавляем депозит на аккаунт 1
        new DepositMoneyRequester(RequestSpecs.authAsUser(
                userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsOK()).execute(depositMoneyRequestModel);

        // создаем аккаунт 2
        CreateAccountResponseModel accountReceiver =
                new CreateAccountRequester(
                        RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                        ResponseSpecs.entityWasCreated())
                        .execute()
                        .extract()
                        .as(CreateAccountResponseModel.class);

        // выполняем трансфер с аккаунта 1 на аккаунт 2
        TransferMoneyRequestModel transferMoneyRequestModel = TransferMoneyRequestModel.builder()
                .senderAccountId(accountSender.getId())
                .receiverAccountId(accountReceiver.getId())
                .amount(transfer)
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsBadRequestOnlyBody(message)).execute(transferMoneyRequestModel);

        // достаем список аккаунтов для юзера
        List<Account> accountList = new GetCustomerAccountRequester(
                RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .execute()
                .extract()
                .as(new TypeRef<List<Account>>() {
                });

        // проверяем размер количества аккаунтов
        softly.assertThat(accountList.size()).isEqualTo(2);

        // находим аккаунт - отправитель
        Account accountSenderResponse = accountList
                .stream()
                .filter(acc -> acc.getId() == accountSender.getId())
                .findFirst()
                .get();

        // проверяем баланс аккаунта отправителя и количество транзакций
        softly.assertThat(accountSenderResponse
                .getBalance()).isEqualTo(deposit);
        softly.assertThat(accountSenderResponse
                .getTransactions()
                .size()).isEqualTo(1);

        // находим аккаунт - получатель
        Account accountReceiverResponse = accountList
                .stream()
                .filter(acc -> acc.getId() == accountReceiver.getId())
                .findFirst()
                .get();

        // проверяем баланс аккаунта получателя и количество транзакций
        softly.assertThat(accountReceiverResponse
                .getBalance()).isEqualTo(0);
        softly.assertThat(accountReceiverResponse
                .getTransactions()
                .size()).isEqualTo(0);
    }

    @Test
    public void transferFromAccountExistToAccountNotExist() {
        double deposit = 100.5;
        double transferAmount = 100.0;

        // создаем юзера
        CreateUserRequestModel userRequestModel = CreateUserRequestModel.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .execute(userRequestModel);

        // создаем аккаунт 1
        CreateAccountResponseModel accountSender =
                new CreateAccountRequester(
                        RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                        ResponseSpecs.entityWasCreated())
                        .execute()
                        .extract()
                        .as(CreateAccountResponseModel.class);

        // добавляем депозит на аккаунт 1
        DepositMoneyRequestModel depositMoneyRequestModel = DepositMoneyRequestModel.builder()
                .id(accountSender.getId())
                .balance(deposit)
                .build();

        new DepositMoneyRequester(RequestSpecs.authAsUser(
                userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsOK()).execute(depositMoneyRequestModel);

        // выполняем трансфер с существующего аккаунта на несуществующий
        TransferMoneyRequestModel transferMoneyRequestModel = TransferMoneyRequestModel.builder()
                .senderAccountId(accountSender.getId())
                .receiverAccountId(accountSender.getId() + 1)
                .amount(transferAmount)
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsBadRequestOnlyBody(BAD_REQUEST_MESSAGE_INVALID_TRANSFER)).execute(transferMoneyRequestModel);

        // достаем список аккаунтов для юзера
        List<Account> accountList = new GetCustomerAccountRequester(
                RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .execute()
                .extract()
                .as(new TypeRef<List<Account>>() {
                });

        // проверяем размер количества аккаунтов
        softly.assertThat(accountList.size()).isEqualTo(1);

        // находим аккаунт - отправитель
        Account accountSenderResponse = accountList
                .stream()
                .filter(acc -> acc.getId() == accountSender.getId())
                .findFirst()
                .get();

        // проверяем баланс аккаунта отправителя и количество транзакций
        softly.assertThat(accountSenderResponse
                .getBalance()).isEqualTo(deposit);
        softly.assertThat(accountSenderResponse
                .getTransactions()
                .size()).isEqualTo(1);
    }

    @Test
    public void transferFromAccountDoesNotExistToAccountExist() {
        double transferAmount = 100.0;

        // создаем юзера
        CreateUserRequestModel userRequestModel = CreateUserRequestModel.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .execute(userRequestModel);

        // создаем аккаунт - receiver
        CreateAccountResponseModel accountReceiver =
                new CreateAccountRequester(
                        RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                        ResponseSpecs.entityWasCreated())
                        .execute()
                        .extract()
                        .as(CreateAccountResponseModel.class);

        // выполняем трансфер с несуществующего аккаунта на аккаунт ресивер
        TransferMoneyRequestModel transferMoneyRequestModel = TransferMoneyRequestModel.builder()
                .senderAccountId(accountReceiver.getId() + 1)
                .receiverAccountId(accountReceiver.getId())
                .amount(transferAmount)
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsForbiddenOnlyBody(UNAUTHENTIC_ERROR_MESSAGE)).execute(transferMoneyRequestModel);

        // достаем список аккаунтов для юзера
        List<Account> accountList = new GetCustomerAccountRequester(
                RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .execute()
                .extract()
                .as(new TypeRef<List<Account>>() {
                });

        // проверяем размер количества аккаунтов
        softly.assertThat(accountList.size()).isEqualTo(1);

        // находим аккаунт - получатель
        Account accountReceiverResponse = accountList
                .stream()
                .filter(acc -> acc.getId() == accountReceiver.getId())
                .findFirst()
                .get();

        // проверяем баланс аккаунта получателя и количество транзакций
        softly.assertThat(accountReceiverResponse
                .getBalance()).isEqualTo(0);
        softly.assertThat(accountReceiverResponse
                .getTransactions()
                .size()).isEqualTo(0);
    }

    @Test
    public void transferFromAccountToSameAccount() {
        double deposit = 100.5;
        double transferAmount = 100.0;

        // создаем юзера
        CreateUserRequestModel userRequestModel = CreateUserRequestModel.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .execute(userRequestModel);

        // создаем аккаунт 1
        CreateAccountResponseModel accountSender =
                new CreateAccountRequester(
                        RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                        ResponseSpecs.entityWasCreated())
                        .execute()
                        .extract()
                        .as(CreateAccountResponseModel.class);

        // добавляем депозит на аккаунт 1
        DepositMoneyRequestModel depositMoneyRequestModel = DepositMoneyRequestModel.builder()
                .id(accountSender.getId())
                .balance(deposit)
                .build();

        new DepositMoneyRequester(RequestSpecs.authAsUser(
                userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsOK()).execute(depositMoneyRequestModel);

        // выполняем трансфер с аккаунта 1 на аккаунт 1
        TransferMoneyRequestModel transferMoneyRequestModel = TransferMoneyRequestModel.builder()
                .senderAccountId(accountSender.getId())
                .receiverAccountId(accountSender.getId())
                .amount(transferAmount)
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsBadRequestOnlyBody(BAD_REQUEST_MESSAGE_INVALID_TRANSFER)).execute(transferMoneyRequestModel);

        // достаем список аккаунтов для юзера
        List<Account> accountList = new GetCustomerAccountRequester(
                RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .execute()
                .extract()
                .as(new TypeRef<List<Account>>() {
                });

        // проверяем размер количества аккаунтов
        softly.assertThat(accountList.size()).isEqualTo(1);

        // находим аккаунт - отправитель
        Account accountSenderResponse = accountList
                .stream()
                .filter(acc -> acc.getId() == accountSender.getId())
                .findFirst()
                .get();

        // проверяем баланс аккаунта отправителя и количество транзакций
        softly.assertThat(accountSenderResponse
                .getBalance()).isEqualTo(deposit);
        softly.assertThat(accountSenderResponse
                .getTransactions()
                .size()).isEqualTo(1);
    }
}


