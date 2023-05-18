package io.broker.api.client.runner;

import com.google.common.util.concurrent.RateLimiter;
import io.broker.api.client.BrokerApiClientFactory;
import io.broker.api.client.BrokerContractApiRestClient;
import io.broker.api.client.domain.contract.*;
import io.broker.api.client.domain.contract.request.CancelContractOrderRequest;
import io.broker.api.client.domain.contract.request.ContractOpenOrderRequest;
import io.broker.api.client.domain.contract.request.ContractOrderRequest;
import io.broker.api.client.constant.Constants;
import lombok.val;
import lombok.var;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MarketTakerPerfRunner implements Runnable, AutoCloseable {
    private final BrokerContractApiRestClient client;
    private final String symbol;
    private final long endTime;
    private final int nThreads;
    private LinkedBlockingQueue<Runnable> workQueue;
    private RateLimiter rateLimiter;
    private ThreadPoolExecutor executorService;

    public MarketTakerPerfRunner(BrokerContractApiRestClient contractRestApiTest, int nThreads, int permitsPerSecond, long duration, String symbol) {
        this.client = contractRestApiTest;
        this.symbol = symbol;
        this.nThreads = nThreads;

        this.rateLimiter = RateLimiter.create(permitsPerSecond, 10, TimeUnit.SECONDS);
        this.workQueue = new LinkedBlockingQueue<Runnable>(nThreads * 2);
        this.executorService = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, workQueue, new ThreadPoolExecutor.CallerRunsPolicy());

        this.endTime = System.currentTimeMillis() + duration;
    }

    public static void main(String[] args) throws InterruptedException {
        val client = BrokerApiClientFactory.newInstance(Constants.Test4.User01.API_BASE_URL, Constants.Test4.User01.ACCESS_KEY, Constants.Test4.User01.SECRET_KEY).newContractRestClient();
        int nThreads = 2;
        val permitsPerSecond = 10;
        val duration = TimeUnit.MINUTES.toMillis(1);
        val symbol = "BTC-SWAP-USDT";

        final MarketTakerPerfRunner selfMatchPerfRunner = new MarketTakerPerfRunner(client, nThreads, permitsPerSecond, duration, symbol);
        selfMatchPerfRunner.once();
        TimeUnit.SECONDS.sleep(5);
        selfMatchPerfRunner.close();
    }

    @Override
    public void run() {

//
//        clearOpenOrders(symbol, null);
//
//        executorService.submit(() -> {
//            Long fromOrdId = null;
//            while (true) {
//                fromOrdId = clearOpenOrders(symbol, fromOrdId);
//                TimeUnit.SECONDS.sleep(5);
//            }
//        });

        while (System.currentTimeMillis() < endTime) {
            if (workQueue.size() > this.nThreads) {
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }

            executorService.submit(() -> {
                rateLimiter.acquire();
                once();
            });
        }
    }

    private void once() {
        var takerClientOrderId = UUID.randomUUID().toString();
        val takerOrdReq = ContractOrderRequest.builder()
                .symbol(symbol)
                .leverage("100")
                .orderType(OrderType.LIMIT)
                .side(OrderSide.SELL_OPEN)
                .quantity("5")
                .price("0")
                .priceType(PriceType.MARKET)
                .timeInForce(TimeInForce.GTC)
                .triggerPrice("")
                .clientOrderId(takerClientOrderId)
                .positionType(2)
                .build();
        try {
            var takerOrdResult = client.newContractOrder(takerOrdReq);
            val takerOrdId = takerOrdResult.getOrderId();
            System.out.println("Taker ordId: " + takerOrdId);
            executorService.submit(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(300);
                    val cancelOrderRequest = CancelContractOrderRequest.builder().orderId(takerOrdId).build();
                    client.cancelContractOrder(cancelOrderRequest);
                } catch (Exception ignore) {
                    System.out.println("Cancel Sell Taker Order fail: oid: " + takerOrdId + ", " + ignore.getMessage());
                }
            });
        } catch (Exception e) {
            System.out.println("Sell taker order place occur an err:" + e.getMessage() + ",cid = " + takerClientOrderId);
        }
    }

    private Long clearOpenOrders(String symbol, Long fromOrderId) {
        while (true) {
            val orderRequestBuilder = ContractOpenOrderRequest.builder().symbol(symbol).limit(500);

            if (fromOrderId != null && fromOrderId > 0) {
                orderRequestBuilder.orderId(fromOrderId);
            }
            val openOrders = client.getContractOpenOrders(orderRequestBuilder.build());
            if (openOrders.isEmpty()) break;


            for (ContractOrderResult order : openOrders) {
                val orderId = order.getOrderId();
                try {
                    client.cancelContractOrder(CancelContractOrderRequest.builder().orderId(orderId).build());
                } catch (Exception ignore) {
                    System.out.println("Cancel Order fail: ordId: " + orderId + ", " + ignore.getMessage());
                }
                fromOrderId = fromOrderId == null ? orderId : Math.max(orderId, fromOrderId);
            }
        }
        return fromOrderId;
    }

    @Override
    public void close() {
        executorService.shutdown();
        clearOpenOrders(symbol, null);
    }
}