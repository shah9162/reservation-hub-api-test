package com.reservationhub.tests;

import com.reservationhub.api.BookingApi;
import com.reservationhub.data.TestData;
import com.reservationhub.model.Booking;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import java.util.Map;

import static org.hamcrest.Matchers.lessThan;
import static org.testng.Assert.assertEquals;

@Epic("Bookings API")
@Feature("Booking journey")
public class BookingJourneyTest extends BaseTest {

    @Test(groups = "smoke", description = "Create, read, update (PUT), update (PATCH) and cancel a booking")
    @Severity(SeverityLevel.BLOCKER)
    public void bookingCanBeCreatedReadUpdatedAndCancelled() {
        // Create
        Booking original = TestData.validBooking();
        int id = createBooking(original);

        // Read: we get back exactly what we sent
        assertEquals(readBooking(id), original, "booking read back after create");

        // Full update (PUT): every field changes, and the change is saved
        Booking replacement = TestData.otherValidBooking();
        Booking afterPut = BookingApi.update(id, replacement, token).then().statusCode(200).extract().as(Booking.class);
        assertEquals(afterPut, replacement, "PUT response");
        assertEquals(readBooking(id), replacement, "booking saved after PUT");

        // Partial update (PATCH): only the fields we send change
        Booking expected = new Booking("Patched", replacement.lastname(), 999,
                replacement.depositpaid(), replacement.bookingdates(), replacement.additionalneeds());
        Booking afterPatch = BookingApi.partialUpdate(id, Map.of("firstname", "Patched", "totalprice", 999), token)
                .then().statusCode(200).extract().as(Booking.class);
        assertEquals(afterPatch, expected, "PATCH response");
        assertEquals(readBooking(id), expected, "booking saved after PATCH");

        // Cancel: any success status (the exact code is checked in DeleteBookingTest), then the booking is gone
        BookingApi.delete(id, token).then().statusCode(lessThan(300));
        BookingApi.get(id).then().statusCode(404);
    }
}
