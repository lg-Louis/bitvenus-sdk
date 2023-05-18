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

public class SelfMatchPerfRunner implements Runnable, AutoCloseable {
    private final BrokerContractApiRestClient client;
    private final String symbol;
    private final long endTime;
    private final int nThreads;
    private LinkedBlockingQueue<Runnable> workQueue;
    private RateLimiter rateLimiter;
    private ThreadPoolExecutor executorService;

    public SelfMatchPerfRunner(BrokerContractApiRestClient contractRestApiTest, int nThreads, int permitsPerSecond, long duration, String symbol) {
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

        final SelfMatchPerfRunner selfMatchPerfRunner = new SelfMatchPerfRunner(client, nThreads, permitsPerSecond, duration, symbol);
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
        var buyClientOrderId = UUID.randomUUID().toString();
        val buyOrdReq = ContractOrderRequest.builder()
                .symbol(symbol)
                .leverage("100")
                .orderType(OrderType.LIMIT)
                .side(OrderSide.BUY_OPEN)
                .price("25230.8")
                .quantity("50")
                .priceType(PriceType.INPUT)
                .timeInForce(TimeInForce.GTC)
                .triggerPrice("")
                .clientOrderId(buyClientOrderId)
                .positionType(2)
                .build();

        try {
            val buyOrdResult = client.newContractOrder(buyOrdReq);
            Long buyOrdId = buyOrdResult.getOrderId();
            System.out.println("Buy ordId: " + buyOrdId);

            executorService.submit(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(300);
                    val cancelOrderRequest = CancelContractOrderRequest.builder().orderId(buyOrdId).build();
                    client.cancelContractOrder(cancelOrderRequest);
                } catch (Exception ignore) {
                    System.out.println("Cancel Buy Order fail: oid: " + buyOrdId + ", " + ignore.getMessage());
                }
            });
        } catch (Exception e) {
            System.out.println(e.getMessage() + "Buy order place occur an err:" + e.getMessage() + ",cid = " + buyClientOrderId);
        }

        var sellClientOrderId = UUID.randomUUID().toString();
        val sellOrdReq = ContractOrderRequest.builder()
                .symbol(symbol)
                .leverage("100")
                .orderType(OrderType.LIMIT)
                .side(OrderSide.SELL_OPEN)
                .quantity("50")
                .price("0")
                .priceType(PriceType.MARKET)
                .timeInForce(TimeInForce.GTC)
                .triggerPrice("")
                .clientOrderId(sellClientOrderId)
                .positionType(2)
                .build();

        try {
            var sellOrdResult = client.newContractOrder(sellOrdReq);
            val sellOrdId = sellOrdResult.getOrderId();
            System.out.println("Buy ordId: " + sellOrdId);

            executorService.submit(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(300);
                    val cancelOrderRequest = CancelContractOrderRequest.builder().orderId(sellOrdId).build();
                    client.cancelContractOrder(cancelOrderRequest);
                } catch (Exception ignore) {
                    System.out.println("Cancel Sell Order fail: oid: " + sellOrdId + ", " + ignore.getMessage());
                }
            });
        } catch (Exception e) {
            System.out.println("Sell order place occur an err:" + e.getMessage() + ",cid = " + sellClientOrderId);
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