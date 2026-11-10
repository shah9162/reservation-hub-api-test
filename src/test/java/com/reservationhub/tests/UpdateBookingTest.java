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

import static org.testng.Assert.assertEquals;

@Epic("Bookings API")
@Feature("Update booking")
public class UpdateBookingTest extends BaseTest {

    @Test(groups = "smoke", description = "PATCH changes only the fields sent; everything else is kept")
    @Severity(SeverityLevel.CRITICAL)
    public void patchChangesOnlyTheFieldsSent() {
        Booking original = TestData.validBooking();
        int id = createBooking(original);
        Booking expected = new Booking(original.firstname(), original.lastname(), original.totalprice(),
                false, original.bookingdates(), "Airport transfer");

        Booking returned = BookingApi.partialUpdate(id, Map.of("depositpaid", false, "additionalneeds", "Airport transfer"), token)
                .then().statusCode(200).extract().as(Booking.class);

        assertEquals(returned, expected, "PATCH response");
        assertEquals(readBooking(id), expected, "booking saved after PATCH");
    }

    @Test(groups = "smoke", dataProvider = "invalidPutBodies", dataProviderClass = TestData.class,
            description = "PUT with a bad price or dates is rejected and the booking is not changed (BUG-01, BUG-02)")
    @Severity(SeverityLevel.BLOCKER)
    public void invalidPutIsRejected(String scenario, Map<String, Object> invalidBooking) {
        Booking original = TestData.validBooking();
        int id = createBooking(original);

        assertBadRequest(BookingApi.update(id, invalidBooking, token));
        assertEquals(readBooking(id), original, "booking must not change");
    }

    @Test(groups = "smoke", dataProvider = "invalidPatchBodies", dataProviderClass = TestData.class,
            description = "PATCH with a bad price or dates is rejected and the booking is not changed (BUG-01, BUG-02)")
    @Severity(SeverityLevel.BLOCKER)
    public void invalidPatchIsRejected(String scenario, Map<String, Object> invalidChanges) {
        Booking original = TestData.validBooking();
        int id = createBooking(original);

        assertBadRequest(BookingApi.partialUpdate(id, invalidChanges, token));
        assertEquals(readBooking(id), original, "booking must not change");
    }

    @Test(description = "Updating a booking that does not exist returns 404 (BUG-05)")
    @Severity(SeverityLevel.MINOR)
    public void updatingUnknownBookingReturns404() {
        BookingApi.update(999999999, TestData.otherValidBooking(), token).then().statusCode(404);
    }
}
