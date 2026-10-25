package api.skelethon;

import api.models.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Endpoint {
    ADMIN_USERS(
            "/admin/users",
            CreateUserRequestModel.class,
            CreateUserResponseModel.class
    ),

    LOGIN(
            "/auth/login",
            LoginUserRequestModel.class,
            LoginUserResponseModel.class
    ),

    ACCOUNTS(
            "/accounts",
            BaseModel.class,
            CreateAccountResponseModel.class
    ),

    CUSTOMER_ACCOUNTS(
            "/customer/accounts",
            BaseModel.class,
            Account.class
    ),

    ACCOUNTS_DEPOSIT(
            "/accounts/deposit",
            DepositMoneyRequestModel.class,
            DepositMoneyResponseModel.class
    ),

    ACCOUNTS_TRANSFER(
            "/accounts/transfer",
            TransferMoneyRequestModel.class,
            TransferMoneyResponseModel.class
    ),

    GET_CUSTOMER_PROFILE(
            "/customer/profile",
            BaseModel.class,
            Customer.class
    ),

    UPDATE_CUSTOMER_PROFILE(
            "/customer/profile",
            UpdateCustomerProfileRequestModel.class,
            UpdateCustomerProfileResponseModel.class
    ),

    ADMIN_DELETE_USER(
        "/admin/users/{id}",
        BaseModel.class,
        BaseModel.class
    );


    private final String url;
    private final Class<? extends BaseModel> requestModel;
    private final Class<? extends BaseModel> responseModel;
}