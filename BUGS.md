# Bug report – Restful-booker (Bookings API)

Host: `https://restful-booker.herokuapp.com` · Found by the automated suite, run from Eclipse on **10 Nov 2026** (JDK 21, Maven 3.9.9).
The new findings (BUG-11, BUG-12) were also re-checked by hand. The full report is `reports/allure-report/index.html`, where each failing test shows the exact request and response.

**Suite result: 46 tests · 17 passed · 29 failed.** All 29 failures are the defects below, plus one product-decision row (zero price).
No failures came from the environment or the framework.

| ID | Title | Severity | Why this severity | Failing tests |
|----|-------|----------|-------------------|---------------|
| BUG-01 | **Negative total price** accepted on create, PUT and PATCH | **Critical** | Corrupts revenue and invoicing data; half of the production incident | 3 |
| BUG-02 | **Check-out before check-in** accepted on create, PUT and PATCH | **Critical** | Impossible stays break availability and billing; the other half of the incident | 3 |
| BUG-08 | **Malformed / impossible dates** accepted and stored as garbage | **Critical** | Bad dates reach the data and the partner is told it worked | 3 |
| BUG-09 | **Wrong data types silently converted** (`"abc"` price → `null`, `"yes"` deposit → `true`) | **High** | Money and deposit status change without any error | 2 |
| BUG-03 | Missing or mistyped fields cause **500 Internal Server Error** | **High** | Partners cannot tell "my request is wrong" from "your service is down" | 7 |
| BUG-04 | Failed login returns **200** | **Medium** | Clients checking only the status treat a failed login as success (no token is issued, so no data leaks) | 3 |
| BUG-10 | `checkin` search **excludes the given day** | **Medium** | "Arrivals from 10 March" misses guests arriving on 10 March | 1 |
| BUG-11 | `GET /booking/1.5` returns **booking 1** | **Medium** | A malformed reference silently returns a different customer's booking | 1 |
| BUG-13 | **Check-in in the past** accepted (e.g. 2024) | **Medium** (needs product decision) | Past stays distort availability and revenue. The documented sample booking is itself dated 2024, so the intended rule must be confirmed | 1 |
| BUG-05 | Unknown booking id on PUT/DELETE returns **405** instead of 404 | **Low** | Misleading: suggests the method is unsupported | 2 |
| BUG-06 | Successful cancel returns **201 Created** | **Low** | Wrong meaning; the booking is removed, not created | 1 |
| BUG-07 | Health check `GET /ping` returns **201 Created** | **Low** | Monitoring that expects 200 reports the service as down | 1 |
| BUG-12 | A normal multi-type `Accept` header gets **418 I'm a teapot** | **Low** | Standard HTTP clients can't create or read bookings until they send exactly `application/json` | – (found while building the suite) |

Product decision, not counted as a bug: `totalprice: 0` is accepted (1 failing row). Drop that row if free bookings are allowed.

Auth for the curls below: `admin:password123` (public sandbox credentials).

---
## BUG-01 – Negative total price accepted
```bash
curl -i -X POST https://restful-booker.herokuapp.com/booking -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"firstname":"Jim","lastname":"Brown","totalprice":-100,"depositpaid":true,"bookingdates":{"checkin":"2031-03-10","checkout":"2031-03-15"},"additionalneeds":"Breakfast"}'
```
**Expected:** `400`/`422`, nothing stored. **Actual:** `200`, the booking is stored with price -100.
Same on update: `PUT` with `"totalprice":-50` → `200`; `PATCH {"totalprice":-50}` → `200`.
Tests: `CreateBookingTest.bookingWithInvalidPriceOrDatesIsRejected[negative price]`, `UpdateBookingTest.invalidPutIsRejected`, `invalidPatchIsRejected`.

## BUG-02 – Check-out before check-in accepted
```bash
curl -i -X POST https://restful-booker.herokuapp.com/booking -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"firstname":"Jim","lastname":"Brown","totalprice":250,"depositpaid":true,"bookingdates":{"checkin":"2031-03-10","checkout":"2031-03-09"},"additionalneeds":"Breakfast"}'
```
**Expected:** `400`/`422`. **Actual:** `200`. Same on `PUT` and `PATCH` (check-out 2031-03-01 with check-in 2031-03-10 → `200`).

## BUG-08 – Malformed or impossible dates accepted
```bash
curl -i -X POST https://restful-booker.herokuapp.com/booking -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"firstname":"Jim","lastname":"Brown","totalprice":250,"depositpaid":true,"bookingdates":{"checkin":"not-a-date","checkout":"2031-03-15"}}'
```
**Expected:** `400`/`422`. **Actual:** `200`, stored as `"checkin":"0NaN-aN-aN"`.
`2031-02-30` (30 February) and `2031-13-01` (month 13) are also accepted with `200`. Test: `CreateBookingTest.bookingWithMalformedDateIsRejected`.

## BUG-09 – Wrong data types silently converted
```bash
curl -i -X POST https://restful-booker.herokuapp.com/booking -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"firstname":"Jim","lastname":"Brown","totalprice":"abc","depositpaid":"yes","bookingdates":{"checkin":"2031-03-10","checkout":"2031-03-15"}}'
```
**Expected:** `400`/`422`. **Actual:** `200`, stored with `"totalprice":null,"depositpaid":true`.
Test: `CreateBookingTest.bookingWithWrongDataTypeIsRejected[totalprice is text]`, `[depositpaid is text]`.

