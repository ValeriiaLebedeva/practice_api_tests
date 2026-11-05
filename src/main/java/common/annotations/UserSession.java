package common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Метка: перед тестом создать пользователей через API и (по умолчанию) авторизовать первого в браузере.
 * Работу делает UserSessionExtension, подключённое на BaseUITest.
 * Пользователи в тесте: SessionStorage.getUser() (первый), SessionStorage.getUser(2) и т.д.
 *
 * @UserSession(users = 2) - создать двух пользователей, в браузере авторизован первый.
 * @UserSession(auth = false) - пользователей создать, но токен в браузер не ставить.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserSession {
    int users() default 1;

    boolean auth() default true;
}
