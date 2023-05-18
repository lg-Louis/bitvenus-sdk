package io.broker.api.client.runner;

import io.broker.api.client.BrokerApiClientFactory;
import io.broker.api.client.BrokerContractApiRestClient;
import io.broker.api.client.constant.Constants;
import lombok.val;
import lombok.var;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class App {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO");
        val eth = "ETH-SWAP-USDT";
        val eos = "EOS-SWAP-USDT";
        val ltc = "LTC-SWAP-USDT";
    }


    public static void main(String[] args) throws InterruptedException {
        System.out.println(ReflectionToStringBuilder.toString(args, ToStringStyle.JSON_STYLE));

        var apiBaseUrl = Constants.Test4.User01.API_BASE_URL;

        int nThreads = 300;
        int permitsPerSecond = 100;

        val btc = "BTC-SWAP-USDT";
        var duration = TimeUnit.MINUTES.toMillis(30);
        String[] symbols = new String[]{btc};

        if (args.length >= 1) permitsPerSecond = Integer.parseInt(args[0]);
        if (args.length >= 2) nThreads = Integer.parseInt(args[1]);
        if (args.length >= 3) symbols = args[2].split(",");
        if (args.length >= 4) apiBaseUrl = args[3];
        if (args.length >= 5) duration = TimeUnit.MINUTES.toMillis(Long.parseLong(args[4]));


        System.out.println("apiBaseUrl: " + apiBaseUrl + ", " +
                "nThreads: " + nThreads + ", " +
                "permitsPerSecond: " + permitsPerSecond + ", " +
                "symbols: " + ReflectionToStringBuilder.toString(symbols, ToStringStyle.JSON_STYLE) + ", " +
                "durationInMinutes: " + TimeUnit.MILLISECONDS.toMinutes(duration));

        if (symbols.length == 0) return;

        val service = Executors.newFixedThreadPool(10);
        val normalUserClient = getContractApiRestClient(apiBaseUrl, Constants.Test4.User02.ACCESS_KEY, Constants.Test4.User02.SECRET_KEY);

        val userCount = Constants.API_USERS.length;

        for (int i = 0; i < symbols.length; i++) {
            val symbol = symbols[i];
            val apiUser = Constants.API_USERS[i % userCount];
            val client = getContractApiRestClient(apiBaseUrl, apiUser.ACCESS_KEY, apiUser.SECRET_KEY);
            start(symbol, client, duration, normalUserClient, service, nThreads, permitsPerSecond);
        }

        TimeUnit.MILLISECONDS.sleep(duration);
        service.shutdown();
    }

    private static void start(String btc, BrokerContractApiRestClient btcMMClient, long duration, BrokerContractApiRestClient normalUserClient,
                              ExecutorService service, int nThreads, int permitsPerSecond) {
        final PlaceAndCancelPerfRunner perfRunner = new PlaceAndCancelPerfRunner(btcMMClient, nThreads, permitsPerSecond, duration, btc);
        service.submit(perfRunner);

        final SelfMatchPerfRunner selfMatchPerfRunner = new SelfMatchPerfRunner(btcMMClient, 5, 5, duration, btc);
        service.submit(selfMatchPerfRunner);

        final MarketTakerPerfRunner marketTakerPerfRunner = new MarketTakerPerfRunner(normalUserClient, 5, 5, duration, btc);
        service.submit(marketTakerPerfRunner);

        final ClosePositionRunner btcMMclosePositionRunner = new ClosePositionRunner(btcMMClient, 5, 5, duration, btc);
        service.submit(btcMMclosePositionRunner);

        final ClosePositionRunner normalClosePositionRunner = new ClosePositionRunner(normalUserClient, 2, 1, duration, btc);
        service.submit(normalClosePositionRunner);
    }

    private static BrokerContractApiRestClient getContractApiRestClient(String apiBaseUrl, String accessKey, String secretKey) {
        return BrokerApiClientFactory.newInstance(apiBaseUrl, accessKey, secretKey).newContractRestClient();
    }
}
