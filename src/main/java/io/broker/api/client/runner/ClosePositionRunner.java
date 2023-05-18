package io.broker.api.client.runner;

import com.google.common.util.concurrent.RateLimiter;
import io.broker.api.client.BrokerApiClientFactory;
import io.broker.api.client.BrokerContractApiRestClient;
import io.broker.api.client.constant.Constants;
import io.broker.api.client.domain.contract.*;
import io.broker.api.client.domain.contract.request.ContractOrderRequest;
import io.broker.api.client.domain.contract.request.ContractPositionRequest;
import lombok.val;
import lombok.var;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ClosePositionRunner implements Runnable, AutoCloseable {
    private final BrokerContractApiRestClient client;
    private final String symbol;
    private final long endTime;
    private final int nThreads;
    private LinkedBlockingQueue<Runnable> workQueue;
    private RateLimiter rateLimiter;
    private ThreadPoolExecutor executorService;

    public ClosePositionRunner(BrokerContractApiRestClient contractRestApiTest, int nThreads, int permitsPerSecond, long duration, String symbol) {
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

        final ClosePositionRunner selfMatchPerfRunner = new ClosePositionRunner(client, nThreads, permitsPerSecond, duration, symbol);
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
        val positionRequest = ContractPositionRequest.builder()
                .symbol(symbol)
                .build();
        try {
            val positions = client.getContractPositions(positionRequest);
            if (positions.isEmpty()) return;

            for (ContractPositionResult position : positions) {
                val availableStr = position.getAvailable();
                val available = new BigDecimal(availableStr);
                if (available.signum() > 0) {
                    if (available.compareTo(BigDecimal.ONE) > 0)
                        marketClosePosition(position);
                } else if (available.signum() < 0) {
                    System.err.println("position " + position.getSymbol() + "-" + position.getSide() + ": available is negative" + availableStr);
                }
            }
        } catch (Exception e) {
            System.out.println("Get OpenPositions fail: " + e.getMessage());
        }
    }

    private void marketClosePosition(ContractPositionResult position) {
        var closeClientOrderId = UUID.randomUUID().toString();
        val closeOrdReq = ContractOrderRequest.builder()
                .symbol(symbol)
                .leverage("100")
                .orderType(OrderType.LIMIT)
                .side(position.getSide() == PositionSide.LONG ? OrderSide.SELL_CLOSE : OrderSide.BUY_CLOSE)
                .quantity("1")
                .price("0")
                .priceType(PriceType.MARKET)
                .timeInForce(TimeInForce.GTC)
                .triggerPrice("")
                .clientOrderId(closeClientOrderId)
                .positionType(2)
                .build();
        try {
            var closeOrdResult = client.newContractOrder(closeOrdReq);
            val closeOrdId = closeOrdResult.getOrderId();
            System.out.println("Close ordId: " + closeOrdId + ", " +
                    "close Position +" + position.getSide() + "-" + position.getSymbol() + ":" +
                    closeOrdReq.getSide() + "-" + closeOrdReq.getQuantity());
        } catch (Exception e) {
            System.out.println("Close order place occur an err:" + e.getMessage() + ",cid = " + closeClientOrderId);
        }
    }

    @Override
    public void close() {
        executorService.shutdown();
    }
}