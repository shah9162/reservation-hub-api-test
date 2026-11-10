package com.reservationhub.api;

import com.reservationhub.config.Config;
import io.restassured.response.Response;

import java.util.Map;

/** POST /auth. */
public final class AuthApi {

    private AuthApi() {
    }

    public static Response createToken(Map<String, ?> credentials) {
        return ApiClient.request().body(credentials).post("/auth");
    }

    /** A token for the configured admin user. Fails clearly here rather than as a confusing 403 later. */
    public static String getToken() {
        Response response = createToken(Map.of("username", Config.USERNAME, "password", Config.PASSWORD));
        String token = response.jsonPath().getString("token");
        if (token == null) {
            throw new IllegalStateException("Could not get an auth token. Response: " + response.asString());
        }
        return token;
    }
}
