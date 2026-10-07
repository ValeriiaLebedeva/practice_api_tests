package iteration1;

import generators.RandomModelGenerator;
import models.CreateUserRequestModel;
import models.CreateUserResponseModel;
import models.assertions.ModelAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.ValidatedCrudRequester;
import requests.skelethon.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

public class CreateUserTest extends BaseTest {
    @Test
    public void adminCanCreateUserWithCorrectData() {
        CreateUserRequestModel createUserRequestModel =
                RandomModelGenerator.generate(CreateUserRequestModel.class);

        CreateUserResponseModel createUserResponseModel = new ValidatedCrudRequester<CreateUserResponseModel>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.entityWasCreated())
                .post(createUserRequestModel);

        ModelAssertions.assertThatModels(createUserRequestModel, createUserResponseModel).match(softly);
        softly.assertThat(createUserRequestModel.getPassword()).isNotEqualTo(createUserResponseModel.getPassword());

        AdminSteps.deleteUser(createUserResponseModel.getId());
    }

    public static Stream<Arguments> userInvalidData() {
        return Stream.of(
                // username field validation
                Arguments.of("   ", "Password33$", "USER", "username", "Username cannot be blank"),
                Arguments.of("ab", "Password33$", "USER", "username", "Username must be between 3 and 15 characters"),
                Arguments.of("abc$", "Password33$", "USER", "username", "Username must contain only letters, digits, dashes, underscores, and dots"),
                Arguments.of("abc%", "Password33$", "USER", "username", "Username must contain only letters, digits, dashes, underscores, and dots")
        );
    }

    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void adminCanNotCreateUserWithInvalidData(String username, String password, String role, String errorKey, String errorValue) {
        CreateUserRequestModel createUserRequestModel = CreateUserRequestModel.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();

        new ValidatedCrudRequester<>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.requestReturnsBadRequest(errorKey, errorValue))
                .post(createUserRequestModel);
    }
}
