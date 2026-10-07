package requests.skelethon.requesters;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import requests.skelethon.Endpoint;
import requests.skelethon.HttpRequester;
import requests.skelethon.interfaces.GetEndpointInterface;

import static io.restassured.RestAssured.given;

public class GetRequester extends HttpRequester implements GetEndpointInterface {
    public GetRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public ValidatableResponse get() {
        return given()
                .spec(requestSpecification)
                .get(endpoint.getUrl())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
