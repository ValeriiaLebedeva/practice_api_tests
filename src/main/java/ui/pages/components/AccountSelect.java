package ui.pages.components;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

// Дропдаун выбора аккаунта. Он одинаковый на страницах депозита и перевода,
// поэтому вынесен в компонент, а не продублирован в двух страницах.
public class AccountSelect {

    private static final String PLACEHOLDER_OPTION = "-- Choose an account --";

    private final SelenideElement select;

    public AccountSelect(SelenideElement select) {
        this.select = select;
    }

    // аккаунты подгружаются асинхронно: ждём плейсхолдер (+1) и все аккаунты пользователя
    public AccountSelect waitForAccounts(int accountsCount) {
        select.$$("option").shouldHave(CollectionCondition.size(accountsCount + 1));
        return this;
    }

    // значение пункта в дропдауне это id аккаунта
    public AccountSelect selectAccount(long accountId) {
        select.selectOptionByValue(String.valueOf(accountId));
        return this;
    }

    public AccountSelect checkOption(long accountId, String accountNumber, double balance) {
        select.$("option[value='" + accountId + "']")
                .shouldHave(Condition.exactText(optionText(accountNumber, balance)));
        return this;
    }

    // все пункты по порядку: первым всегда плейсхолдер
    public AccountSelect checkAllOptions(String... accountOptionTexts) {
        String[] expected = Stream.concat(Stream.of(PLACEHOLDER_OPTION), Arrays.stream(accountOptionTexts))
                .toArray(String[]::new);
        select.$$("option").shouldHave(CollectionCondition.exactTexts(expected));
        return this;
    }

    // формат пункта: ACC1 (Balance: $150.50)
    public static String optionText(String accountNumber, double balance) {
        return accountNumber + " (Balance: $" + String.format(Locale.US, "%.2f", balance) + ")";
    }
}
