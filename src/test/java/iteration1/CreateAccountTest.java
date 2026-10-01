package iteration1;

import generators.RandomData;
import models.CreateUserRequestModel;
import models.UserRole;
import org.junit.jupiter.api.Test;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

public class CreateAccountTest extends BaseTest {

    @Test
    public void userCanCreateAccountTest() {
        CreateUserRequestModel userRequestModel = CreateUserRequestModel.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .execute(userRequestModel);

        new CreateAccountRequester(RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.entityWasCreated())
                .execute();

        // запросить все аккаунты пользователя и проверить, что наш аккаунт там

    }
}
