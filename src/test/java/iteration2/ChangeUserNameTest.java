package iteration2;

import generators.RandomData;
import iteration1.BaseTest;
import models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import requests.ChangeUserNameRequester;
import requests.GetCustomerProfileRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

public class ChangeUserNameTest extends BaseTest {

    private static final String SUCCESSFUL_MESSAGE = "Profile updated successfully";

    @Test
    public void userCanChangeProfileNamePositiveTest() {

        CreateUserRequestModel userRequestModel = CreateUserRequestModel.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .execute(userRequestModel);

        UpdateCustomerProfileRequestModel updateCustomerProfileRequestModel = UpdateCustomerProfileRequestModel.builder()
                .name(RandomData.getProfileName())
                .build();

        UpdateCustomerProfileResponseModel updateCustomerProfileResponseModel = new ChangeUserNameRequester(RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .execute(updateCustomerProfileRequestModel)
                .extract()
                .as(UpdateCustomerProfileResponseModel.class);

        softly.assertThat(updateCustomerProfileRequestModel.getName()).isEqualTo(updateCustomerProfileResponseModel.getCustomer().getName());
        softly.assertThat(SUCCESSFUL_MESSAGE).isEqualTo(updateCustomerProfileResponseModel.getMessage());

        Customer customer = new GetCustomerProfileRequester(RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .execute()
                .extract().as(Customer.class);

        softly.assertThat(customer.getName()).isEqualTo(updateCustomerProfileRequestModel.getName());
    }

    public static Stream<Arguments> userInvalidData() {
        return Stream.of(
                Arguments.of("", "Name must contain two words with letters only"), Arguments.of(" ", "Name must contain two words with letters only"), Arguments.of("New", "Name must contain two words with letters only"), Arguments.of("1234 5353", "Name must contain two words with letters only"), Arguments.of("*%^#$@!()-_+=", "Name must contain two words with letters only"));

    }

    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void userCanChangeProfileNameNegativeTest(String profileName, String message) {

        CreateUserRequestModel userRequestModel = CreateUserRequestModel.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        UpdateCustomerProfileRequestModel updateCustomerProfileRequestModel = UpdateCustomerProfileRequestModel.builder()
                .name(profileName)
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .execute(userRequestModel);

        new ChangeUserNameRequester(RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsBadRequestOnlyBody(message))
                .execute(updateCustomerProfileRequestModel);

        Customer customer = new GetCustomerProfileRequester(RequestSpecs.authAsUser(userRequestModel.getUsername(), userRequestModel.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .execute()
                .extract().as(Customer.class);

        softly.assertThat(customer.getName()).isNotEqualTo(profileName);
    }
}
