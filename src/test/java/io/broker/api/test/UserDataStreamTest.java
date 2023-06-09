package io.broker.api.test;

import io.broker.api.client.BrokerApiClientFactory;
import io.broker.api.client.BrokerApiRestClient;
import io.broker.api.client.BrokerApiWebSocketClient;
import io.broker.api.client.BrokerContractApiRestClient;
import io.broker.api.client.constant.BrokerConstants;
import io.broker.api.client.domain.account.*;
import io.broker.api.client.domain.account.request.OrderStatusRequest;
import io.broker.api.client.domain.contract.ContractAccountResult;
import io.broker.api.client.domain.contract.ContractOrderResult;
import io.broker.api.client.domain.contract.OrderSide;
import io.broker.api.client.domain.contract.OrderType;
import io.broker.api.client.domain.contract.PriceType;
import io.broker.api.client.domain.contract.TimeInForce;
import io.broker.api.client.domain.contract.request.CancelContractOrderRequest;
import io.broker.api.client.domain.contract.request.ContractOrderRequest;
import io.broker.api.client.constant.Constants;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class UserDataStreamTest {

    static BrokerApiClientFactory factory = BrokerApiClientFactory.newInstance(Constants.API_BASE_URL, Constants.ACCESS_KEY, Constants.SECRET_KEY);
    static BrokerApiWebSocketClient wsClient = factory.newWebSocketClient(Constants.WS_API_BASE_URL, Constants.WS_API_USER_URL);
    static BrokerApiRestClient restClient = factory.newRestClient();
    static BrokerContractApiRestClient contractApiRestClient = factory.newContractRestClient();

    public static void main(String[] args) {

        log.info("\n ------Get Listen Key -----");
        String listenKey = restClient.startUserDataStream(BrokerConstants.DEFAULT_RECEIVING_WINDOW, System.currentTimeMillis());
        log.info("listenKey:" + listenKey);

        // check received msg time delay
        wsClient.onUserEvent(listenKey, response -> {
            long now = System.currentTimeMillis();
            List<SocketOrder> orderList = response.getOrderList();
            List<SocketAccount> accountList = response.getAccountList();
            log.info("received event: {}", response);

            if (orderList != null && orderList.size() > 0) {
                SocketOrder order = orderList.get(0);
                long costTime1 = now - order.getCreateTime();
                long costTime2 = now - order.getEventTime();
                if (costTime2 > 100L) {
                    log.warn("received event {}, order id {}, \n -- order status {}, costTime1 {}, costTime2 {}",
                            order.getEventType(), order.getOrderId(), order.getStatus(), costTime1, costTime2);
                } else {
                    log.info("received event {}, order id {}, \n -- order status {}, costTime1 {}, costTime2 {}",
                            order.getEventType(), order.getOrderId(), order.getStatus(), costTime1, costTime2);
                }
            }

            if (accountList != null && accountList.size() > 0) {
                SocketAccount account = accountList.get(0);
                List<SocketBalance> balances = account.getBalanceChangedList();
                long costTime1 = now - account.getLastUpdated();
                long costTime2 = now - account.getEventTime();
                log.info("received event {}, Account Balance changed size {}, costTime1 {}, costTime2 {}",
                        account.getEventType(), balances.size(), costTime1, costTime2);
            }
        }, true);

//        new Thread(() -> {
//            startPlaceOrder();
//        }).start();
    }

    private static void startPlaceOrder() {
        final Map<String, ContractAccountResult> account = contractApiRestClient.getContractAccount();
        log.info("start place order using account: {}", account);

        for (int i = 0; i < 2; i++) {
            try {
                // HBCUSDT now price 3.2 , so it will not fill
                final ContractOrderResult response = contractApiRestClient.newContractOrder(ContractOrderRequest.builder()
                        .side(OrderSide.BUY_OPEN)
                        .timeInForce(TimeInForce.GTC)
                        .orderType(OrderType.LIMIT)
                        .priceType(PriceType.INPUT)
                        .clientOrderId("TT" + System.currentTimeMillis())
                        .symbol("BTC-SWAP-USDT")
                        .price("22000")
                        .quantity("10")
                        .build());
//            NewOrderResponse response = restClient.newOrder(NewOrder.limitBuy("BTC-SWAP-USDT", TimeInForce.GTC, "10", "1"));

                safeSleep(100L);
                try {
                    //                restClient.cancelOrder(new CancelOrderRequest(response.getOrderId()));
                    contractApiRestClient.cancelContractOrder(CancelContractOrderRequest.builder().orderId(response.getOrderId()).build());
                } catch (Exception e) {
                    log.error("cancel order catch exception, order {}, exception {}", response, e);

                    //                Order order2 = restClient.getOrderStatus(new OrderStatusRequest(response.getOrderId()));
                    final ContractOrderResult order = contractApiRestClient.getContractOrder(OrderType.LIMIT, "", response.getOrderId());

                    log.info("order is {}", order);
                }
                Order order = restClient.getOrderStatus(new OrderStatusRequest(response.getOrderId()));
                //log.info("new order no.{}, order id {}, status: {}", i, order.getOrderId(), order.getStatus());
                safeSleep(100L);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    private static void safeSleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
