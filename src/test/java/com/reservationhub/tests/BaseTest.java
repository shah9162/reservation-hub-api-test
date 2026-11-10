package com.reservationhub.tests;

import com.reservationhub.api.ApiClient;
import com.reservationhub.api.AuthApi;
import com.reservationhub.api.BookingApi;
import com.reservationhub.model.Booking;
import io.restassured.response.Response;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

/**
 * Setup and cleanup shared by all tests, so every test is independent:
 * - wait once for the API to wake up (cold start);
 * - get a fresh token before each test (the server's ~10-minute reset invalidates old tokens);
 * - delete every booking a test created, even when the test failed.
 */
public abstract class BaseTest {

    protected String token;

    private final List<Integer> createdBookingIds = new ArrayList<>();

    @BeforeSuite(alwaysRun = true)
    public void waitForApi() {
        ApiClient.waitUntilApiIsUp();
    }

    @BeforeMethod(alwaysRun = true)
    public void getFreshToken() {
        token = AuthApi.getToken();
    }

    @AfterMethod(alwaysRun = true)
    public void deleteCreatedBookings() {
        for (int id : createdBookingIds) {
            try {
                BookingApi.delete(id, token);
            } catch (Exception serverTooSlow) {
                // Best effort: the shared server's ~10-minute reset removes the booking anyway.
            }
        }
        createdBookingIds.clear();
    }

    /** Creates a valid booking, checks it worked, and returns its id. */
    protected int createBooking(Booking booking) {
        Response response = createAndTrack(booking);
        response.then().statusCode(200);
        return response.jsonPath().getInt("bookingid");
    }

    /**
     * Sends any body to POST /booking. If the API created something (even when it should not have),
     * the id is remembered so cleanup deletes it.
     */
    protected Response createAndTrack(Object body) {
        Response response = BookingApi.create(body);
        try {
            Integer id = response.jsonPath().getObject("bookingid", Integer.class);
            if (id != null) {
                createdBookingIds.add(id);
            }
        } catch (Exception responseIsNotJson) {
            // e.g. a plain-text "Internal Server Error": nothing was created
        }
        return response;
    }

    protected Booking readBooking(int id) {
        return BookingApi.get(id).then().statusCode(200).extract().as(Booking.class);
    }

    /** Invalid input must be rejected. 400 and 422 are both correct answers. */
    protected static void assertBadRequest(Response response) {
        response.then().statusCode(anyOf(is(400), is(422)));
    }

    /** Missing or wrong credentials must be refused. 401 and 403 are both correct answers. */
    protected static void assertUnauthorised(Response response) {
        response.then().statusCode(anyOf(is(401), is(403)));
    }
}
