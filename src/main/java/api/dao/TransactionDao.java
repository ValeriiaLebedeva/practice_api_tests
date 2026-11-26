package api.dao;

import api.models.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// строка таблицы transactions (timestamp не берём: в API это строка другого формата)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionDao {
    private Long id;
    private Double amount;
    private TransactionType type;
    private Long accountId;
    private Long relatedAccountId;
}
