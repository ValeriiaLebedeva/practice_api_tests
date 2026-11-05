package ui.elements;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

// Дропдаун выбора аккаунта. Он одинаковый на страницах депозита и перевода,
// поэтому вынесен в компонент, а не продублирован в двух страницах.
public class AccountSelect extends BaseElement {

    private static final String PLACEHOLDER_OPTION = "-- Choose an account --";

    public AccountSelect(SelenideElement element) {
        super(element);
    }

    // аккаунты подгружаются асинхронно: ждём плейсхолдер (+1) и все аккаунты пользователя
    public AccountSelect waitForAccounts(int accountsCount) {
        findAll("option").shouldHave(CollectionCondition.size(accountsCount + 1));
        return this;
    }

    // значение пункта в дропдауне это id аккаунта
    public AccountSelect selectAccount(long accountId) {
        element.selectOptionByValue(String.valueOf(accountId));
        return this;
    }

    public AccountSelect checkOption(long accountId, String accountNumber, double balance) {
        find("option[value='" + accountId + "']")
                .shouldHave(Condition.exactText(optionText(accountNumber, balance)));
        return this;
    }

    // Все пункты вместе с плейсхолдером, порядок не важен: приложение не гарантирует порядок аккаунтов
    // (в одном запуске аккаунты шли в порядке ACC62, ACC61), и тест не должен от него зависеть.
    public AccountSelect checkAllOptions(String... accountOptionTexts) {
        String[] expected = Stream.concat(Stream.of(PLACEHOLDER_OPTION), Arrays.stream(accountOptionTexts))
                .toArray(String[]::new);
        findAll("option").first().shouldHave(Condition.exactText(PLACEHOLDER_OPTION));
        findAll("option").shouldHave(CollectionCondition.exactTextsCaseSensitiveInAnyOrder(expected));
        return this;
    }

    // формат пункта: ACC1 (Balance: $150.50)
    public static String optionText(String accountNumber, double balance) {
        return accountNumber + " (Balance: $" + String.format(Locale.US, "%.2f", balance) + ")";
    }
}
