package com.reservationhub.tests;

import com.reservationhub.api.AuthApi;
import com.reservationhub.api.BookingApi;
import com.reservationhub.config.Config;
import com.reservationhub.data.TestData;
import com.reservationhub.model.Booking;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.testng.Assert.assertEquals;

@Epic("Bookings API")
@Feature("Authentication")
public class AuthTest extends BaseTest {

    @Test(groups = "smoke", description = "Valid credentials return a token")
    @Severity(SeverityLevel.BLOCKER)
    public void validCredentialsReturnAToken() {
        AuthApi.createToken(Map.of("username", Config.USERNAME, "password", Config.PASSWORD))
                .then()
                .statusCode(200)
                .body("token", not(emptyOrNullString()))
                .body(matchesJsonSchemaInClasspath("schemas/token.schema.json"));
    }

    @Test(dataProvider = "invalidCredentials", dataProviderClass = TestData.class,
            description = "Invalid credentials are refused and no token is issued (BUG-04)")
    @Severity(SeverityLevel.CRITICAL)
    public void invalidCredentialsAreRejected(String scenario, Map<String, Object> credentials) {
        Response response = AuthApi.createToken(credentials);

        assertUnauthorised(response);
        response.then().body("token", nullValue());
    }

    @Test(groups = "smoke", dataProvider = "missingOrInvalidToken", dataProviderClass = TestData.class,
            description = "PUT without a valid token is refused and the booking is not changed")
    @Severity(SeverityLevel.BLOCKER)
    public void putWithoutValidTokenIsRejected(String scenario, String badToken) {
        Booking original = TestData.validBooking();
        int id = createBooking(original);

        assertUnauthorised(BookingApi.update(id, TestData.otherValidBooking(), badToken));
        assertEquals(readBooking(id), original, "booking must not change");
    }

    @Test(groups = "smoke", dataProvider = "missingOrInvalidToken", dataProviderClass = TestData.class,
            description = "PATCH without a valid token is refused and the booking is not changed")
    @Severity(SeverityLevel.BLOCKER)
    public void patchWithoutValidTokenIsRejected(String scenario, String badToken) {
        Booking original = TestData.validBooking();
        int id = createBooking(original);

        assertUnauthorised(BookingApi.partialUpdate(id, Map.of("firstname", "Intruder"), badToken));
        assertEquals(readBooking(id), original, "booking must not change");
    }

    @Test(groups = "smoke", dataProvider = "missingOrInvalidToken", dataProviderClass = TestData.class,
            description = "DELETE without a valid token is refused and the booking still exists")
    @Severity(SeverityLevel.BLOCKER)
    public void deleteWithoutValidTokenIsRejected(String scenario, String badToken) {
        Booking original = TestData.validBooking();
        int id = createBooking(original);

        assertUnauthorised(BookingApi.delete(id, badToken));
        assertEquals(readBooking(id), original, "booking must still exist");
    }
}
