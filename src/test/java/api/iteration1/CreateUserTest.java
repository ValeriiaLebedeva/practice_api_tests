package api.iteration1;

import api.dao.UserDao;
import api.dao.comparison.DaoAndModelAssertions;
import api.generators.RandomModelGenerator;
import api.models.CreateUserRequestModel;
import api.models.CreateUserResponseModel;
import api.models.assertions.ModelAssertions;
import api.skelethon.requesters.CrudRequester;
import api.steps.DataBaseSteps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import api.skelethon.Endpoint;
import api.skelethon.requesters.ValidatedCrudRequester;
import api.steps.AdminSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNull;

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

        UserDao userDao = DataBaseSteps.getUserByUsername(createUserRequestModel.getUsername());
        DaoAndModelAssertions.assertThat(createUserResponseModel, userDao).match();

        AdminSteps.deleteUser(createUserResponseModel.getId());
    }

    public static Stream<Arguments> userInvalidData() {
        return Stream.of(
                // username field validation
                Arguments.of("   ", "Password33$", "USER", "username", List.of("Username cannot be blank", "Username must contain only letters, digits, dashes, underscores, and dots")),
                Arguments.of("ab", "Password33$", "USER", "username", List.of("Username must be between 3 and 15 characters")),
                Arguments.of("abc$", "Password33$", "USER", "username", List.of("Username must contain only letters, digits, dashes, underscores, and dots")),
                Arguments.of("abc%", "Password33$", "USER", "username", List.of("Username must contain only letters, digits, dashes, underscores, and dots"))
        );

    }

    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void adminCanNotCreateUserWithInvalidData(String username, String password, String role, String errorKey, List<String> errorValues) {
        CreateUserRequestModel createUserRequest = CreateUserRequestModel.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();

        new CrudRequester(RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.requestReturnsBadRequest(errorKey, errorValues))
                .post(createUserRequest);

        assertNull(DataBaseSteps.getUserByUsername(createUserRequest.getUsername()));
    }
}
