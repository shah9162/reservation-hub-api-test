package com.reservationhub.tests;

import com.reservationhub.api.BookingApi;
import com.reservationhub.data.TestData;
import com.reservationhub.model.Booking;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.assertEquals;

@Epic("Bookings API")
@Feature("Read and search bookings")
public class GetBookingTest extends BaseTest {

    @Test(dataProvider = "unknownOrInvalidIds", dataProviderClass = TestData.class,
            description = "An unknown or invalid id returns 404 (or 400), never someone else's booking (BUG-11)")
    @Severity(SeverityLevel.CRITICAL)
    public void unknownOrInvalidIdIsNotFound(String scenario, String id) {
        BookingApi.get(id).then().statusCode(anyOf(is(404), is(400)));
    }

    @Test(description = "Searching by first and last name finds exactly the booking")
    @Severity(SeverityLevel.NORMAL)
    public void searchByNameFindsTheBooking() {
        Booking booking = TestData.validBooking();
        int id = createBooking(booking);

        Response response = BookingApi.search(Map.of("firstname", booking.firstname(), "lastname", booking.lastname()));

        response.then().statusCode(200);
        assertEquals(response.jsonPath().getList("bookingid", Integer.class), List.of(id), "only our booking matches");
    }

    @Test(description = "Search by check-in date includes bookings that start on that day (BUG-10)")
    @Severity(SeverityLevel.NORMAL)
    public void searchByCheckinIncludesTheSameDay() {
        Booking booking = TestData.validBooking();
        int id = createBooking(booking);

        BookingApi.search(Map.of("lastname", booking.lastname(), "checkin", TestData.CHECKIN))
                .then().statusCode(200).body("bookingid", hasItem(id));
    }

    @Test(description = "Search by check-out date includes bookings that end on that day")
    @Severity(SeverityLevel.NORMAL)
    public void searchByCheckoutIncludesTheSameDay() {
        Booking booking = TestData.validBooking();
        int id = createBooking(booking);

        BookingApi.search(Map.of("lastname", booking.lastname(), "checkout", TestData.CHECKOUT))
                .then().statusCode(200).body("bookingid", hasItem(id));
    }
}
