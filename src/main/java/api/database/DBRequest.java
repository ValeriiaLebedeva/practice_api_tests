package api.database;

import api.configs.Config;
import api.dao.UserDao;
import api.dao.AccountDao;
import api.dao.TransactionDao;
import api.models.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class DBRequest {
    private RequestType requestType;
    private Table table;
    private List<Condition> conditions;
    private Class<?> extractAsClass;

    public enum RequestType {
        SELECT, INSERT, UPDATE, DELETE
    }

    // таблицы БД: имя в SQL хранится в одном месте, опечатку в названии не сделать
    public enum Table {
        CUSTOMERS("customers"),
        ACCOUNTS("accounts"),
        TRANSACTIONS("transactions");

        private final String tableName;

        Table(String tableName) {
            this.tableName = tableName;
        }

        public String getTableName() {
            return tableName;
        }
    }

    public <T> T extractAs(Class<T> clazz) {
        this.extractAsClass = clazz;
        return executeQuery(clazz);
    }

    public <T> List<T> extractAsList(Class<T> clazz) {
        this.extractAsClass = clazz;
        return executeQueryList(clazz);
    }

    // Общая часть любого запроса: подключение, параметры условий и разбор результата переданной функцией
    @FunctionalInterface
    private interface ResultMapper<R> {
        R map(ResultSet resultSet) throws SQLException;
    }

    private <R> R execute(ResultMapper<R> mapper) {
        String sql = buildSQL();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // Set parameters for conditions
            if (conditions != null) {
                for (int i = 0; i < conditions.size(); i++) {
                    statement.setObject(i + 1, conditions.get(i).getValue());
                }
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                return mapper.map(resultSet);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed", e);
        }
    }

    // одна строка (или null, если ничего не найдено)
    private <T> T executeQuery(Class<T> clazz) {
        return execute(resultSet -> {
            if (clazz == UserDao.class) {
                return (T) mapToUserDao(resultSet);
            }
            if (clazz == AccountDao.class) {
                return (T) mapToAccountDao(resultSet);
            }
            // Add more mappings as needed
            throw new UnsupportedOperationException("Mapping for " + clazz.getSimpleName() + " not implemented");
        });
    }

    // все найденные строки (пустой список, если ничего не найдено)
    private <T> List<T> executeQueryList(Class<T> clazz) {
        return execute(resultSet -> {
            if (clazz == TransactionDao.class) {
                List<T> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add((T) mapToTransactionDao(resultSet));
                }
                return result;
            }
            throw new UnsupportedOperationException("List mapping for " + clazz.getSimpleName() + " not implemented");
        });
    }

    private UserDao mapToUserDao(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            return UserDao.builder()
                    .id(resultSet.getLong("id"))
                    .username(resultSet.getString("username"))
                    .password(resultSet.getString("password"))
                    .role(resultSet.getString("role"))
                    .name(resultSet.getString("name"))
                    .build();
        }
        return null;
    }

    private AccountDao mapToAccountDao(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            return AccountDao.builder()
                    .id(resultSet.getLong("id"))
                    .accountNumber(resultSet.getString("account_number"))
                    .balance(resultSet.getDouble("balance"))
                    .customerId(resultSet.getLong("customer_id"))
                    .build();
        }
        return null;
    }

    // строка текущей позиции курсора (next() вызывает цикл в executeQueryList)
    private TransactionDao mapToTransactionDao(ResultSet resultSet) throws SQLException {
        return TransactionDao.builder()
                .id(resultSet.getLong("id"))
                .amount(resultSet.getDouble("amount"))
                .type(TransactionType.valueOf(resultSet.getString("type")))
                .accountId(resultSet.getLong("account_id"))
                .relatedAccountId(resultSet.getObject("related_account_id", Long.class))
                .build();
    }

    private String buildSQL() {
        StringBuilder sql = new StringBuilder();

        switch (requestType) {
            case SELECT:
                sql.append("SELECT * FROM ").append(table.getTableName());
                if (conditions != null && !conditions.isEmpty()) {
                    sql.append(" WHERE ");
                    for (int i = 0; i < conditions.size(); i++) {
                        if (i > 0) sql.append(" AND ");
                        sql.append(conditions.get(i).getColumn()).append(" ").append(conditions.get(i).getOperator()).append(" ?");
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Request type " + requestType + " not implemented");
        }

        return sql.toString();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                Config.getProperty("db.url"),
                Config.getProperty("db.username"),
                Config.getProperty("db.password")
        );
    }

    public static DBRequestBuilder builder() {
        return new DBRequestBuilder();
    }

    public static class DBRequestBuilder {
        private RequestType requestType;
        private Table table;
        private List<Condition> conditions = new ArrayList<>();
        private Class<?> extractAsClass;

        public DBRequestBuilder requestType(RequestType requestType) {
            this.requestType = requestType;
            return this;
        }

        public DBRequestBuilder where(Condition condition) {
            this.conditions.add(condition);
            return this;
        }

        public DBRequestBuilder table(Table table) {
            this.table = table;
            return this;
        }

        public <T> T extractAs(Class<T> clazz) {
            this.extractAsClass = clazz;
            DBRequest request = DBRequest.builder()
                    .requestType(requestType)
                    .table(table)
                    .conditions(conditions)
                    .extractAsClass(extractAsClass)
                    .build();
            return request.extractAs(clazz);
        }

        public <T> List<T> extractAsList(Class<T> clazz) {
            this.extractAsClass = clazz;
            DBRequest request = DBRequest.builder()
                    .requestType(requestType)
                    .table(table)
                    .conditions(conditions)
                    .extractAsClass(extractAsClass)
                    .build();
            return request.extractAsList(clazz);
        }
    }
}
