package api.skelethon.requesters;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import api.skelethon.Endpoint;
import api.skelethon.HttpRequester;
import api.skelethon.interfaces.GetListEndpointInterface;


import static io.restassured.RestAssured.given;

public class GetListRequester extends HttpRequester implements GetListEndpointInterface {
    public GetListRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public ValidatableResponse getAll() {
        return given()
                .spec(requestSpecification)
                .get(endpoint.getUrl())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
