package models.assertions;

/**
 * Правило сравнения одного поля.
 */
public enum ComparisonRule {
    EQUALS,      // значения должны совпасть (по умолчанию)
    NOT_EQUALS,  // значения должны отличаться (например, пароль в ответе зашифрован)
    IGNORE       // поле не проверяется
}
