package iteration2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class TransferMoneyTest {

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(List.of(new RequestLoggingFilter(), new ResponseLoggingFilter()));
    }


    @Test
    public void transferFromUserAccount1ToUserAccount2Test() {
        String username = "user" + (int) (Math.random() * 10000);
        double deposit = 100.5;
        double transferAmount = 100.0;

        // создание пользователя
        given().contentType(ContentType.JSON).accept(ContentType.JSON).header("Authorization", "Basic YWRtaW46YWRtaW4=").body("""
                {
                  "username": "%s",
                  "password": "Kate2000#",
                  "role": "USER"
                }
                """.formatted(username)).post("http://localhost:4111/api/v1/admin/users").then().statusCode(HttpStatus.SC_CREATED);

        // получаем токен юзера
        String userAuthHeader = given().contentType(ContentType.JSON).accept(ContentType.JSON).body("""
                {
                  "username": "%s",
                  "password": "Kate2000#"
                }
                """.formatted(username)).post("http://localhost:4111/api/v1/auth/login").then().statusCode(HttpStatus.SC_OK).extract().header("Authorization");

        // создаем аккаунт(счет) 1
        int account1Id = given().header("Authorization", userAuthHeader).post("http://localhost:4111/api/v1/accounts").then().statusCode(HttpStatus.SC_CREATED).body("balance", equalTo(0.0F)).extract().path("id");

        // создаем аккаунт(счет) 2
        int account2Id = given().header("Authorization", userAuthHeader).post("http://localhost:4111/api/v1/accounts").then().statusCode(HttpStatus.SC_CREATED).body("balance", equalTo(0.0F)).extract().path("id");

        // добавляем депозит на счет 1
        given().header("Authorization", userAuthHeader).contentType(ContentType.JSON).body("""
                {
                  "id": %d,
                  "balance": %.1f
                }
                """.formatted(account1Id, deposit)).post("http://localhost:4111/api/v1/accounts/deposit").then().statusCode(HttpStatus.SC_OK).body("balance", equalTo((float) deposit));

        // выполняем трансфер с аккаунта 1 на аккаунт 2
        given().header("Authorization", userAuthHeader).contentType(ContentType.JSON).accept(ContentType.JSON).body("""
                {
                  "senderAccountId": %d,
                  "receiverAccountId": %d,
                  "amount": %.1f
                }
                """.formatted(account1Id, account2Id, transferAmount)).post("http://localhost:4111/api/v1/accounts/transfer").then().statusCode(HttpStatus.SC_OK).body("senderAccountId", equalTo(account1Id)).body("receiverAccountId", equalTo(account2Id)).body("amount", equalTo((float) transferAmount)).body("message", equalTo("Transfer successful"));

        given().header("Authorization", userAuthHeader).accept(ContentType.JSON).when().get("http://localhost:4111/api/v1/customer/accounts").then().statusCode(HttpStatus.SC_OK).body("size()", equalTo(2))

                .body("find { it.id == %d }.balance" .formatted(account1Id), equalTo((float) (deposit - transferAmount)))

                .body("find { it.id == %d }.transactions.size()" .formatted(account1Id), equalTo(2))

                .body("find { it.id == %d }.balance" .formatted(account2Id), equalTo((float) transferAmount))

                .body("find { it.id == %d }.transactions.size()" .formatted(account2Id), equalTo(1));


        // проверяем историю транзакций счета 1
        given().header("Authorization", userAuthHeader).accept(ContentType.JSON).get("http://localhost:4111/api/v1/accounts/" + account1Id + "/transactions").then().statusCode(HttpStatus.SC_OK).body("size()", equalTo(2))

                .body("[0].id", greaterThan(0)).body("[0].amount", equalTo((float) deposit)).body("[0].type", equalTo("DEPOSIT")).body("[0].timestamp", notNullValue()).body("[0].relatedAccountId", equalTo(account1Id))

                .body("[1].id", greaterThan(0)).body("[1].amount", equalTo((float) transferAmount)).body("[1].type", equalTo("TRANSFER_OUT")).body("[1].timestamp", notNullValue()).body("[1].relatedAccountId", equalTo(account2Id));

        // проверяем историю транзакций счета 2
        given().header("Authorization", userAuthHeader).accept(ContentType.JSON).get("http://localhost:4111/api/v1/accounts/" + account2Id + "/transactions").then().statusCode(HttpStatus.SC_OK).body("size()", equalTo(1))

                .body("[0].id", greaterThan(0)).body("[0].amount", equalTo((float) transferAmount)).body("[0].type", equalTo("TRANSFER_IN")).body("[0].timestamp", notNullValue()).body("[0].relatedAccountId", equalTo(account1Id));
    }


}


