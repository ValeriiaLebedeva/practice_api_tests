package ui.iteration2;

import api.generators.RandomData;
import api.models.Account;
import api.models.CreateAccountResponseModel;
import api.models.CreateUserRequestModel;
import api.models.TransactionType;
import api.steps.UserSteps;
import common.annotations.UserSession;
import common.annotations.WithAccount;
import common.annotations.WithProfileName;
import common.storage.SessionStorage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ui.iteration1.BaseUITest;
import ui.pages.BankAlert;
import ui.pages.TransferPage;


// Пользователь 1 (он же авторизован) и его аккаунты создаются аннотациями.
// Аккаунт 1 - отправитель. Второго пользователя (получателя) готовим в самом тесте.
public class TransferMoneyTest extends BaseUITest {

    private static final double DEPOSIT = 500;
    private static final double AMOUNT = 100;
    private static final String AMOUNT_TEXT = "100";

    // какое поле пропускаем в параметризованной проверке формы (T-4)
    enum MissingField {SENDER_ACCOUNT, RECIPIENT_ACCOUNT, AMOUNT, CONFIRMATION}

    // ---------- T-1. Успешный перевод между своими аккаунтами ----------
    @WithProfileName
    @WithAccount(count = 2, balance = DEPOSIT)
    @Test
    public void userCanTransferMoneyBetweenOwnAccountsTest() {
        CreateUserRequestModel user = SessionStorage.getUser();
        String userName = SessionStorage.getName();
        CreateAccountResponseModel sender = SessionStorage.getAccount(1);
        CreateAccountResponseModel receiver = SessionStorage.getAccount(2);

        // целевое действие на UI и проверки UI
        new TransferPage().open()
                .waitForAccountsLoaded(2)
                .fillForm(sender.getId(), userName, receiver.getAccountNumber(), AMOUNT_TEXT, true)
                .clickSend()
                .checkAlertMessageAndAccept(successAlert(receiver))
                .checkFormIsCleared();

        // проверки API
        assertTransferDone(user, sender, user, receiver);
    }

    // Выключен: по текущему коду страница не перезапрашивает аккаунты после перевода.
    // Включить после ответа разработки (должен ли дропдаун обновляться или нужен переход на другую страницу).
    @WithProfileName
    @WithAccount(count = 2, balance = DEPOSIT)
    @Test
    @Disabled("Ждём ответ разработки: должны ли обновляться балансы в дропдауне после перевода")
    public void balancesInDropdownAreUpdatedAfterTransferTest() {
        String userName = SessionStorage.getName();
        CreateAccountResponseModel sender = SessionStorage.getAccount(1);
        CreateAccountResponseModel receiver = SessionStorage.getAccount(2);

        new TransferPage().open()
                .waitForAccountsLoaded(2)
                .fillForm(sender.getId(), userName, receiver.getAccountNumber(), AMOUNT_TEXT, true)
                .clickSend()
                .checkAlertMessageAndAccept(successAlert(receiver))
                .accounts()
                .checkOption(sender.getId(), sender.getAccountNumber(), DEPOSIT - AMOUNT)
                .checkOption(receiver.getId(), receiver.getAccountNumber(), AMOUNT);
    }

    // ---------- T-2. Успешный перевод другому пользователю ----------
    // user1 - отправитель, авторизован; user2 - получатель, готовим его в тесте
    @UserSession(users = 2)
    @WithAccount(balance = DEPOSIT)
    @Test
    public void userCanTransferMoneyToAnotherUserTest() {
        CreateUserRequestModel user1 = SessionStorage.getUser(1);
        CreateUserRequestModel user2 = SessionStorage.getUser(2);
        CreateAccountResponseModel sender = SessionStorage.getAccount();

        String user2Name = UserSteps.setRandomProfileName(user2);
        CreateAccountResponseModel receiver = UserSteps.createAccount(user2);

        new TransferPage().open()
                .waitForAccountsLoaded(1)
                .fillForm(sender.getId(), user2Name, receiver.getAccountNumber(), AMOUNT_TEXT, true)
                .clickSend()
                .checkAlertMessageAndAccept(successAlert(receiver));

        assertTransferDone(user1, sender, user2, receiver);
    }

    // ---------- T-3. Успешный перевод пользователю без имени ----------
    // Фиксирует текущее поведение: если у получателя имя не задано, сверка имени пропускается.
    @UserSession(users = 2)
    @WithAccount(balance = DEPOSIT)
    @Test
    public void userCanTransferMoneyToUserWithoutNameTest() {
        CreateUserRequestModel user1 = SessionStorage.getUser(1);
        CreateUserRequestModel user2 = SessionStorage.getUser(2);
        CreateAccountResponseModel sender = SessionStorage.getAccount();

        // имя user2 не задаём
        CreateAccountResponseModel receiver = UserSteps.createAccount(user2);

        // имя получателя оставляем пустым
        new TransferPage().open()
                .waitForAccountsLoaded(1)
                .fillForm(sender.getId(), null, receiver.getAccountNumber(), AMOUNT_TEXT, true)
                .clickSend()
                .checkAlertMessageAndAccept(successAlert(receiver));

        assertTransferDone(user1, sender, user2, receiver);
    }

