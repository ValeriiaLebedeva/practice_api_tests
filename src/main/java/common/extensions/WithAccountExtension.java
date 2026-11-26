package common.extensions;

import api.models.CreateUserRequestModel;
import api.steps.UserSteps;
import common.annotations.WithAccount;
import common.storage.SessionStorage;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

public class WithAccountExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        AnnotationSupport.findAnnotation(context.getElement(), WithAccount.class).ifPresent(config -> {
            CreateUserRequestModel user = SessionStorage.getUser();
            for (int i = 0; i < config.count(); i++) {
                SessionStorage.addAccount(UserSteps.createAccount(user));
            }
            if (config.balance() > 0) {
                UserSteps.deposit(user, SessionStorage.getAccount(1).getId(), config.balance());
            }
        });
    }
}
