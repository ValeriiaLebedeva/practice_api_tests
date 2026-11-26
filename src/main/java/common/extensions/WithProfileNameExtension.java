package common.extensions;

import api.steps.UserSteps;
import common.annotations.WithProfileName;
import common.storage.SessionStorage;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

public class WithProfileNameExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        if (AnnotationSupport.isAnnotated(context.getElement(), WithProfileName.class)) {
            SessionStorage.setName(UserSteps.setRandomProfileName(SessionStorage.getUser()));
        }
    }
}
