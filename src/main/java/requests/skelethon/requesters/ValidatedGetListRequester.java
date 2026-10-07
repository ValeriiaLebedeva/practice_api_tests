package requests.skelethon.requesters;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import requests.skelethon.Endpoint;
import requests.skelethon.HttpRequester;
import requests.skelethon.interfaces.GetListEndpointInterface;

import java.util.List;

public class ValidatedGetListRequester<T extends BaseModel> extends HttpRequester implements GetListEndpointInterface {
    private final GetListRequester getListRequester;

    public ValidatedGetListRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
        this.getListRequester = new GetListRequester(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> getAll() {
        return (List<T>) getListRequester.getAll()
                .extract()
                .jsonPath()
                .getList("", endpoint.getResponseModel());
    }
}
