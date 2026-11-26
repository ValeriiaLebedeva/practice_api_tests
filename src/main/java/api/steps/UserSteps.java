package api.steps;

import api.generators.RandomData;
import api.models.*;
import api.skelethon.Endpoint;
import api.skelethon.requesters.*;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.List;

public class UserSteps {

    // спецификация запроса от имени пользователя: одна точка вместо повторения во всех методах
    private static RequestSpecification userSpec(CreateUserRequestModel user) {
        return RequestSpecs.authAsUser(user.getUsername(), user.getPassword());
    }

    // создание аккаунта
    public static CreateAccountResponseModel createAccount(CreateUserRequestModel userRequestModel) {
        return new ValidatedCrudRequester<CreateAccountResponseModel>(
                userSpec(userRequestModel),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post(null);
    }

    // получение списка аккаунтов
    public static List<Account> getAccountsList(CreateUserRequestModel userRequestModel) {
        return new ValidatedGetListRequester<Account>(
                userSpec(userRequestModel),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .getAll();
    }

    // поиск аккаунта по id
    public static Account getAccountById(List<Account> accounts, long accountId) {
        return accounts.stream()
                .filter(a -> a.getId() == accountId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Account not found: " + accountId));
    }

    // поиск транзакции по типу
    public static Transaction getTransactionByType(Account account, TransactionType type) {
        return account.getTransactions().stream()
                .filter(t -> t.getType() == type)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Account " + account.getId() + " has no " + type + " transaction"));
    }


    // успешный депозит
    // запрос собирается снаружи, чтобы тест мог сравнить его с ответом через ModelAssertions
    public static DepositMoneyResponseModel deposit(CreateUserRequestModel user, DepositMoneyRequestModel request) {
        return new ValidatedCrudRequester<DepositMoneyResponseModel>(
                userSpec(user),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsOK())
                .post(request);
    }

    // успешный депозит, когда нужен только результат
    public static DepositMoneyResponseModel deposit(CreateUserRequestModel user, long accountId, double amount) {
        return deposit(user, depositRequest(accountId, amount));
    }

    // неуспешный депозит
    public static ValidatableResponse depositExpecting(CreateUserRequestModel user, long accountId, double amount,
                                                       ResponseSpecification expected) {
        return new CrudRequester(
                userSpec(user),
                Endpoint.ACCOUNTS_DEPOSIT,
                expected)
                .post(depositRequest(accountId, amount));
    }

    // создание модели депозита
    private static DepositMoneyRequestModel depositRequest(long accountId, double amount) {
        return DepositMoneyRequestModel.builder().id(accountId).balance(amount).build();
    }

    // успешный трансфер
    // запрос собирается снаружи, чтобы тест мог сравнить его с ответом через ModelAssertions
    public static TransferMoneyResponseModel transfer(CreateUserRequestModel user, TransferMoneyRequestModel request) {
        return new ValidatedCrudRequester<TransferMoneyResponseModel>(
                userSpec(user),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsOK())
                .post(request);
    }

    // неуспешный трансфер
    public static ValidatableResponse transferExpecting(CreateUserRequestModel user, TransferMoneyRequestModel request,
                                                        ResponseSpecification expected) {
        return new CrudRequester(
                userSpec(user),
                Endpoint.ACCOUNTS_TRANSFER,
                expected)
                .post(request);
    }


    // получение профиля
    public static Customer getProfile(CreateUserRequestModel user) {
        return new ValidatedGetRequester<Customer>(
                userSpec(user),
                Endpoint.GET_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK())
                .get();
    }

    // успешное изменение профиля
    public static UpdateCustomerProfileResponseModel updateProfile(CreateUserRequestModel user,
                                                                   UpdateCustomerProfileRequestModel request) {
        return new ValidatedPutRequester<UpdateCustomerProfileResponseModel>(
                userSpec(user),
                Endpoint.UPDATE_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK())
                .put(request);
    }

    // неуспешное изменение профиля
    public static ValidatableResponse updateProfileExpecting(CreateUserRequestModel user,
                                                             UpdateCustomerProfileRequestModel request,
                                                             ResponseSpecification expected) {
        return new PutRequester(
                userSpec(user),
                Endpoint.UPDATE_CUSTOMER_PROFILE,
                expected)
                .put(request);
    }

    // задаём пользователю случайное имя и возвращаем его
    public static String setRandomProfileName(CreateUserRequestModel user) {
        String name = RandomData.getProfileName();
        UserSteps.updateProfile(user, UpdateCustomerProfileRequestModel.builder().name(name).build());
        return name;
    }
}
