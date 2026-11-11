package com.reservationhub.data;

import com.reservationhub.model.Booking;
import com.reservationhub.model.BookingDates;
import org.testng.annotations.DataProvider;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * All test data in one place: valid bookings, and the invalid inputs for the data-driven tests.
 * Each data provider row is {description, input}, so the report names the exact case that failed.
 */
public final class TestData {

    /** Far-future dates, so they never clash with the API's seeded data. */
    public static final String CHECKIN = "2031-03-10";
    public static final String CHECKOUT = "2031-03-15";

    private TestData() {
    }

    // ------------------------------------------------------------------ valid bookings

    /** A valid booking with a unique name, so tests never clash with each other or other users of the shared server. */
    public static Booking validBooking() {
        String id = uniqueId();
        return new Booking("Jim" + id, "Brown" + id, 250, true, new BookingDates(CHECKIN, CHECKOUT), "Breakfast");
    }

    /** A second valid booking that differs in every field, used for PUT. */
    public static Booking otherValidBooking() {
        String id = uniqueId();
        return new Booking("Sally" + id, "Green" + id, 480, false, new BookingDates("2031-07-01", "2031-07-04"), "Late checkout");
    }

    // ------------------------------------------------------------------ invalid bookings (built as maps: a typed object cannot hold a wrong type)

    public static Map<String, Object> bookingWith(String field, Object value) {
        Map<String, Object> booking = validBookingAsMap();
        booking.put(field, value);
        return booking;
    }

    public static Map<String, Object> bookingWithout(String field) {
        Map<String, Object> booking = validBookingAsMap();
        booking.remove(field);
        return booking;
    }

    public static Map<String, Object> bookingWithDates(String checkin, String checkout) {
        return bookingWith("bookingdates", Map.of("checkin", checkin, "checkout", checkout));
    }

    private static Map<String, Object> validBookingAsMap() {
        String id = uniqueId();
        Map<String, Object> booking = new LinkedHashMap<>();
        booking.put("firstname", "Jim" + id);
        booking.put("lastname", "Brown" + id);
        booking.put("totalprice", 250);
        booking.put("depositpaid", true);
        booking.put("bookingdates", Map.of("checkin", CHECKIN, "checkout", CHECKOUT));
        booking.put("additionalneeds", "Breakfast");
        return booking;
    }

    /** A date relative to today, as yyyy-MM-dd. Built at run time so "in the past" stays in the past. */
    public static String daysFromToday(int days) {
        return LocalDate.now().plusDays(days).toString();
    }

    public static String uniqueId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // ------------------------------------------------------------------ data providers

    /** The production incident (bad price or impossible dates), plus check-in in the past. */
    @DataProvider(name = "incidentCases")
    public static Object[][] incidentCases() {
        return new Object[][]{
                {"negative price", bookingWith("totalprice", -100)},
                {"zero price", bookingWith("totalprice", 0)},
                {"check-out before check-in", bookingWithDates(CHECKIN, "2031-03-09")},
                {"check-in in the past", bookingWithDates(daysFromToday(-30), daysFromToday(-26))},
        };
    }

    @DataProvider(name = "missingFields")
    public static Object[][] missingFields() {
        return new Object[][]{
                {"no firstname", bookingWithout("firstname")},
                {"no lastname", bookingWithout("lastname")},
                {"no totalprice", bookingWithout("totalprice")},
                {"no depositpaid", bookingWithout("depositpaid")},
                {"no bookingdates", bookingWithout("bookingdates")},
                {"empty payload", new LinkedHashMap<String, Object>()},
        };
    }

    @DataProvider(name = "wrongDataTypes")
    public static Object[][] wrongDataTypes() {
        return new Object[][]{
                {"totalprice is text", bookingWith("totalprice", "abc")},
                {"depositpaid is text", bookingWith("depositpaid", "yes")},
                {"firstname is a number", bookingWith("firstname", 12345)},
        };
    }

    @DataProvider(name = "malformedDates")
    public static Object[][] malformedDates() {
        return new Object[][]{
                {"not a date", bookingWithDates("not-a-date", CHECKOUT)},
                {"30 February", bookingWithDates("2031-02-30", CHECKOUT)},
                {"month 13", bookingWithDates("2031-13-01", CHECKOUT)},
        };
    }

    @DataProvider(name = "invalidPutBodies")
    public static Object[][] invalidPutBodies() {
        return new Object[][]{
                {"negative price", bookingWith("totalprice", -50)},
                {"check-out before check-in", bookingWithDates(CHECKIN, "2031-03-01")},
        };
    }

    @DataProvider(name = "invalidPatchBodies")
    public static Object[][] invalidPatchBodies() {
        return new Object[][]{
                {"negative price", Map.of("totalprice", -50)},
                {"check-out before check-in", Map.of("bookingdates", Map.of("checkin", CHECKIN, "checkout", "2031-03-01"))},
        };
    }

    @DataProvider(name = "invalidCredentials")
    public static Object[][] invalidCredentials() {
        return new Object[][]{
                {"wrong password", Map.of("username", "admin", "password", "wrong-password")},
                {"unknown user", Map.of("username", "nobody", "password", "password123")},
                {"empty body", Map.of()},
        };
    }

    /** null means "send no auth at all". */
    @DataProvider(name = "missingOrInvalidToken")
    public static Object[][] missingOrInvalidToken() {
        return new Object[][]{
                {"no token", null},
                {"invalid token", "not-a-real-token"},
        };
    }

    @DataProvider(name = "unknownOrInvalidIds")
    public static Object[][] unknownOrInvalidIds() {
        return new Object[][]{
                {"id that does not exist", "999999999"},
                {"text id", "abc"},
                {"decimal id", "1.5"},
        };
    }
}