    // ---------- T-4. Валидация формы (параметризованный) ----------
    @WithProfileName
    @WithAccount(count = 2, balance = DEPOSIT)
    @ParameterizedTest
    @EnumSource(MissingField.class)
    public void userCanNotTransferWithIncompleteFormTest(MissingField missing) {
        CreateUserRequestModel user = SessionStorage.getUser();
        String userName = SessionStorage.getName();
        CreateAccountResponseModel sender = SessionStorage.getAccount(1);
        CreateAccountResponseModel receiver = SessionStorage.getAccount(2);

        new TransferPage().open()
                .waitForAccountsLoaded(2)
                .fillForm(
                        missing == MissingField.SENDER_ACCOUNT ? null : sender.getId(),
                        userName,
                        missing == MissingField.RECIPIENT_ACCOUNT ? null : receiver.getAccountNumber(),
                        missing == MissingField.AMOUNT ? null : AMOUNT_TEXT,
                        missing != MissingField.CONFIRMATION)
                .clickSend()
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_FILL_ALL_FIELDS);

        // запрос не должен был уйти: балансы и транзакции не изменились
        assertAccountUnchanged(user, sender, DEPOSIT, 1);
        assertAccountUnchanged(user, receiver, 0, 0);
    }

    // ---------- T-5. Получатель не найден ----------
    @WithAccount(balance = DEPOSIT)
    @Test
    public void userCanNotTransferToNonExistentAccountTest() {
        CreateUserRequestModel user = SessionStorage.getUser();
        CreateAccountResponseModel sender = SessionStorage.getAccount();

        String nonExistentAccountNumber = "ACC" + (sender.getId() + 100_000);
        new TransferPage().open()
                .waitForAccountsLoaded(1)
                .fillForm(sender.getId(), RandomData.getProfileName(), nonExistentAccountNumber, AMOUNT_TEXT, true)
                .clickSend()
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_NO_USER_FOUND);

        assertAccountUnchanged(user, sender, DEPOSIT, 1);
    }

    // ---------- T-6. Имя получателя не совпадает ----------
    @UserSession(users = 2)
    @WithAccount(balance = DEPOSIT)
    @Test
    public void userCanNotTransferWhenRecipientNameDoesNotMatchTest() {
        CreateUserRequestModel user1 = SessionStorage.getUser(1);
        CreateUserRequestModel user2 = SessionStorage.getUser(2);
        CreateAccountResponseModel sender = SessionStorage.getAccount();

        UserSteps.setRandomProfileName(user2);
        CreateAccountResponseModel receiver = UserSteps.createAccount(user2);

        // имя, которое не принадлежит пользователю 2
        new TransferPage().open()
                .waitForAccountsLoaded(1)
                .fillForm(sender.getId(), RandomData.getProfileName(), receiver.getAccountNumber(), AMOUNT_TEXT, true)
                .clickSend()
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_NAME_MISMATCH);

        assertAccountUnchanged(user1, sender, DEPOSIT, 1);
        assertAccountUnchanged(user2, receiver, 0, 0);
    }

    // ---------- T-7. Ошибка бэка отображается ----------
    // Из ошибок бэка берём одну: сумма больше баланса. Остальные покрыты API-тестами.
    @WithProfileName
    @WithAccount(count = 2, balance = 100)
    @Test
    public void userSeesBackendErrorWhenAmountExceedsBalanceTest() {
        CreateUserRequestModel user = SessionStorage.getUser();
        String userName = SessionStorage.getName();
        CreateAccountResponseModel sender = SessionStorage.getAccount(1);
        CreateAccountResponseModel receiver = SessionStorage.getAccount(2);

        new TransferPage().open()
                .waitForAccountsLoaded(2)
                .fillForm(sender.getId(), userName, receiver.getAccountNumber(), "200", true)
                .clickSend()
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_INSUFFICIENT_FUNDS);

        assertAccountUnchanged(user, sender, 100, 1);
        assertAccountUnchanged(user, receiver, 0, 0);
    }

    // ---------- вспомогательные методы (только для переводов) ----------
    private static String successAlert(CreateAccountResponseModel receiver) {
        return BankAlert.TRANSFER_SUCCESSFUL.format(AMOUNT_TEXT, receiver.getAccountNumber());
    }

    private void assertTransferDone(CreateUserRequestModel senderUser, CreateAccountResponseModel sender,
                                    CreateUserRequestModel receiverUser, CreateAccountResponseModel receiver) {
        Account senderAcc = UserSteps.getAccountById(UserSteps.getAccountsList(senderUser), sender.getId());
        Account receiverAcc = UserSteps.getAccountById(UserSteps.getAccountsList(receiverUser), receiver.getId());

        softly.assertThat(senderAcc.getBalance()).isEqualTo(DEPOSIT - AMOUNT);
        softly.assertThat(senderAcc.getTransactions().size()).isEqualTo(2);
        softly.assertThat(UserSteps.getTransactionByType(senderAcc, TransactionType.TRANSFER_OUT)
                .getRelatedAccountId()).isEqualTo(receiver.getId());

        softly.assertThat(receiverAcc.getBalance()).isEqualTo(AMOUNT);
        softly.assertThat(receiverAcc.getTransactions().size()).isEqualTo(1);
        softly.assertThat(UserSteps.getTransactionByType(receiverAcc, TransactionType.TRANSFER_IN)
                .getRelatedAccountId()).isEqualTo(sender.getId());
    }
}
