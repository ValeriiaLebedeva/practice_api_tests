package iteration2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class DepositMoneyTest {

    private final static String UNAUTH_ERROR_MESSAGE = "Unauthorized access to account";


    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(List.of(new RequestLoggingFilter(), new ResponseLoggingFilter()));
    }

    public static Stream<Arguments> depositAmountValidData() {
        return Stream.of(Arguments.of(0.01), Arguments.of(5000.00), Arguments.of(4999.99), Arguments.of(400.0001), Arguments.of(5000));

    }

    @MethodSource("depositAmountValidData")
    @ParameterizedTest
    public void userCanDepositMoneyAccountExistTest(double deposit) {

        String username = "user" + (int) (Math.random() * 10000);

        // создание пользователя
        given().contentType(ContentType.JSON).accept(ContentType.JSON).header("Authorization", "Basic YWRtaW46YWRtaW4=").body("""
                {
                  "username": "%s",
                  "password": "Kate2000#",
                  "role": "USER"
                }
                """.formatted(username)).post("http://localhost:4111/api/v1/admin/users").then().assertThat().statusCode(HttpStatus.SC_CREATED);

        // получаем токен юзера
        String userAuthHeader = given().contentType(ContentType.JSON).accept(ContentType.JSON).body("""
                {
                  "username": "%s",
                  "password": "Kate2000#"
                }
                """.formatted(username)).post("http://localhost:4111/api/v1/auth/login").then().assertThat().statusCode(HttpStatus.SC_OK).extract().header("Authorization");

        // создаем аккаунт(счет)
        int accountId = given().header("Authorization", userAuthHeader).contentType(ContentType.JSON).accept(ContentType.JSON).post("http://localhost:4111/api/v1/accounts").then().assertThat().statusCode(HttpStatus.SC_CREATED).body("balance", equalTo(0.0F)).extract().path("id");


        // добавляем депозит
        given().header("Authorization", userAuthHeader).contentType(ContentType.JSON).accept(ContentType.JSON).body("""
                {
                  "id": %d,
                  "balance": %s
                }
                """.formatted(accountId, deposit)).post("http://localhost:4111/api/v1/accounts/deposit").then().assertThat().statusCode(HttpStatus.SC_OK).body("balance", equalTo((float) deposit)).body("transactions.size()", equalTo(1)).body("transactions[0].amount", equalTo((float) deposit)).body("transactions[0].type", equalTo("DEPOSIT")).body("transactions[0].relatedAccountId", equalTo(accountId));
    }


    public static Stream<Arguments> depositAmountInvalidData() {
        return Stream.of(Arguments.of(-0.01, "Deposit amount must be at least 0.01"), Arguments.of(0.000001, "Deposit amount must be at least 0.01"), Arguments.of(5000.01, "Deposit amount cannot exceed 5000"));

    }

    @MethodSource("depositAmountInvalidData")
    @ParameterizedTest
    public void userCanNotDepositInvalidMoneyAccountExistTest(double deposit, String message) {

        String username = "user" + (int) (Math.random() * 10000);

        // создание пользователя
        given().contentType(ContentType.JSON).accept(ContentType.JSON).header("Authorization", "Basic YWRtaW46YWRtaW4=").body("""
                {
                  "username": "%s",
                  "password": "Kate2000#",
                  "role": "USER"
                }
                """.formatted(username)).post("http://localhost:4111/api/v1/admin/users").then().assertThat().statusCode(HttpStatus.SC_CREATED);

        // получаем токен юзера
        String userAuthHeader = given().contentType(ContentType.JSON).accept(ContentType.JSON).body("""
                {
                  "username": "%s",
                  "password": "Kate2000#"
                }
                """.formatted(username)).post("http://localhost:4111/api/v1/auth/login").then().assertThat().statusCode(HttpStatus.SC_OK).extract().header("Authorization");

        // создаем аккаунт(счет)
        int accountId = given().header("Authorization", userAuthHeader).contentType(ContentType.JSON).accept(ContentType.JSON).post("http://localhost:4111/api/v1/accounts").then().assertThat().statusCode(HttpStatus.SC_CREATED).body("balance", equalTo(0.0F)).extract().path("id");


        // добавляем депозит
        given().header("Authorization", userAuthHeader).contentType(ContentType.JSON).accept(ContentType.JSON).body("""
                {
                  "id": %d,
                  "balance": %s
                }
                """.formatted(accountId, deposit)).post("http://localhost:4111/api/v1/accounts/deposit").then().assertThat().statusCode(HttpStatus.SC_BAD_REQUEST).body(equalTo(message));
    }


    @Test
    public void userCanNotDepositMoneyAccountDoesNotExistTest() {

        String username = "user" + (int) (Math.random() * 10000);

        // создание пользователя
        given().contentType(ContentType.JSON).accept(ContentType.JSON).header("Authorization", "Basic YWRtaW46YWRtaW4=").body("""
                {
                  "username": "%s",
                  "password": "Kate2000#",
                  "role": "USER"
                }
                """.formatted(username)).post("http://localhost:4111/api/v1/admin/users").then().assertThat().statusCode(HttpStatus.SC_CREATED);

        // получаем токен юзера
        String userAuthHeader = given().contentType(ContentType.JSON).accept(ContentType.JSON).body("""
                {
                  "username": "%s",
                  "password": "Kate2000#"
                }
                """.formatted(username)).post("http://localhost:4111/api/v1/auth/login").then().assertThat().statusCode(HttpStatus.SC_OK).extract().header("Authorization");

        // создаем аккаунт(счет)
        int accountId = given().header("Authorization", userAuthHeader).contentType(ContentType.JSON).accept(ContentType.JSON).post("http://localhost:4111/api/v1/accounts").then().assertThat().statusCode(HttpStatus.SC_CREATED).body("balance", equalTo(0.0F)).extract().path("id");


        // добавляем депозит
        given().header("Authorization", userAuthHeader).contentType(ContentType.JSON).accept(ContentType.JSON).body("""
                {
                  "id": 100,
                  "balance": 100
                }
                """).post("http://localhost:4111/api/v1/accounts/deposit").then().assertThat().statusCode(HttpStatus.SC_FORBIDDEN).body(equalTo(UNAUTH_ERROR_MESSAGE));
    }


    @Test
    public void userCanNotDepositMoneyToAccountAssociatedWithAnotherUserTest() {

        String ownerUsername = "owner" + (int) (Math.random() * 10000);

        // создание owner пользователя
        given().contentType(ContentType.JSON).accept(ContentType.JSON).header("Authorization", "Basic YWRtaW46YWRtaW4=").body("""
                {
                  "username": "%s",
                  "password": "Kate2000#",
                  "role": "USER"
                }
                """.formatted(ownerUsername)).post("http://localhost:4111/api/v1/admin/users").then().assertThat().statusCode(HttpStatus.SC_CREATED);

        // получаем токен owner пользователя
        String userOwnerAuthHeader = given().contentType(ContentType.JSON).accept(ContentType.JSON).body("""
                {
                  "username": "%s",
                  "password": "Kate2000#"
                }
                """.formatted(ownerUsername)).post("http://localhost:4111/api/v1/auth/login").then().assertThat().statusCode(HttpStatus.SC_OK).extract().header("Authorization");


        // создаем аккаунт(счет) для owner
        int accountOwnerId = given().header("Authorization", userOwnerAuthHeader).contentType(ContentType.JSON).accept(ContentType.JSON).post("http://localhost:4111/api/v1/accounts").then().assertThat().statusCode(HttpStatus.SC_CREATED).body("balance", equalTo(0.0F)).extract().path("id");


        String anotherUsername = "another" + (int) (Math.random() * 10000);

        // создание another пользователя
        given().contentType(ContentType.JSON).accept(ContentType.JSON).header("Authorization", "Basic YWRtaW46YWRtaW4=").body("""
                {
                  "username": "%s",
                  "password": "Kate2000#",
                  "role": "USER"
                }
                """.formatted(anotherUsername)).post("http://localhost:4111/api/v1/admin/users").then().assertThat().statusCode(HttpStatus.SC_CREATED);

        // получаем токен юзера
        String userAnotherAuthHeader = given().contentType(ContentType.JSON).accept(ContentType.JSON).body("""
                {
                  "username": "%s",
                  "password": "Kate2000#"
                }
                """.formatted(anotherUsername)).post("http://localhost:4111/api/v1/auth/login").then().assertThat().statusCode(HttpStatus.SC_OK).extract().header("Authorization");


        // создаем аккаунт(счет) для another
        given().header("Authorization", userAnotherAuthHeader).contentType(ContentType.JSON).accept(ContentType.JSON).post("http://localhost:4111/api/v1/accounts").then().assertThat().statusCode(HttpStatus.SC_CREATED).body("balance", equalTo(0.0F)).extract().path("id");


        // another юзер пробует внести депозит на owner аккаунт
        given().header("Authorization", userAnotherAuthHeader).contentType(ContentType.JSON).accept(ContentType.JSON).body("""
                {
                  "id": %d,
                  "balance": 100
                }
                """.formatted(accountOwnerId)).post("http://localhost:4111/api/v1/accounts/deposit").then().assertThat().statusCode(HttpStatus.SC_FORBIDDEN).body(equalTo(UNAUTH_ERROR_MESSAGE));
    }

}
