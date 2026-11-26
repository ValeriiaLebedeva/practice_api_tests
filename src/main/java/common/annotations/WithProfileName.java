package common.annotations;

import common.extensions.UserSessionExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Метка-предусловие: у пользователя 1 уже задано имя профиля.
 * Работу делает WithProfileNameExtension. Имя в тесте: SessionStorage.getName().
 * Если @UserSession на тесте нет, пользователь создаётся и авторизуется автоматически.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WithProfileName {
}