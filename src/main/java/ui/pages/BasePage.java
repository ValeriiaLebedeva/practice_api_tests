package ui.pages;

import com.codeborne.selenide.*;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.webdriver;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * на основании чего мы создаем страницу?
 * на основании относительного url и если мы хотим в итоге создать объект нужного класса страницы,
 * нам нужно вернуть сам объект.
 *
 * в методе open - обращаемся к url() - где взять url. и каким образом сделать так, чтобы
 * url, который мы используем - выбирался в зависимости от страницы?
 *
 * необходимо, чтобы LoginPage и его url автоматически появлялся в open(url)
 * параметризировать url так, чтобы на основании класса страницы мы могли переиспользовать его в BasePage,
 * описывая общую логику открытия страницы - создание публичного абстрактного метода.
 * он заставит каждого из наследников реализовать этот урл.
 *
 * необходимо возвращать экземпляр страницы, чтобы цепочно использовать
 * но мы не знаем, экземпляр какой страницы будем создавать - обратимся к дженерикам
 *
 *
 * делаем метод, который помогает перейти с одной страницы на другую: метод goTo
 * или getPage()
 *
 *
 * @param <T>
 */

public abstract class BasePage<T extends BasePage<T>> {
    protected SelenideElement usernameInput = $(Selectors.byAttribute("placeholder", "Username"));
    protected SelenideElement passwordInput = $(Selectors.byAttribute("placeholder", "Password"));

    // хедер общий для всех страниц после логина
    protected SelenideElement headerUserName = $(".user-name");


    public abstract String url();

    // под собой имплементирует фабрику
    public T open() {
        return Selenide.open(url(), (Class<T>) this.getClass());
    }

    public <P extends BasePage<?>> P getPage(Class<P> pageClass) {
        // фабричный метод по созданию страницы на основании класса pageClass
        // вызываем конструктор для создания страницы, на которую хотим перейти
        return Selenide.page(pageClass);
    }

    // проверка, что браузер находится на этой странице (например, после редиректа)
    public T shouldBeOpened() {
        webdriver().shouldHave(WebDriverConditions.urlContaining(url()));
        return (T) this;
    }

    public T checkAlertMessageAndAccept(String bankAlert) {
        // confirm() в отличие от switchTo().alert() ждёт появления алерта, принимает его и возвращает текст
        String actualText = Selenide.confirm();
        assertThat(actualText).contains(bankAlert);
        return (T) this;
    }

    public T checkAlertMessageAndAccept(BankAlert bankAlert) {
        return checkAlertMessageAndAccept(bankAlert.getMessage());
    }

    public T checkHeaderName(String name) {
        headerUserName.shouldHave(Condition.exactText(name));
        return (T) this;
    }
}
