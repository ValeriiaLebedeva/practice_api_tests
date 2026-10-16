package iteration1;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import requests.skelethon.steps.AdminSteps;

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
}
