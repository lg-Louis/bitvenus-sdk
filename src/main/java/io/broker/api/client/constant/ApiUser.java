package io.broker.api.client.constant;

public class ApiUser {
    public final String ACCESS_KEY;
    public final String SECRET_KEY;

    public ApiUser(String accessKey, String secretKey) {
        ACCESS_KEY = accessKey;
        SECRET_KEY = secretKey;
    }
}
