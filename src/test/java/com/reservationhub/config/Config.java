package com.reservationhub.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Reads settings from config.properties.
 * A -Dkey=value on the command line wins, so the same suite can point at another environment without code changes.
 */
public final class Config {

    private static final Properties FILE = load();

    public static final String BASE_URL = get("baseUrl");
    public static final String USERNAME = get("username");
    public static final String PASSWORD = get("password");
    public static final int TIMEOUT_MS = Integer.parseInt(get("timeoutMs"));
    public static final int WARMUP_SECONDS = Integer.parseInt(get("warmupSeconds"));

    private Config() {
    }

    private static String get(String key) {
        return System.getProperty(key, FILE.getProperty(key));
    }

    private static Properties load() {
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read config.properties", e);
        }
    }
}
