package api.skelethon.requesters;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import api.models.BaseModel;
import api.skelethon.Endpoint;
import api.skelethon.HttpRequester;
import api.skelethon.interfaces.GetEndpointInterface;

public class ValidatedGetRequester<T extends BaseModel> extends HttpRequester implements GetEndpointInterface {
    private final GetRequester getRequester;

    public ValidatedGetRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
        this.getRequester = new GetRequester(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        return (T) getRequester.get().extract().as(endpoint.getResponseModel());
    }
}
