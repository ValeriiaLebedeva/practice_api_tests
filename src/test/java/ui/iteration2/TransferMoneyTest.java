package ui.iteration2;

import api.generators.RandomData;
import api.models.Account;
import api.models.CreateAccountResponseModel;
import api.models.CreateUserRequestModel;
import api.models.TransactionType;
import api.skelethon.steps.AdminSteps;
import api.skelethon.steps.UserSteps;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ui.iteration1.BaseUITest;
import ui.pages.BankAlert;
import ui.pages.TransferPage;


public class TransferMoneyTest extends BaseUITest {

    private static final double DEPOSIT = 500;
    private static final double AMOUNT = 100;
    private static final String AMOUNT_TEXT = "100";

    // какое поле пропускаем в параметризованной проверке формы (T-4)
    enum MissingField {SENDER_ACCOUNT, RECIPIENT_ACCOUNT, AMOUNT, CONFIRMATION}

    // ---------- T-1. Успешный перевод между своими аккаунтами ----------
    @Test
    public void userCanTransferMoneyBetweenOwnAccountsTest() {
        // предшаги через API
        CreateUserRequestModel user = AdminSteps.createUser();
        String userName = UserSteps.setRandomProfileName(user);
        CreateAccountResponseModel sender = UserSteps.createAccount(user);
        CreateAccountResponseModel receiver = UserSteps.createAccount(user);
        UserSteps.deposit(user, sender.getId(), DEPOSIT);

        authAsUser(user);

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
    @Test
    @Disabled("Ждём ответ разработки: должны ли обновляться балансы в дропдауне после перевода")
    public void balancesInDropdownAreUpdatedAfterTransferTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        String userName = UserSteps.setRandomProfileName(user);
        CreateAccountResponseModel sender = UserSteps.createAccount(user);
        CreateAccountResponseModel receiver = UserSteps.createAccount(user);
        UserSteps.deposit(user, sender.getId(), DEPOSIT);

        authAsUser(user);

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
    @Test
    public void userCanTransferMoneyToAnotherUserTest() {
        CreateUserRequestModel user1 = AdminSteps.createUser();
        CreateAccountResponseModel sender = UserSteps.createAccount(user1);
        UserSteps.deposit(user1, sender.getId(), DEPOSIT);

        CreateUserRequestModel user2 = AdminSteps.createUser();
        String user2Name = UserSteps.setRandomProfileName(user2);
        CreateAccountResponseModel receiver = UserSteps.createAccount(user2);

        authAsUser(user1);

        new TransferPage().open()
                .waitForAccountsLoaded(1)
                .fillForm(sender.getId(), user2Name, receiver.getAccountNumber(), AMOUNT_TEXT, true)
                .clickSend()
                .checkAlertMessageAndAccept(successAlert(receiver));

        assertTransferDone(user1, sender, user2, receiver);
    }

    // ---------- T-3. Успешный перевод пользователю без имени ----------
    // Фиксирует текущее поведение: если у получателя имя не задано, сверка имени пропускается.
    @Test
    public void userCanTransferMoneyToUserWithoutNameTest() {
        CreateUserRequestModel user1 = AdminSteps.createUser();
        CreateAccountResponseModel sender = UserSteps.createAccount(user1);
        UserSteps.deposit(user1, sender.getId(), DEPOSIT);

        CreateUserRequestModel user2 = AdminSteps.createUser(); // имя не задаём
        CreateAccountResponseModel receiver = UserSteps.createAccount(user2);

        authAsUser(user1);

        // имя получателя оставляем пустым
        new TransferPage().open()
                .waitForAccountsLoaded(1)
                .fillForm(sender.getId(), null, receiver.getAccountNumber(), AMOUNT_TEXT, true)
                .clickSend()
                .checkAlertMessageAndAccept(successAlert(receiver));

        assertTransferDone(user1, sender, user2, receiver);
    }

    // ---------- T-4. Валидация формы (параметризованный) ----------
    @ParameterizedTest
    @EnumSource(MissingField.class)
    public void userCanNotTransferWithIncompleteFormTest(MissingField missing) {
        CreateUserRequestModel user = AdminSteps.createUser();
        String userName = UserSteps.setRandomProfileName(user);
        CreateAccountResponseModel sender = UserSteps.createAccount(user);
        CreateAccountResponseModel receiver = UserSteps.createAccount(user);
        UserSteps.deposit(user, sender.getId(), DEPOSIT);

        authAsUser(user);

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
    @Test
    public void userCanNotTransferToNonExistentAccountTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        CreateAccountResponseModel sender = UserSteps.createAccount(user);
        UserSteps.deposit(user, sender.getId(), DEPOSIT);

        authAsUser(user);

        String nonExistentAccountNumber = "ACC" + (sender.getId() + 100_000);
        new TransferPage().open()
                .waitForAccountsLoaded(1)
                .fillForm(sender.getId(), RandomData.getProfileName(), nonExistentAccountNumber, AMOUNT_TEXT, true)
                .clickSend()
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_NO_USER_FOUND);

        assertAccountUnchanged(user, sender, DEPOSIT, 1);
    }

    // ---------- T-6. Имя получателя не совпадает ----------
    @Test
    public void userCanNotTransferWhenRecipientNameDoesNotMatchTest() {
        CreateUserRequestModel user1 = AdminSteps.createUser();
        CreateAccountResponseModel sender = UserSteps.createAccount(user1);
        UserSteps.deposit(user1, sender.getId(), DEPOSIT);

        CreateUserRequestModel user2 = AdminSteps.createUser();
        UserSteps.setRandomProfileName(user2);
        CreateAccountResponseModel receiver = UserSteps.createAccount(user2);

        authAsUser(user1);

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
    @Test
    public void userSeesBackendErrorWhenAmountExceedsBalanceTest() {
        double smallDeposit = 100;

        CreateUserRequestModel user = AdminSteps.createUser();
        String userName = UserSteps.setRandomProfileName(user);
        CreateAccountResponseModel sender = UserSteps.createAccount(user);
        CreateAccountResponseModel receiver = UserSteps.createAccount(user);
        UserSteps.deposit(user, sender.getId(), smallDeposit);

        authAsUser(user);

        new TransferPage().open()
                .waitForAccountsLoaded(2)
                .fillForm(sender.getId(), userName, receiver.getAccountNumber(), "200", true)
                .clickSend()
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_INSUFFICIENT_FUNDS);

        assertAccountUnchanged(user, sender, smallDeposit, 1);
        assertAccountUnchanged(user, receiver, 0, 0);
    }

    // ---------- вспомогательные методы ----------
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
