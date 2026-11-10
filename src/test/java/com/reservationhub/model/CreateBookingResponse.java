package com.reservationhub.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The body of POST /booking: {"bookingid": 123, "booking": {...}}.
 * Jackson fills the nested Booking object too when the JSON is deserialized into this class.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateBookingResponse(int bookingid, Booking booking) {
}
