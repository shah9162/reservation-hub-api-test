package com.reservationhub.api;

import io.restassured.response.Response;

import java.util.Map;

/**
 * One method per endpoint. Methods only send the request and return the response;
 * the tests decide what to assert. Write methods take a token (null = no auth).
 */
public final class BookingApi {

    private static final String BOOKINGS = "/booking";
    private static final String BOOKING_BY_ID = "/booking/{id}";

    private BookingApi() {
    }

    public static Response ping() {
        return ApiClient.request().get("/ping");
    }

    public static Response create(Object booking) {
        return ApiClient.request().body(booking).post(BOOKINGS);
    }

    public static Response get(Object id) {
        return ApiClient.request().pathParam("id", id).get(BOOKING_BY_ID);
    }

    public static Response listAll() {
        return ApiClient.request().get(BOOKINGS);
    }

    public static Response search(Map<String, ?> filters) {
        return ApiClient.request().queryParams(filters).get(BOOKINGS);
    }

    public static Response update(Object id, Object booking, String token) {
        return ApiClient.request(token).pathParam("id", id).body(booking).put(BOOKING_BY_ID);
    }

    public static Response partialUpdate(Object id, Object changes, String token) {
        return ApiClient.request(token).pathParam("id", id).body(changes).patch(BOOKING_BY_ID);
    }

    public static Response delete(Object id, String token) {
        return ApiClient.request(token).pathParam("id", id).delete(BOOKING_BY_ID);
    }
}
