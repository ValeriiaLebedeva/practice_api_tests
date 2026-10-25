package api.iteration2;

import api.generators.RandomModelGenerator;
import api.iteration1.BaseTest;
import api.models.CreateUserRequestModel;
import api.models.Customer;
import api.models.UpdateCustomerProfileRequestModel;
import api.models.UpdateCustomerProfileResponseModel;
import api.models.assertions.ModelAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import api.skelethon.steps.AdminSteps;
import api.skelethon.steps.UserSteps;
import api.specs.ResponseSpecs;

import java.util.stream.Stream;

public class ChangeUserNameTest extends BaseTest {

    private static final String SUCCESSFUL_MESSAGE = "Profile updated successfully";

    @Test
    public void userCanChangeProfileNamePositiveTest() {

        CreateUserRequestModel userRequestModel = AdminSteps.createUser();

        UpdateCustomerProfileRequestModel updateCustomerProfileRequestModel =
                RandomModelGenerator.generate(UpdateCustomerProfileRequestModel.class);

        UpdateCustomerProfileResponseModel updateCustomerProfileResponseModel =
                UserSteps.updateProfile(userRequestModel, updateCustomerProfileRequestModel);

        // request.name == response.customer.name (правило в model-comparison.properties)
        ModelAssertions.assertThatModels(updateCustomerProfileRequestModel, updateCustomerProfileResponseModel).match(softly);
        softly.assertThat(SUCCESSFUL_MESSAGE).isEqualTo(updateCustomerProfileResponseModel.getMessage());

        // имя действительно сохранилось
        Customer customer = UserSteps.getProfile(userRequestModel);
        softly.assertThat(updateCustomerProfileRequestModel.getName()).isEqualTo(customer.getName());

    }

    public static Stream<Arguments> userInvalidData() {
        return Stream.of(
                Arguments.of("", "Name must contain two words with letters only"), Arguments.of(" ", "Name must contain two words with letters only"), Arguments.of("New", "Name must contain two words with letters only"), Arguments.of("1234 5353", "Name must contain two words with letters only"), Arguments.of("*%^#$@!()-_+=", "Name must contain two words with letters only"));

    }

    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void userCanChangeProfileNameNegativeTest(String profileName, String message) {

        CreateUserRequestModel userRequestModel = AdminSteps.createUser();

        UpdateCustomerProfileRequestModel updateCustomerProfileRequestModel = UpdateCustomerProfileRequestModel.builder()
                .name(profileName)
                .build();

        UserSteps.updateProfileExpecting(
                userRequestModel,
                updateCustomerProfileRequestModel,
                ResponseSpecs.requestReturnsBadRequestOnlyBody(message));

        Customer customer = UserSteps.getProfile(userRequestModel);

        softly.assertThat(profileName).isNotEqualTo(customer.getName());
    }
}
