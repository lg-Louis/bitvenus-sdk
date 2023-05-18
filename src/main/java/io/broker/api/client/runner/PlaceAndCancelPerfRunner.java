package io.broker.api.client.runner;

import com.google.common.util.concurrent.RateLimiter;
import io.broker.api.client.BrokerContractApiRestClient;
import io.broker.api.client.domain.contract.*;
import io.broker.api.client.domain.contract.request.CancelContractOrderRequest;
import io.broker.api.client.domain.contract.request.ContractOpenOrderRequest;
import io.broker.api.client.domain.contract.request.ContractOrderRequest;
import lombok.val;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PlaceAndCancelPerfRunner implements Runnable, AutoCloseable {
    private final BrokerContractApiRestClient client;
    private final String symbol;
    private final long endTime;
    private final int nThreads;
    private LinkedBlockingQueue<Runnable> workQueue;
    private RateLimiter rateLimiter;
    private ThreadPoolExecutor executorService;

    public PlaceAndCancelPerfRunner(BrokerContractApiRestClient contractRestApiTest, int nThreads, int permitsPerSecond, long duration, String symbol) {
        this.client = contractRestApiTest;
        this.symbol = symbol;
        this.nThreads = nThreads;

        this.rateLimiter = RateLimiter.create(permitsPerSecond, 10, TimeUnit.SECONDS);
        this.workQueue = new LinkedBlockingQueue<Runnable>(nThreads * 2);
        this.executorService = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, workQueue, new ThreadPoolExecutor.CallerRunsPolicy());

        this.endTime = System.currentTimeMillis() + duration;
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
            if (workQueue.size() > nThreads) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }

            executorService.submit(() -> {
                rateLimiter.acquire();
                val clientOrderId = UUID.randomUUID().toString();
                try {

                    val placeOrdReq = ContractOrderRequest.builder()
                            .symbol(symbol)
                            .leverage("100")
                            .orderType(OrderType.LIMIT)
                            .side(OrderSide.BUY_OPEN)
                            .price("25130.8")
                            .quantity("5")
                            .priceType(PriceType.INPUT)
                            .timeInForce(TimeInForce.GTC)
                            .triggerPrice("")
                            .clientOrderId(clientOrderId)
                            .positionType(2)
                            .build();

                    val orderResult = client.newContractOrder(placeOrdReq);
                    val orderId = orderResult.getOrderId();
                    System.out.println("ordId: " + orderId);

                    executorService.submit(() -> {
                        try {
                            val cancelOrderRequest = CancelContractOrderRequest.builder().orderId(orderId).build();
                            client.cancelContractOrder(cancelOrderRequest);
                        } catch (Exception ignore) {
                            System.out.println("Cancel Order fail: cid: " + clientOrderId + ", " + ignore.getMessage());
                        }
                    });

                } catch (Exception ignore) {
                    System.out.println(ignore.getMessage() + ": cid: " + clientOrderId);
                }
            });
        }
    }

    private long clearOpenOrders(String symbol, Long fromOrderId) {
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