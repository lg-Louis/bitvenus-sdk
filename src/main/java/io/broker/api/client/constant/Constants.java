package io.broker.api.client.constant;

public class Constants {

    public static ApiUser[] API_USERS = {
            new ApiUser(Test4.User01.ACCESS_KEY, Test4.User01.SECRET_KEY),
            new ApiUser(Test4.User03.ACCESS_KEY, Test4.User03.SECRET_KEY),
            new ApiUser("S1sL98FY7O3Q1mPBUFsNDIDyVIEIUw1xZ13QnDvO2R052tw9do6O3j9BAhWy6aD9", "Bk7GhSm3BhQ8phGMkWRnHXcaYWYiEJURZu3AAht2RVyonquxvzxP9PolbVYF3zMh"),
            new ApiUser("EV2GFL5sD4KP042frDC4xlLyhCUDdwvTz7SXFwTDzqwONMgt0cEiX6jrRqal7ZXR", "I5nLHr6GKilK0XfTwMm7nAJVj9L3CO3TZO0m8wHMPblZYYPpGVQGNmndBYtAmKLK")
    };

    public interface Test4 {

        interface User01 {
            String ACCESS_KEY = "qC54Kv8SbVloWrqT0MK9r8lp9owgWMNprBi5xU5Sbtbup799tGnqu9Fa5MgmkiBz";
            String SECRET_KEY = "ZRkWVbLkV7McoAGh5LIv1SCaR1vT0Kl9ErF5tbY5fx6csrTbkCG4fPOrHqQbLWMa";

            String API_BASE_URL = "https://www-t-4.dooob.ltd/";
            String WS_API_BASE_URL = "wss://wsapi-t-4.dooob.ltd/openapi/quote/ws/v1";
            String WS_API_USER_URL = "wss://wsapi-t-4.dooob.ltd/openapi/ws/";
        }

        interface User02 {
            String ACCESS_KEY = "RcHvxnnDClN5m9LUXojdi04PORA9rqjqwmXTBkrSgAlBN690Dx6HZTGcDUx6EePN";
            String SECRET_KEY = "toLG2vzIoeQfOLcp3oRvcCzzSEf4gZ751uwCL1ZFcA0krIXz0N4v5RFtPcqgV2KV";

            String API_BASE_URL = "https://www-t-4.dooob.ltd/";
            String WS_API_BASE_URL = "wss://wsapi-t-4.dooob.ltd/openapi/quote/ws/v1";
            String WS_API_USER_URL = "wss://wsapi-t-4.dooob.ltd/openapi/ws/";
        }

        interface User03 {
            String ACCESS_KEY = "jFJV2zgmUK3zSAyh1IgzNZzPq9ijBy5LtCIYQgUxCqn1ee7mNXzZ48qrAjej9JCn";
            String SECRET_KEY = "Zs8Xvq4CYQ1vnu979cFE0Ik2AXUgbpLEWIrVOVRZ0qIBOz0T8cUGVvAfyXDNuM4s";

            String API_BASE_URL = "https://www-t-4.dooob.ltd/";
            String WS_API_BASE_URL = "wss://wsapi-t-4.dooob.ltd/openapi/quote/ws/v1";
            String WS_API_USER_URL = "wss://wsapi-t-4.dooob.ltd/openapi/ws/";
        }
    }

    // FIXME: change to a valid key and secret for test
//    public final static String ACCESS_KEY = "GaTMTDkIBODnjUu0vInFmYZKOeCkiTW9pcsQTzSvulj3XmdEfVtlNEpcjsAnxPXV";
//    public final static String SECRET_KEY = "5uWMy4aDACBfT9KuqNfQZghvDUSOgs9B2H8Eemt8fwNkmbSKBagjdQzVP73i5zIe";
//
//    public static final String BASE_DOMAIN = "bitvenus.info";

    public final static String ACCESS_KEY = "x5ItoBkxA1xrkzvJwNKfWch1jZJM8NGLWWZjSA7ejkuNpyKDRpn8dQHjMl9KmPWl";
    public final static String SECRET_KEY = "JTew8mQ7osYMrTvo4qJ3xrPntRw0iC6cGiSGvzC6bhLK44wSnQtVPlQLzTlv7dZY";

    public static final String BASE_DOMAIN = "bitvenus.shop";

    // REST api url format: https://api.BASE_DOMAIN
    // for example: https://api.bhop.cloud
    public static final String API_BASE_URL = "https://api." + BASE_DOMAIN;

    // Websocket base api url format: wss://wsapi.BASE_DOMAIN/openapi/quote/ws/v1
    // for example: wss://wsapi.bhop.cloud/openapi/quote/ws/v1
    public static final String WS_API_BASE_URL = "wss://wsapi." + BASE_DOMAIN + "/openapi/quote/ws/v1";

    // Websocket user api url format: wss://wsapi.BASE_DOMAIN/openapi/ws
    // for example: wss://wsapi.bhop.cloud/openapi/ws
    public static final String WS_API_USER_URL = "wss://wsapi." + BASE_DOMAIN + "/openapi/ws/";
}
