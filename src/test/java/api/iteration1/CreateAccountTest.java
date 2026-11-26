package api.iteration1;

import api.dao.AccountDao;
import api.dao.comparison.DaoAndModelAssertions;
import api.models.CreateAccountResponseModel;
import api.models.CreateUserRequestModel;
import api.skelethon.Endpoint;
import api.skelethon.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import api.steps.AdminSteps;
import api.steps.DataBaseSteps;
import org.junit.jupiter.api.Test;

public class CreateAccountTest extends BaseTest {

    @Test
    public void userCanCreateAccountTest() {
        CreateUserRequestModel userRequest = AdminSteps.createUser();

        CreateAccountResponseModel createAccountResponse = new ValidatedCrudRequester<CreateAccountResponseModel>
                (RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                        Endpoint.ACCOUNTS,
                        ResponseSpecs.entityWasCreated())
                .post(null);

        AccountDao accountDao = DataBaseSteps.getAccountByAccountNumber(createAccountResponse.getAccountNumber());
        DaoAndModelAssertions.assertThat(createAccountResponse, accountDao).match();
    }
}
