package com.reservationhub.tests;

import com.reservationhub.api.BookingApi;
import com.reservationhub.data.TestData;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

/**
 * Checks the SHAPE of responses (required fields, types, date format, no unexpected fields)
 * against the JSON schemas in src/test/resources/schemas.
 */
@Epic("Bookings API")
@Feature("Response contract")
public class ContractTest extends BaseTest {

    @Test(description = "POST /booking response matches its schema")
    @Severity(SeverityLevel.CRITICAL)
    public void createResponseMatchesSchema() {
        assertMatchesSchema(createAndTrack(TestData.validBooking()), "schemas/create-booking-response.schema.json");
    }

    @Test(description = "GET /booking/{id} response matches the booking schema")
    @Severity(SeverityLevel.CRITICAL)
    public void getBookingResponseMatchesSchema() {
        int id = createBooking(TestData.validBooking());

        assertMatchesSchema(BookingApi.get(id), "schemas/booking.schema.json");
    }

    @Test(description = "GET /booking response is a list of booking ids")
    @Severity(SeverityLevel.NORMAL)
    public void listResponseMatchesSchema() {
        assertMatchesSchema(BookingApi.listAll(), "schemas/booking-ids.schema.json");
    }

    private static void assertMatchesSchema(Response response, String schema) {
        response.then().statusCode(200).body(matchesJsonSchemaInClasspath(schema));
    }
}
