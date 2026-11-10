package com.reservationhub.tests;

import com.reservationhub.api.BookingApi;
import com.reservationhub.data.TestData;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@Epic("Bookings API")
@Feature("Cancel booking")
public class DeleteBookingTest extends BaseTest {

    @Test(description = "A successful cancel answers 200 or 204, not 201 Created (BUG-06)")
    @Severity(SeverityLevel.MINOR)
    public void cancelReturns200or204() {
        int id = createBooking(TestData.validBooking());

        BookingApi.delete(id, token).then().statusCode(anyOf(is(200), is(204)));
    }

    @Test(description = "Cancelling a booking that does not exist returns 404 (BUG-05)")
    @Severity(SeverityLevel.MINOR)
    public void cancellingUnknownBookingReturns404() {
        BookingApi.delete(999999999, token).then().statusCode(404);
    }
}
