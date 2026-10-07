package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;


import static io.restassured.RestAssured.given;

public class GetCustomerAccountRequester extends Requester {
    public GetCustomerAccountRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse execute(BaseModel model) {
        return given()
                .spec(requestSpecification)
                .get("/api/v1/customer/accounts")
                .then()
                .spec(responseSpecification);
    }

    public ValidatableResponse execute() {
        return execute(null);
    }
}
