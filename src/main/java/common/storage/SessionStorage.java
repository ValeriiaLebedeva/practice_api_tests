package common.storage;

import api.models.CreateAccountResponseModel;
import api.models.CreateUserRequestModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Данные, которые расширения подготовили для текущего теста: пользователи, аккаунты, имя.
 * ThreadLocal: при параллельном запуске у каждого потока свои данные.
 * Номера начинаются с 1 и идут в порядке создания. Аккаунты и имя относятся к пользователю 1.
 */
public final class SessionStorage {

    private static final ThreadLocal<List<CreateUserRequestModel>> USERS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<List<CreateAccountResponseModel>> ACCOUNTS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<String> NAME = new ThreadLocal<>();

    private SessionStorage() {
    }

    // ---- пользователи ----
    public static void setUsers(List<CreateUserRequestModel> users) {
        USERS.set(new ArrayList<>(users));
    }

    public static CreateUserRequestModel getUser(int number) {
        List<CreateUserRequestModel> users = USERS.get();
        if (number < 1 || number > users.size()) {
            throw new IllegalStateException("Пользователь №" + number + " не создан: в сессии пользователей "
                    + users.size() + ". Проверьте @UserSession(users = ...) на тесте.");
        }
        return users.get(number - 1);
    }

    public static CreateUserRequestModel getUser() {
        return getUser(1);
    }

    // ---- аккаунты пользователя 1 ----
    public static void addAccount(CreateAccountResponseModel account) {
        ACCOUNTS.get().add(account);
    }

    public static CreateAccountResponseModel getAccount(int number) {
        List<CreateAccountResponseModel> accounts = ACCOUNTS.get();
        if (number < 1 || number > accounts.size()) {
            throw new IllegalStateException("Аккаунт №" + number + " не создан: в сессии аккаунтов "
                    + accounts.size() + ". Проверьте @WithAccount(count = ...) на тесте.");
        }
        return accounts.get(number - 1);
    }

    public static CreateAccountResponseModel getAccount() {
        return getAccount(1);
    }

    // ---- имя пользователя 1 ----
    public static void setName(String name) {
        NAME.set(name);
    }

    public static String getName() {
        String name = NAME.get();
        if (name == null) {
            throw new IllegalStateException("Имя не задано: добавьте @WithProfileName на тест");
        }
        return name;
    }

    public static void clear() {
        USERS.remove();
        ACCOUNTS.remove();
        NAME.remove();
    }
}
