package com.reservationhub.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservationhub.data.TestData;
import com.reservationhub.model.Booking;
import com.reservationhub.model.CreateBookingResponse;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Epic("Bookings API")
@Feature("Create booking")
public class CreateBookingTest extends BaseTest {

    /** Jackson's converter between Java objects and JSON text. */
    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Shows serialization and deserialization explicitly.
     * (Elsewhere RestAssured does the same thing for us: .body(booking) serializes, .as(Booking.class) deserializes.)
     */
    @Test(groups = "smoke", description = "A valid booking is created and echoed back (Java object -> JSON -> Java object)")
    @Severity(SeverityLevel.BLOCKER)
    public void validBookingIsCreated() throws JsonProcessingException {
        Booking booking = TestData.validBooking();

        // Serialization: Java object -> JSON text
        String requestJson = JSON.writeValueAsString(booking);
        Allure.addAttachment("Serialized request (Booking -> JSON)", "application/json", requestJson);

        Response response = createAndTrack(requestJson);
        response.then().statusCode(200);

        // Deserialization: JSON text -> Java objects (CreateBookingResponse with a nested Booking)
        CreateBookingResponse created = JSON.readValue(response.asString(), CreateBookingResponse.class);
        Allure.addAttachment("Deserialized response (JSON -> CreateBookingResponse)", created.toString());

        assertTrue(created.bookingid() > 0, "the API returns a booking id");
        assertEquals(created.booking(), booking, "the API echoes exactly the booking we sent");
    }

    @Test(groups = "smoke", dataProvider = "incidentCases", dataProviderClass = TestData.class,
            description = "The production incident: bad price or check-out before check-in is rejected (BUG-01, BUG-02)")
    @Severity(SeverityLevel.BLOCKER)
    public void bookingWithInvalidPriceOrDatesIsRejected(String scenario, Map<String, Object> booking) {
        assertBadRequest(createAndTrack(booking));
    }

    @Test(dataProvider = "missingFields", dataProviderClass = TestData.class,
            description = "A booking with a missing field is rejected with 4xx, not a 500 crash (BUG-03)")
    @Severity(SeverityLevel.CRITICAL)
    public void bookingWithMissingFieldIsRejected(String scenario, Map<String, Object> booking) {
        assertBadRequest(createAndTrack(booking));
    }

    @Test(dataProvider = "wrongDataTypes", dataProviderClass = TestData.class,
            description = "A field with the wrong type is rejected, not silently converted (BUG-03, BUG-09)")
    @Severity(SeverityLevel.CRITICAL)
    public void bookingWithWrongDataTypeIsRejected(String scenario, Map<String, Object> booking) {
        assertBadRequest(createAndTrack(booking));
    }

    @Test(dataProvider = "malformedDates", dataProviderClass = TestData.class,
            description = "A malformed or impossible date is rejected (BUG-08)")
    @Severity(SeverityLevel.CRITICAL)
    public void bookingWithMalformedDateIsRejected(String scenario, Map<String, Object> booking) {
        assertBadRequest(createAndTrack(booking));
    }
}
