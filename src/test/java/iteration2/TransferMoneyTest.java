package iteration2;

import iteration1.BaseTest;
import models.*;
import models.assertions.ModelAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.steps.AdminSteps;
import requests.skelethon.steps.UserSteps;
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
        CreateUserRequestModel userRequestModel = AdminSteps.createUser();

        // создаем аккаунт 1
        CreateAccountResponseModel accountSender = UserSteps.createAccount(userRequestModel);

        // добавляем депозит на аккаунт 1
        UserSteps.deposit(userRequestModel, accountSender.getId(), deposit);

        // создаем аккаунт 2
        CreateAccountResponseModel accountReceiver = UserSteps.createAccount(userRequestModel);

        // создаем модель запроса трансфера
        TransferMoneyRequestModel transferMoneyRequestModel = transferRequest(accountSender.getId(), accountReceiver.getId(), transferAmount);

        // выполняем трансфер с аккаунта 1 на аккаунт 2
        TransferMoneyResponseModel transferMoneyResponseModel =
                UserSteps.transfer(userRequestModel,
                        transferMoneyRequestModel);

        // сравниваем модели по model-comparison.properties
        ModelAssertions.assertThatModels(transferMoneyRequestModel, transferMoneyResponseModel).match(softly);

        // вручную проверяем сообщение
        softly.assertThat(SUCCESSFUL_MESSAGE).isEqualTo(transferMoneyResponseModel.getMessage());

        // достаем список аккаунтов для юзера
        List<Account> accountList = UserSteps.getAccountsList(userRequestModel);

        // проверяем размер количества аккаунтов
        softly.assertThat(accountList.size()).isEqualTo(2);

        // находим аккаунт - отправитель
        Account accountSenderResponse = UserSteps.getAccountById(accountList, accountSender.getId());

        // проверяем баланс аккаунта отправителя и количество транзакций
        softly.assertThat(accountSenderResponse
                .getBalance()).isEqualTo(deposit - transferAmount);
        softly.assertThat(accountSenderResponse
                .getTransactions()
                .size()).isEqualTo(2);

        // находим транзакцию с типом DEPOSIT
        Transaction depositTransaction = UserSteps.getTransactionByType(accountSenderResponse, TransactionType.DEPOSIT);

        // находим транзакцию с типом TRANSFER_OUT
        Transaction transferOutTransaction = UserSteps.getTransactionByType(accountSenderResponse, TransactionType.TRANSFER_OUT);

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
        Account accountReceiverResponse = UserSteps.getAccountById(accountList, accountReceiver.getId());

        // проверяем баланс аккаунта получателя и количество транзакций
        softly.assertThat(accountReceiverResponse
                .getBalance()).isEqualTo(transferAmount);
        softly.assertThat(accountReceiverResponse
                .getTransactions()
                .size()).isEqualTo(1);

        // находим транзакцию с типом TRANSFER_IN
        Transaction transferInTransaction = UserSteps.getTransactionByType(accountReceiverResponse, TransactionType.TRANSFER_IN);

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
        CreateUserRequestModel userRequestModel1 = AdminSteps.createUser();

        // создаем аккаунт 1 для юзера 1
        CreateAccountResponseModel accountSender = UserSteps.createAccount(userRequestModel1);

        // добавляем депозит на аккаунт 1 для юзера 1
        UserSteps.deposit(userRequestModel1, accountSender.getId(), deposit);

        // создаем юзера 2
        CreateUserRequestModel userRequestModel2 = AdminSteps.createUser();

        // создаем аккаунт 2 для юзера 2
        CreateAccountResponseModel accountReceiver = UserSteps.createAccount(userRequestModel2);

        // создаем модель запроса трансфера
        TransferMoneyRequestModel transferMoneyRequestModel = transferRequest(accountSender.getId(), accountReceiver.getId(), transferAmount);

        // выполняем трансфер с аккаунта 1 юзера 1 на аккаунт 2 юзера 2
        TransferMoneyResponseModel transferMoneyResponseModel =
                UserSteps.transfer(userRequestModel1,
                        transferMoneyRequestModel);

        // сравниваем модели по model-comparison.properties
        ModelAssertions.assertThatModels(transferMoneyRequestModel, transferMoneyResponseModel).match(softly);

        // вручную проверяем сообщение
        softly.assertThat(SUCCESSFUL_MESSAGE).isEqualTo(transferMoneyResponseModel.getMessage());

        // получаем список аккаунтов для юзера 1
        List<Account> accountList1 = UserSteps.getAccountsList(userRequestModel1);

        // проверяем количество аккаунтов для юзера 1
        softly.assertThat(accountList1.size()).isEqualTo(1);

        // находим аккаунт - отправитель
        Account accountSenderResponse = UserSteps.getAccountById(accountList1, accountSender.getId());

        // проверяем баланс аккаунта отправителя и количество транзакций
        softly.assertThat(accountSenderResponse
                .getBalance()).isEqualTo(deposit - transferAmount);
        softly.assertThat(accountSenderResponse
                .getTransactions()
                .size()).isEqualTo(2);

        // находим транзакцию с типом DEPOSIT
        Transaction depositTransaction = UserSteps.getTransactionByType(accountSenderResponse, TransactionType.DEPOSIT);

        // находим транзакцию с типом TRANSFER_OUT
        Transaction transferOutTransaction = UserSteps.getTransactionByType(accountSenderResponse, TransactionType.TRANSFER_OUT);

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
        List<Account> accountList2 = UserSteps.getAccountsList(userRequestModel2);

        // проверяем количество аккаунтов для юзера 2
        softly.assertThat(accountList2.size()).isEqualTo(1);

        // находим аккаунт - получатель
        Account accountReceiverResponse = UserSteps.getAccountById(accountList2, accountReceiver.getId());

        // проверяем баланс аккаунта получателя и количество транзакций
        softly.assertThat(accountReceiverResponse
                .getBalance()).isEqualTo(transferAmount);
        softly.assertThat(accountReceiverResponse
                .getTransactions()
                .size()).isEqualTo(1);

        // находим транзакцию с типом TRANSFER_IN
        Transaction transferInTransaction = UserSteps.getTransactionByType(accountReceiverResponse, TransactionType.TRANSFER_IN);

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
        CreateUserRequestModel userRequestModel = AdminSteps.createUser();

        // создаем аккаунт 1
        CreateAccountResponseModel accountSender = UserSteps.createAccount(userRequestModel);

        // добавляем депозит на аккаунт 1

        UserSteps.deposit(userRequestModel, accountSender.getId(), deposit);

        // создаем аккаунт 2
        CreateAccountResponseModel accountReceiver = UserSteps.createAccount(userRequestModel);

        // создаем модель запроса трансфера
        TransferMoneyRequestModel transferMoneyRequestModel = transferRequest(accountSender.getId(), accountReceiver.getId(), transfer);

        // выполняем трансфер с аккаунта 1 на аккаунт 2
        UserSteps.transferExpecting(
                userRequestModel,
                transferMoneyRequestModel,
                ResponseSpecs.requestReturnsBadRequestOnlyBody(message));

        // достаем список аккаунтов для юзера
        List<Account> accountList = UserSteps.getAccountsList(userRequestModel);

        // проверяем размер количества аккаунтов
        softly.assertThat(accountList.size()).isEqualTo(2);

        // находим аккаунт - отправитель
        Account accountSenderResponse = UserSteps.getAccountById(accountList, accountSender.getId());

        // проверяем баланс аккаунта отправителя и количество транзакций
        softly.assertThat(accountSenderResponse
                .getBalance()).isEqualTo(deposit);
        softly.assertThat(accountSenderResponse
                .getTransactions()
                .size()).isEqualTo(1);

        // находим аккаунт - получатель
        Account accountReceiverResponse = UserSteps.getAccountById(accountList, accountReceiver.getId());

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
        CreateUserRequestModel userRequestModel = AdminSteps.createUser();

        // создаем аккаунт 1
        CreateAccountResponseModel accountSender = UserSteps.createAccount(userRequestModel);

        // добавляем депозит на аккаунт 1
        UserSteps.deposit(userRequestModel, accountSender.getId(), deposit);

        // создаем модель трансфера
        TransferMoneyRequestModel transferMoneyRequestModel = transferRequest(
                accountSender.getId(),
                accountSender.getId() + 1,
                transferAmount);

        // выполняем трансфер с существующего аккаунта на несуществующий
        UserSteps.transferExpecting(
                userRequestModel,
                transferMoneyRequestModel,
                ResponseSpecs.requestReturnsBadRequestOnlyBody(BAD_REQUEST_MESSAGE_INVALID_TRANSFER));

        // достаем список аккаунтов для юзера
        List<Account> accountList = UserSteps.getAccountsList(userRequestModel);

        // проверяем размер количества аккаунтов
        softly.assertThat(accountList.size()).isEqualTo(1);

        // находим аккаунт - отправитель
        Account accountSenderResponse = UserSteps.getAccountById(accountList, accountSender.getId());

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
        CreateUserRequestModel userRequestModel = AdminSteps.createUser();

        // создаем аккаунт - receiver
        CreateAccountResponseModel accountReceiver = UserSteps.createAccount(userRequestModel);

        // создаем модель запроса трансфера
        TransferMoneyRequestModel transferMoneyRequestModel = transferRequest(
                accountReceiver.getId() + 1,
                accountReceiver.getId(),
                transferAmount);

        // выполняем трансфер с несуществующего аккаунта на аккаунт ресивер
        UserSteps.transferExpecting(
                userRequestModel,
                transferMoneyRequestModel,
                ResponseSpecs.requestReturnsForbiddenOnlyBody(UNAUTHENTIC_ERROR_MESSAGE));

        // достаем список аккаунтов для юзера
        List<Account> accountList = UserSteps.getAccountsList(userRequestModel);

        // проверяем размер количества аккаунтов
        softly.assertThat(accountList.size()).isEqualTo(1);

        // находим аккаунт - получатель
        Account accountReceiverResponse = UserSteps.getAccountById(accountList, accountReceiver.getId());

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
        CreateUserRequestModel userRequestModel = AdminSteps.createUser();

        // создаем аккаунт 1
        CreateAccountResponseModel accountSender = UserSteps.createAccount(userRequestModel);

        // добавляем депозит на аккаунт 1
        UserSteps.deposit(userRequestModel, accountSender.getId(), deposit);

        // создаем модель запроса трансфера
        TransferMoneyRequestModel transferMoneyRequestModel = transferRequest(accountSender.getId(),
                accountSender.getId(),
                transferAmount);

        // выполняем трансфер с аккаунта 1 на аккаунт 1
        UserSteps.transferExpecting(
                userRequestModel,
                transferMoneyRequestModel,
                ResponseSpecs.requestReturnsBadRequestOnlyBody(BAD_REQUEST_MESSAGE_INVALID_TRANSFER));

        // достаем список аккаунтов для юзера
        List<Account> accountList = UserSteps.getAccountsList(userRequestModel);

        // проверяем размер количества аккаунтов
        softly.assertThat(accountList.size()).isEqualTo(1);

        // находим аккаунт - отправитель
        Account accountSenderResponse = UserSteps.getAccountById(accountList, accountSender.getId());

        // проверяем баланс аккаунта отправителя и количество транзакций
        softly.assertThat(accountSenderResponse
                .getBalance()).isEqualTo(deposit);
        softly.assertThat(accountSenderResponse
                .getTransactions()
                .size()).isEqualTo(1);
    }

    // создание модели запроса трансфера
    private TransferMoneyRequestModel transferRequest(long senderId, long receiverId, double amount) {
        return TransferMoneyRequestModel.builder()
                .senderAccountId(senderId)
                .receiverAccountId(receiverId)
                .amount(amount)
                .build();
    }
}


