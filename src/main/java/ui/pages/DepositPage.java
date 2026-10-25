package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import ui.pages.components.AccountSelect;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class DepositPage extends BasePage<DepositPage> {

    private final AccountSelect accountSelect = new AccountSelect($(".account-selector"));
    private final SelenideElement amountInput = $(".deposit-input");
    // кнопка ищется по тексту среди всех button: перед надписью стоит эмодзи, поэтому проверка по вхождению
    private final SelenideElement depositButton = $$("button").findBy(Condition.text("Deposit"));

    @Override
    public String url() {
        return "/deposit";
    }

    public DepositPage waitForAccountsLoaded(int accountsCount) {
        accountSelect.waitForAccounts(accountsCount);
        return this;
    }

    public DepositPage selectAccount(long accountId) {
        accountSelect.selectAccount(accountId);
        return this;
    }

    public DepositPage enterAmount(String amount) {
        amountInput.setValue(amount);
        return this;
    }

    public DepositPage clickDeposit() {
        depositButton.click();
        return this;
    }

    // выбор аккаунта, ввод суммы и нажатие «Deposit»
    public DepositPage deposit(long accountId, String amount) {
        return selectAccount(accountId).enterAmount(amount).clickDeposit();
    }

    public AccountSelect accounts() {
        return accountSelect;
    }
}
