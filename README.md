# Reservation Hub – Bookings API tests

Automated safety net for the Bookings API (Restful-booker), written in **Java 17 + REST Assured + TestNG**, with an **Allure** report.

## How to run

**What runs is controlled by `src/test/resources/testng.xml`**: one `<test>` block per feature. Comment a block out to skip that
feature, or switch on the `smoke` group at the top of the file to run only the smoke tests.

- **Eclipse, quick run:** right-click `testng.xml` → **Run As → TestNG Suite**. This runs the tests; it does not build the HTML report.
- **Eclipse, run + report:** right-click the project → **Run As → Maven test**.
- **Command line:** `mvn test`.

The Maven run executes the same `testng.xml` and then builds the report. Open **`reports/allure-report/index.html`** by double-clicking it. It is a single
file and needs no server. Failing tests do not stop the build, because the failures are the API's defects (see `BUGS.md`).
In CI, add `-Dmaven.test.failure.ignore=false` so the build goes red when a test fails.
Settings live in `src/test/resources/config.properties`, and any of them can be overridden, e.g. `mvn test -DbaseUrl=https://other-host`.

## Project layout

| Folder (`src/test/java/com/reservationhub/`) | What it holds |
|---|---|
| `config/Config` | Base URL, credentials and timeouts, read from `config.properties` |
| `api/ApiClient`, `AuthApi`, `BookingApi` | How to call the API: one method per endpoint, no assertions |
| `model/Booking`, `BookingDates`, `CreateBookingResponse` | Java classes for the JSON. Jackson **serializes** them into request bodies and **deserializes** responses back into them, so a whole response can be compared with one `assertEquals`. `CreateBookingTest.validBookingIsCreated` does both steps explicitly with `ObjectMapper` and attaches the JSON to the report |
| `data/TestData` | Valid bookings with unique names, plus the invalid inputs for the data-driven tests |
| `tests/` | One class per feature. `BaseTest` holds the shared setup and cleanup |
| `src/test/resources/testng.xml` | Which tests run, grouped by feature |
| `src/test/resources/schemas` | JSON Schemas for the response contract tests |

## Test strategy

I covered the endpoints in order of risk, starting from the production incident.

1. **The incident (smoke).** A negative or zero price, or check-out before check-in, must be rejected on create, on PUT *and* on PATCH.
   A rejected update must also leave the booking unchanged.
2. **Core journey (smoke).** Create → read → PUT → PATCH → cancel → gone. Each step checks the response body *and* that the change was saved, not only the status code.
3. **Write protection (smoke).** PUT, PATCH and DELETE with no token or a fake token must be refused, and the booking must be untouched afterwards.
4. **Input validation.** Missing fields, wrong data types and malformed dates (data-driven: one test method per risk, one row per case).
   A correct API answers 4xx, never 2xx and never a 500 crash.
5. **Auth.** A valid login returns a token. Bad logins are refused and get no token.
6. **Read and search.** Unknown or invalid ids return 404, name search, and same-day `checkin`/`checkout` search.
7. **Contract.** The create, get and list responses match JSON Schemas.

Tests assert what a *correct* API should do, so tests fail where the API is wrong. Each such test names its bug in the description, e.g. `(BUG-01)`.
I accept either of two equally valid codes where the choice is only style: 400 or 422 for bad input, 401 or 403 for refused writes.

**Deliberately left out:**
- **Load testing:** a shared free instance gives meaningless numbers.
- **Very long or unicode strings, and injection fuzzing:** there is no stated requirement, so this needs a product decision on limits.
- **Same-day check-in/check-out:** this is a product decision.
- **HTTP Basic auth for writes:** token auth is the documented main path.
- **XML responses:** not in scope.

**Assumptions to confirm with product:**
- **Zero price:** treated as invalid. If free bookings are allowed, delete that row in `TestData.incidentCases`.
- **Check-in in the past:** treated as invalid (BUG-13), even though the documentation's own sample booking is dated 2024. If past bookings are allowed (e.g. imports), delete that row.

## Shared, self-resetting environment and flakiness

- **Independent tests:** every test creates its own booking with a random name and dates in 2031. Nothing depends on the 10 seeded records, on test order, or on another test.
- **Cleanup:** `BaseTest` deletes every booking a test created, after each test, even if it failed. That includes bookings the API wrongly accepted in negative tests.
- **Tokens:** the server's ~10-minute reset invalidates old tokens, so a fresh token is fetched before each test.
- **Cold start:** before the first test, `ApiClient.waitUntilApiIsUp()` polls `/ping` for up to 90 s. If the server never wakes up, the run stops with one clear message instead of dozens of timeouts. Each request has a 30 s timeout.
- **Setup failures stay contained:** `testng.xml` uses `configfailurepolicy="continue"`, so if the slow shared server times out while one test is getting its token, only that test is affected. TestNG's default would skip the rest of the class (this happened in one run).
- **No automatic test retries:** retries would hide real intermittent defects.

## Known limitations

- **Reset race:** the reset can still delete a booking in the middle of a test, which shows up as an unexpected 404. It is rare because each test takes about a second. If it happens, re-run.
- **Sequential runs:** tests run one after another, to be gentle on the shared server.
- **Hand-written schemas:** there is no OpenAPI file, so the schemas come from the docs. They reject unknown fields on purpose, so a new response field will fail the contract tests until the schema is updated.
- **Dates relative to today:** most tests use fixed 2031 dates for stability. The one exception is the past-check-in case, which builds its dates from today so it never goes stale. A gap I found after the first submission was that nothing tested the boundary around today (BUG-13).
- **Exact `Accept` header:** requests send `Accept: application/json` exactly, because the API answers `418` to the usual list of JSON types (BUG-12).
