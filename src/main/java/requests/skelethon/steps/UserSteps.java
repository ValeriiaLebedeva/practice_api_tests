package requests.skelethon.steps;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.ResponseSpecification;
import models.*;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;

public class UserSteps {

    // создание аккаунта
    public static CreateAccountResponseModel createAccount(CreateUserRequestModel userRequestModel) {
        return new ValidatedCrudRequester<CreateAccountResponseModel>(
                RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post(null);
    }

    // получение списка аккаунтов
    public static List<Account> getAccountsList(CreateUserRequestModel userRequestModel) {
        return new ValidatedGetListRequester<Account>(
                RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
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
    public static DepositMoneyResponseModel deposit(CreateUserRequestModel user, long accountId, double amount) {
        return new ValidatedCrudRequester<DepositMoneyResponseModel>(
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsOK())
                .post(depositRequest(accountId, amount));
    }

    // неуспешный депозит
    public static ValidatableResponse depositExpecting(CreateUserRequestModel user, long accountId, double amount,
                                                       ResponseSpecification expected) {
        return new CrudRequester(
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
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
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsOK())
                .post(request);
    }

    // неуспешный трансфер
    public static ValidatableResponse transferExpecting(CreateUserRequestModel user, TransferMoneyRequestModel request,
                                                        ResponseSpecification expected) {
        return new CrudRequester(
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                Endpoint.ACCOUNTS_TRANSFER,
                expected)
                .post(request);
    }


    // получение профиля
    public static Customer getProfile(CreateUserRequestModel user) {
        return new ValidatedGetRequester<Customer>(
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                Endpoint.GET_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK())
                .get();
    }

    // успешное изменение профиля
    public static UpdateCustomerProfileResponseModel updateProfile(CreateUserRequestModel user,
                                                                   UpdateCustomerProfileRequestModel request) {
        return new ValidatedPutRequester<UpdateCustomerProfileResponseModel>(
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                Endpoint.UPDATE_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK())
                .put(request);
    }

    // неуспешное изменение профиля
    public static ValidatableResponse updateProfileExpecting(CreateUserRequestModel user,
                                                             UpdateCustomerProfileRequestModel request,
                                                             ResponseSpecification expected) {
        return new PutRequester(
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                Endpoint.UPDATE_CUSTOMER_PROFILE,
                expected)
                .put(request);
    }
}
