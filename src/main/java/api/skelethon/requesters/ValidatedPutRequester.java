package api.skelethon.requesters;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import api.models.BaseModel;
import api.skelethon.Endpoint;
import api.skelethon.HttpRequester;
import api.skelethon.interfaces.PutEndpointInterface;

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
