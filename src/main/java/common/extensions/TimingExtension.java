package common.extensions;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    // тесты идут параллельно в разных потоках: нужна потокобезопасная карта
    private final Map<String, Long> startTimes = new ConcurrentHashMap<>();

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        String testName = extensionContext.getRequiredTestClass().getPackageName() + "." + extensionContext.getDisplayName();
        // ключ — uniqueId теста: он не повторяется, а имя пакета + displayName у разных классов совпадает
        startTimes.put(extensionContext.getUniqueId(), System.currentTimeMillis());
        System.out.println("Thread " + Thread.currentThread().getName() + ": Test started " + testName);
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        String testName = extensionContext.getRequiredTestClass().getPackageName() + "." + extensionContext.getDisplayName();
        Long start = startTimes.remove(extensionContext.getUniqueId());
        long testDuration = start == null ? -1 : System.currentTimeMillis() - start;
        System.out.println("Thread " + Thread.currentThread().getName() + ": Test finished " + testName + ", test duration " + testDuration + " ms");
    }
}
