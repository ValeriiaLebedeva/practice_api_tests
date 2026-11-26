package common.extensions;

import api.models.CreateUserRequestModel;
import api.steps.AdminSteps;
import common.annotations.UserSession;
import common.annotations.WithAccount;
import common.annotations.WithProfileName;
import common.storage.SessionStorage;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import ui.pages.BasePage;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Создаёт пользователей и авторизует первого, если тесту нужен пользователь:
 * есть @UserSession или предусловие, которому нужен пользователь (@WithAccount, @WithProfileName).
 * Без @UserSession создаётся один пользователь, он авторизован.
 * Должно стоять в BaseUITest раньше WithAccountExtension и WithProfileNameExtension.
 */
public class UserSessionExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        Optional<UserSession> session = AnnotationSupport.findAnnotation(context.getElement(), UserSession.class);
        boolean needsUser = session.isPresent()
                || AnnotationSupport.isAnnotated(context.getElement(), WithAccount.class)
                || AnnotationSupport.isAnnotated(context.getElement(), WithProfileName.class);
        if (!needsUser) {
            return;
        }

        int usersCount = session.map(UserSession::users).orElse(1);
        boolean auth = session.map(UserSession::auth).orElse(true);

        List<CreateUserRequestModel> users = IntStream.range(0, usersCount)
                .mapToObj(i -> AdminSteps.createUser())
                .toList();
        SessionStorage.setUsers(users);

        if (auth) {
            BasePage.authAsUser(users.get(0).getUsername(), users.get(0).getPassword());
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        SessionStorage.clear();
    }
}
