package api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Новые версии бэкенда могут добавлять в ответы поля, которых нет в моделях (например, relatedAccount).
// Лишние поля игнорируем, чтобы тесты не падали на разборе ответа.
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseModel {
}
