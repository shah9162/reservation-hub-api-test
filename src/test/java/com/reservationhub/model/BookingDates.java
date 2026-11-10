package com.reservationhub.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Dates are kept as strings (yyyy-MM-dd), exactly as they travel over the wire. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingDates(String checkin, String checkout) {
}
