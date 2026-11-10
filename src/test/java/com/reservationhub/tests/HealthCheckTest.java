package com.reservationhub.tests;

import com.reservationhub.api.BookingApi;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

@Epic("Bookings API")
@Feature("Health check")
public class HealthCheckTest extends BaseTest {

    @Test(description = "GET /ping answers 200 OK (BUG-07)")
    @Severity(SeverityLevel.MINOR)
    public void pingReturns200() {
        BookingApi.ping().then().statusCode(200);
    }
}
