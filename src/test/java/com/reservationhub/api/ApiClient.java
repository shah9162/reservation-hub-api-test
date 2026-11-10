package com.reservationhub.api;

import com.reservationhub.config.Config;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/** The shared starting point for every request, plus the cold-start wait. */
public final class ApiClient {

    private static final RestAssuredConfig TIMEOUTS = RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", Config.TIMEOUT_MS)
                    .setParam("http.socket.timeout", Config.TIMEOUT_MS))
            .encoderConfig(EncoderConfig.encoderConfig()
                    .appendDefaultContentCharsetToContentTypeIfUndefined(false));

    private ApiClient() {
    }

    /**
     * Base URL, JSON headers and timeouts. The Allure filter attaches every request and response
     * to the report, so a failure can be understood without re-running anything.
     */
    public static RequestSpecification request() {
        return RestAssured.given()
                .baseUri(Config.BASE_URL)
                .contentType(ContentType.JSON)
                // Exactly "application/json": the API answers 418 to RestAssured's default list of JSON types (BUG-12).
                .accept("application/json")
                .config(TIMEOUTS)
                .filter(new AllureRestAssured());
    }

    /** Same as request(), with the auth cookie. A null token sends no auth at all. */
    public static RequestSpecification request(String token) {
        RequestSpecification request = request();
        if (token != null) {
            request.cookie("token", token);
        }
        return request;
    }

    /**
     * The shared Heroku instance sleeps when idle. Instead of letting the first tests fail on a cold start,
     * poll /ping until it answers (up to warmupSeconds), then start testing.
     */
    public static void waitUntilApiIsUp() {
        long deadline = System.currentTimeMillis() + Config.WARMUP_SECONDS * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                int status = RestAssured.given().baseUri(Config.BASE_URL).config(TIMEOUTS).get("/ping").statusCode();
                if (status >= 200 && status < 300) {
                    return;
                }
            } catch (Exception serverNotAwakeYet) {
                // keep waiting
            }
            pause(3000);
        }
        throw new IllegalStateException(Config.BASE_URL + " did not respond within " + Config.WARMUP_SECONDS + " seconds");
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
