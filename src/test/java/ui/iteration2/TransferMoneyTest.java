package ui.iteration2;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import generators.RandomData;
import models.Account;
import models.CreateAccountResponseModel;
import models.CreateUserRequestModel;
import models.TransactionType;
import models.UpdateCustomerProfileRequestModel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import requests.skelethon.steps.AdminSteps;
import requests.skelethon.steps.UserSteps;
import ui.iteration1.BaseUITest;

import java.util.Locale;

import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class TransferMoneyTest extends BaseUITest{

    private static final double DEPOSIT = 500;
    private static final double AMOUNT = 100;
    private static final String AMOUNT_TEXT = "100";

    private static final String MSG_FILL_ALL = "❌ Please fill all fields and confirm.";
    private static final String MSG_NO_USER = "❌ No user found with this account number.";
    private static final String MSG_NAME_MISMATCH = "❌ The recipient name does not match the registered name.";
    private static final String MSG_INSUFFICIENT = "❌ Error: Invalid transfer: insufficient funds or invalid accounts";

    // какое поле пропускаем в параметризованной проверке формы (T-4)
    enum MissingField {SENDER_ACCOUNT, RECIPIENT_ACCOUNT, AMOUNT, CONFIRMATION}

    // ---------- T-1. Успешный перевод между своими аккаунтами ----------
    @Test
    public void userCanTransferMoneyBetweenOwnAccountsTest() {
        // предшаги через API
        CreateUserRequestModel user = AdminSteps.createUser();
        String userName = setProfileName(user);
        CreateAccountResponseModel sender = UserSteps.createAccount(user);
        CreateAccountResponseModel receiver = UserSteps.createAccount(user);
        UserSteps.deposit(user, sender.getId(), DEPOSIT);

        authAsUser(user);
        openTransferPage(2);

        // целевое действие на UI
        fillForm(sender.getId(), userName, receiver.getAccountNumber(), AMOUNT_TEXT, true);
        sendButton().click();

        // проверки UI
        Selenide.confirm(successMessage(receiver));
        recipientNameInput().shouldBe(Condition.empty);
        recipientAccountInput().shouldBe(Condition.empty);
        amountInput().shouldBe(Condition.empty);
        confirmCheckbox().shouldNotBe(Condition.checked);

        // проверки API
        assertTransferDone(user, sender, user, receiver);
    }

    // Выключен: по текущему коду страница не перезапрашивает аккаунты после перевода.
    // Включить после ответа разработки (должен ли дропдаун обновляться или нужен переход на другую страницу).
    @Test
    @Disabled("Ждём ответ разработки: должны ли обновляться балансы в дропдауне после перевода")
    public void balancesInDropdownAreUpdatedAfterTransferTest() {
        CreateUserRequestModel user = AdminSteps.createUser();
        String userName = setProfileName(user);
        CreateAccountResponseModel sender = UserSteps.createAccount(user);
        CreateAccountResponseModel receiver = UserSteps.createAccount(user);
        UserSteps.deposit(user, sender.getId(), DEPOSIT);

        authAsUser(user);
        openTransferPage(2);

        fillForm(sender.getId(), userName, receiver.getAccountNumber(), AMOUNT_TEXT, true);
        sendButton().click();
        Selenide.confirm(successMessage(receiver));

        optionOf(sender).shouldHave(Condition.exactText(optionText(sender, DEPOSIT - AMOUNT)));
        optionOf(receiver).shouldHave(Condition.exactText(optionText(receiver, AMOUNT)));
    }

    // ---------- T-2. Успешный перевод другому пользователю ----------
    @Test
    public void userCanTransferMoneyToAnotherUserTest() {
        CreateUserRequestModel user1 = AdminSteps.createUser();
        CreateAccountResponseModel sender = UserSteps.createAccount(user1);
        UserSteps.deposit(user1, sender.getId(), DEPOSIT);

        CreateUserRequestModel user2 = AdminSteps.createUser();
        String user2Name = setProfileName(user2);
        CreateAccountResponseModel receiver = UserSteps.createAccount(user2);

        authAsUser(user1);
        openTransferPage(1);

        fillForm(sender.getId(), user2Name, receiver.getAccountNumber(), AMOUNT_TEXT, true);
        sendButton().click();

        Selenide.confirm(successMessage(receiver));

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
        openTransferPage(1);

        // имя получателя оставляем пустым
        fillForm(sender.getId(), null, receiver.getAccountNumber(), AMOUNT_TEXT, true);
        sendButton().click();

        Selenide.confirm(successMessage(receiver));

        assertTransferDone(user1, sender, user2, receiver);
    }

    // ---------- T-4. Валидация формы (параметризованный) ----------
    @ParameterizedTest
    @EnumSource(MissingField.class)
    public void userCanNotTransferWithIncompleteFormTest(MissingField missing) {
        CreateUserRequestModel user = AdminSteps.createUser();
        String userName = setProfileName(user);
        CreateAccountResponseModel sender = UserSteps.createAccount(user);
        CreateAccountResponseModel receiver = UserSteps.createAccount(user);
        UserSteps.deposit(user, sender.getId(), DEPOSIT);

        authAsUser(user);
        openTransferPage(2);

        fillForm(
                missing == MissingField.SENDER_ACCOUNT ? null : sender.getId(),
                userName,
                missing == MissingField.RECIPIENT_ACCOUNT ? null : receiver.getAccountNumber(),
                missing == MissingField.AMOUNT ? null : AMOUNT_TEXT,
                missing != MissingField.CONFIRMATION);
        sendButton().click();

        Selenide.confirm(MSG_FILL_ALL);

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
        openTransferPage(1);

        String nonExistentAccountNumber = "ACC" + (sender.getId() + 100_000);
        fillForm(sender.getId(), RandomData.getProfileName(), nonExistentAccountNumber, AMOUNT_TEXT, true);
        sendButton().click();

        Selenide.confirm(MSG_NO_USER);

        assertAccountUnchanged(user, sender, DEPOSIT, 1);
    }

    // ---------- T-6. Имя получателя не совпадает ----------
    @Test
    public void userCanNotTransferWhenRecipientNameDoesNotMatchTest() {
        CreateUserRequestModel user1 = AdminSteps.createUser();
        CreateAccountResponseModel sender = UserSteps.createAccount(user1);
        UserSteps.deposit(user1, sender.getId(), DEPOSIT);

        CreateUserRequestModel user2 = AdminSteps.createUser();
        setProfileName(user2);
        CreateAccountResponseModel receiver = UserSteps.createAccount(user2);

        authAsUser(user1);
        openTransferPage(1);

        // имя, которое не принадлежит пользователю 2
        fillForm(sender.getId(), RandomData.getProfileName(), receiver.getAccountNumber(), AMOUNT_TEXT, true);
        sendButton().click();

        Selenide.confirm(MSG_NAME_MISMATCH);

        assertAccountUnchanged(user1, sender, DEPOSIT, 1);
        assertAccountUnchanged(user2, receiver, 0, 0);
    }

    // ---------- T-7. Ошибка бэка отображается ----------
    // Из ошибок бэка берём одну: сумма больше баланса. Остальные покрыты API-тестами.
    @Test
    public void userSeesBackendErrorWhenAmountExceedsBalanceTest() {
        double smallDeposit = 100;

        CreateUserRequestModel user = AdminSteps.createUser();
        String userName = setProfileName(user);
        CreateAccountResponseModel sender = UserSteps.createAccount(user);
        CreateAccountResponseModel receiver = UserSteps.createAccount(user);
        UserSteps.deposit(user, sender.getId(), smallDeposit);

        authAsUser(user);
        openTransferPage(2);

        fillForm(sender.getId(), userName, receiver.getAccountNumber(), "200", true);
        sendButton().click();

        Selenide.confirm(MSG_INSUFFICIENT);

        assertAccountUnchanged(user, sender, smallDeposit, 1);
        assertAccountUnchanged(user, receiver, 0, 0);
    }

    // ---------- локаторы ----------
    private SelenideElement accountSelect() {
        return $("select.account-selector");
    }

    private SelenideElement recipientNameInput() {
        return $(byAttribute("placeholder", "Enter recipient name"));
    }

    private SelenideElement recipientAccountInput() {
        return $(byAttribute("placeholder", "Enter recipient account number"));
    }

    private SelenideElement amountInput() {
        return $(byAttribute("placeholder", "Enter amount"));
    }

    private SelenideElement confirmCheckbox() {
        return $("#confirmCheck");
    }

    // кнопка ищется по тексту среди всех button: перед надписью стоит эмодзи, поэтому проверка по вхождению
    private SelenideElement sendButton() {
        return $$("button").findBy(Condition.text("Send Transfer"));
    }

    private SelenideElement optionOf(CreateAccountResponseModel account) {
        return accountSelect().$("option[value='" + account.getId() + "']");
    }

    // ---------- вспомогательные методы ----------
    // аккаунты подгружаются асинхронно: ждём плейсхолдер и все аккаунты пользователя
    private void openTransferPage(int accountsCount) {
        Selenide.open("/transfer");
        accountSelect().$$("option").shouldHave(CollectionCondition.size(accountsCount + 1));
    }

    // null означает «поле не заполняем»
    private void fillForm(Long senderAccountId, String recipientName, String recipientAccountNumber,
                          String amount, boolean confirm) {
        if (senderAccountId != null) {
            accountSelect().selectOptionByValue(String.valueOf(senderAccountId));
        }
        if (recipientName != null) {
            recipientNameInput().setValue(recipientName);
        }
        if (recipientAccountNumber != null) {
            recipientAccountInput().setValue(recipientAccountNumber);
        }
        if (amount != null) {
            amountInput().setValue(amount);
        }
        if (confirm) {
            confirmCheckbox().setSelected(true);
        }
    }

    // предшаг через API: задаём пользователю имя и возвращаем его
    private String setProfileName(CreateUserRequestModel user) {
        String name = RandomData.getProfileName();
        UserSteps.updateProfile(user, UpdateCustomerProfileRequestModel.builder().name(name).build());
        return name;
    }

    private static String successMessage(CreateAccountResponseModel receiver) {
        return "✅ Successfully transferred $" + AMOUNT_TEXT + " to account " + receiver.getAccountNumber() + "!";
    }

    // формат пункта дропдауна: ACC1 (Balance: $400.00)
    private static String optionText(CreateAccountResponseModel account, double balance) {
        return account.getAccountNumber() + " (Balance: $" + String.format(Locale.US, "%.2f", balance) + ")";
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

    private void assertAccountUnchanged(CreateUserRequestModel user, CreateAccountResponseModel account,
                                        double expectedBalance, int expectedTransactions) {
        Account acc = UserSteps.getAccountById(UserSteps.getAccountsList(user), account.getId());
        softly.assertThat(acc.getBalance()).isEqualTo(expectedBalance);
        softly.assertThat(acc.getTransactions().size()).isEqualTo(expectedTransactions);
    }
}
