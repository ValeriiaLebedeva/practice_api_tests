package api.iteration1;

import common.extensions.APIVersionExtension;
import common.extensions.TimingExtension;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import api.steps.AdminSteps;
import api.steps.DataBaseSteps;
import api.dao.TransactionDao;
import api.dao.comparison.DaoAndModelAssertions;
import api.models.Account;
import api.models.Transaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({TimingExtension.class, APIVersionExtension.class})
public class BaseTest {
    protected SoftAssertions softly;

    @BeforeEach
    public void setupTest() {
        this.softly = new SoftAssertions();
    }

    @AfterEach
    public void afterTest() {
        try {
            softly.assertAll();
        } finally {
            // выполнится, даже если soft-проверки упали
            AdminSteps.cleanUp();
        }
    }

    // В БД сумма хранится с двумя знаками после запятой, поэтому новый бекенд округляет (0.1234 -> 0.12).
    protected static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    // Транзакции аккаунта из API совпадают с таблицей transactions: то же число записей и совпадают поля
    protected void assertTransactionsInDb(Account account) {
        List<TransactionDao> dbTransactions = DataBaseSteps.getTransactionsByAccountId(account.getId());
        softly.assertThat(dbTransactions)
                .as("число транзакций аккаунта %s в БД", account.getId())
                .hasSize(account.getTransactions().size());

        for (Transaction apiTransaction : account.getTransactions()) {
            dbTransactions.stream()
                    .filter(dao -> dao.getId() == apiTransaction.getId())
                    .findFirst()
                    .ifPresentOrElse(
                            dao -> DaoAndModelAssertions.assertThat(apiTransaction, dao).match(),
                            () -> softly.fail("Транзакция " + apiTransaction.getId() + " не найдена в БД"));
        }
    }
}
