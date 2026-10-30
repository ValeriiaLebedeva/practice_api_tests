package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import ui.pages.components.AccountSelect;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class TransferPage extends BasePage<TransferPage> {

    private final AccountSelect accountSelect = new AccountSelect($("select.account-selector"));
    private final SelenideElement recipientNameInput = $(Selectors.byAttribute("placeholder", "Enter recipient name"));
    private final SelenideElement recipientAccountInput = $(Selectors.byAttribute("placeholder", "Enter recipient account number"));
    private final SelenideElement amountInput = $(Selectors.byAttribute("placeholder", "Enter amount"));
    private final SelenideElement confirmCheckbox = $("#confirmCheck");
    // кнопка ищется по тексту среди всех button: перед надписью стоит эмодзи, поэтому проверка по вхождению
    private final SelenideElement sendButton = $$("button").findBy(Condition.text("Send Transfer"));

    @Override
    public String url() {
        return "/transfer";
    }

    public TransferPage waitForAccountsLoaded(int accountsCount) {
        accountSelect.waitForAccounts(accountsCount);
        return this;
    }

    // null означает «поле не заполняем»
    public TransferPage fillForm(Long senderAccountId, String recipientName, String recipientAccountNumber,
                                 String amount, boolean confirm) {
        if (senderAccountId != null) {
            accountSelect.selectAccount(senderAccountId);
        }
        if (recipientName != null) {
            recipientNameInput.setValue(recipientName);
        }
        if (recipientAccountNumber != null) {
            recipientAccountInput.setValue(recipientAccountNumber);
        }
        if (amount != null) {
            amountInput.setValue(amount);
        }
        if (confirm) {
            confirmCheckbox.setSelected(true);
        }
        return this;
    }

    public TransferPage clickSend() {
        sendButton.click();
        return this;
    }

    // после успешного перевода поля и чекбокс сбрасываются
    public TransferPage checkFormIsCleared() {
        recipientNameInput.shouldBe(Condition.empty);
        recipientAccountInput.shouldBe(Condition.empty);
        amountInput.shouldBe(Condition.empty);
        confirmCheckbox.shouldNotBe(Condition.checked);
        return this;
    }

    public AccountSelect accounts() {
        return accountSelect;
    }
}
