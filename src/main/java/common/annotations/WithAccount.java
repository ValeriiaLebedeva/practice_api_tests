package common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Метка-предусловие: у пользователя 1 уже есть count аккаунтов, на первый положен balance (0 - ничего не кладём).
 * Работу делает WithAccountExtension. Аккаунты в тесте: SessionStorage.getAccount(1), getAccount(2) и т.д.
 * Если @UserSession на тесте нет, пользователь создаётся и авторизуется автоматически.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WithAccount {
    int count() default 1;

    double balance() default 0;
}
