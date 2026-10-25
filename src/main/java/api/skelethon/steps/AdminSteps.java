package api.skelethon.steps;

import api.generators.RandomModelGenerator;
import api.models.CreateUserRequestModel;
import api.models.CreateUserResponseModel;
import api.skelethon.Endpoint;
import api.skelethon.requesters.CrudRequester;
import api.skelethon.requesters.ValidatedCrudRequester;
import api.skelethon.requesters.ValidatedGetListRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.ArrayList;
import java.util.List;

public class AdminSteps {

    // id пользователей, созданных текущим тестом (ThreadLocal - безопасно для параллельного запуска)
    private static final ThreadLocal<List<Long>> CREATED_USER_IDS =
            ThreadLocal.withInitial(ArrayList::new);

    public record CreatedUser(CreateUserRequestModel request, CreateUserResponseModel response) {
    }

    // старый контракт: возвращает данные для логина
    public static CreateUserRequestModel createUser() {
        return createUserWithResponse().request();
    }

    public static CreatedUser createUserWithResponse() {
        CreateUserRequestModel request =
                RandomModelGenerator.generate(CreateUserRequestModel.class);

        CreateUserResponseModel response = new ValidatedCrudRequester<CreateUserResponseModel>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.entityWasCreated())
                .post(request);

        CREATED_USER_IDS.get().add(response.getId());   // запоминаем для очистки
        return new CreatedUser(request, response);
    }

    public static void deleteUser(long id) {
        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_DELETE_USER,
                ResponseSpecs.requestReturnsOK())
                .delete(id);
    }

    public static List<CreateUserResponseModel> getAllUsers() {
        return new ValidatedGetListRequester<CreateUserResponseModel>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.requestReturnsOK())
                .getAll();
    }

    // вызывается из BaseTest после каждого теста
    public static void cleanUp() {
        try {
            for (Long id : CREATED_USER_IDS.get()) {
                new CrudRequester(
                        RequestSpecs.adminSpec(),
                        Endpoint.ADMIN_DELETE_USER,
                        ResponseSpecs.requestReturnsOK())
                        .delete(id);
            }
        } finally {
            CREATED_USER_IDS.remove();
        }
    }
}
