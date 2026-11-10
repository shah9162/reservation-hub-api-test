package com.reservationhub.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A booking as the API sends and returns it.
 * A record compares by value, so "the API returned exactly what we sent" is one assertEquals.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Booking(String firstname,
                      String lastname,
                      Integer totalprice,
                      Boolean depositpaid,
                      BookingDates bookingdates,
                      String additionalneeds) {
}
