package requests.skelethon.requesters;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import requests.skelethon.Endpoint;
import requests.skelethon.HttpRequester;
import requests.skelethon.interfaces.PutEndpointInterface;

public class ValidatedPutRequester<T extends BaseModel> extends HttpRequester implements PutEndpointInterface {
    private final PutRequester putRequester;

    public ValidatedPutRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
        this.putRequester = new PutRequester(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T put(BaseModel model) {
        return (T) putRequester.put(model).extract().as(endpoint.getResponseModel());
    }
}