## BUG-03 – Missing or mistyped fields crash the server (500)
```bash
curl -i -X POST https://restful-booker.herokuapp.com/booking -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"lastname":"Brown","totalprice":250,"depositpaid":true,"bookingdates":{"checkin":"2031-03-10","checkout":"2031-03-15"}}'
```
**Expected:** `400`/`422` naming the missing field. **Actual:** `500`, body `Internal Server Error`.
Same for a missing `lastname`, `totalprice`, `depositpaid` or `bookingdates`, an empty `{}` payload, and `firstname` sent as a number.
Tests: `CreateBookingTest.bookingWithMissingFieldIsRejected` (6 rows), `bookingWithWrongDataTypeIsRejected[firstname is a number]`.

## BUG-04 – Failed login returns 200
```bash
curl -i -X POST https://restful-booker.herokuapp.com/auth -H 'Content-Type: application/json' -d '{"username":"admin","password":"wrong-password"}'
```
**Expected:** `401`, no token. **Actual:** `200 OK`, body `{"reason":"Bad credentials"}`. The same happens for an unknown user and for an empty body.
Test: `AuthTest.invalidCredentialsAreRejected`.

## BUG-10 – `checkin` search excludes the given day
Create a booking with check-in `2031-03-10`, then:
```bash
curl -s 'https://restful-booker.herokuapp.com/booking?lastname=<its lastname>&checkin=2031-03-10'   # actual: []
curl -s 'https://restful-booker.herokuapp.com/booking?lastname=<its lastname>&checkin=2031-03-09'   # actual: [{"bookingid":…}]
```
**Expected (API docs: "checkin date greater than or equal"):** the first search returns the booking. **Actual:** it does not.
The `checkout` filter does include the given day (that test passes). Test: `GetBookingTest.searchByCheckinIncludesTheSameDay`.

## BUG-11 – A decimal id returns a different booking
```bash
curl -i https://restful-booker.herokuapp.com/booking/1.5 -H 'Accept: application/json'
```
**Expected:** `404` (or `400`). **Actual:** `200` with the data of booking **1** (checked by hand: identical to `GET /booking/1`).
`abc` and `999999999` correctly return `404`. Test: `GetBookingTest.unknownOrInvalidIdIsNotFound[decimal id]`.

## BUG-13 – A booking with check-in in the past is accepted (needs product decision)
```bash
curl -i -X POST https://restful-booker.herokuapp.com/booking -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"firstname":"Jim","lastname":"Brown","totalprice":111,"depositpaid":true,"bookingdates":{"checkin":"2024-01-01","checkout":"2024-01-05"},"additionalneeds":"Breakfast"}'
```
**Expected:** `400`/`422` for a check-in before today. **Actual:** `200`, and the booking is stored.
**Why it needs a decision:** the sample booking in the API documentation is itself dated 2024, so either past bookings are meant to be
allowed (e.g. importing historical records) or the API **and** its documentation are both wrong. For a reservation hub that takes new
bookings from partners, a stay in the past is almost certainly invalid: it is the same kind of bad data as the production incident.
**Recommendation:** confirm the rule with product. If past check-ins are invalid, reject them with `400`/`422` and update the documentation example.
**How it was missed at first:** the suite used fixed 2031 dates for stability, so it never tested the boundary around today. The test now
builds its dates from today's date (`TestData.daysFromToday(-30)`), so "in the past" stays in the past on every run.
Test: `CreateBookingTest.bookingWithInvalidPriceOrDatesIsRejected[check-in in the past]`.

## BUG-05 – Unknown booking id returns 405 instead of 404
```bash
curl -i -X DELETE https://restful-booker.herokuapp.com/booking/999999999 -u admin:password123
```
**Expected:** `404`. **Actual:** `405 Method Not Allowed`. `PUT` on the same id also returns `405`.
Tests: `DeleteBookingTest.cancellingUnknownBookingReturns404`, `UpdateBookingTest.updatingUnknownBookingReturns404`.

## BUG-06 – Successful cancel returns 201 Created
Create a booking, then `curl -i -X DELETE https://restful-booker.herokuapp.com/booking/<id> -u admin:password123`.
**Expected:** `200` or `204`. **Actual:** `201 Created`. The booking is removed (a following GET gives `404`). Test: `DeleteBookingTest.cancelReturns200or204`.

## BUG-07 – Health check returns 201 Created
`curl -i https://restful-booker.herokuapp.com/ping` → **Expected** `200`. **Actual** `201 Created`. Test: `HealthCheckTest.pingReturns200`.

## BUG-12 – Multi-type Accept header gets 418
```bash
curl -i https://restful-booker.herokuapp.com/booking/1 -H 'Accept: application/json, application/javascript, text/javascript, text/json'
```
**Expected:** `200` with JSON (the list contains `application/json`). **Actual:** `418 I'm a teapot`. Same on `POST /booking`.
This is RestAssured's default `Accept` header for JSON, so it made every create and read fail until the suite sent exactly `application/json` (see `ApiClient`).

---
## What works correctly (passing tests)
- **Full journey:** create → read → PUT → PATCH → cancel → `404` works end to end, and every change is saved.
- **PATCH:** changes only the fields sent.
- **Write protection:** PUT, PATCH and DELETE with no token or a fake token are refused with `403`, and the booking is untouched.
- **Login:** a valid login returns a token.
- **Search:** searching by name finds exactly the booking, and the `checkout` filter includes the given day.
- **Unknown ids:** `999999999` and `abc` return `404`.
- **Response contracts:** the create, get and list responses match their JSON Schemas.
