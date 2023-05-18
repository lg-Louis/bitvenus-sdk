package io.broker.api.test;

import com.google.common.collect.Lists;
import io.broker.api.client.BrokerApiClientFactory;
import io.broker.api.client.BrokerContractApiRestClient;
import io.broker.api.client.constant.Constants;
import io.broker.api.client.domain.contract.*;
import io.broker.api.client.domain.contract.request.*;
import io.broker.api.client.domain.general.BrokerInfo;
import io.broker.api.client.domain.market.Candlestick;
import io.broker.api.client.domain.market.CandlestickInterval;
import io.broker.api.client.domain.market.OrderBook;
import io.broker.api.client.domain.market.TradeHistoryItem;
import io.broker.api.client.runner.ClosePositionRunner;
import io.broker.api.client.runner.MarketTakerPerfRunner;
import io.broker.api.client.runner.PlaceAndCancelPerfRunner;
import io.broker.api.client.runner.SelfMatchPerfRunner;
import lombok.val;
import lombok.var;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ContractRestApiTest {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO");
    }

    public static void main(String[] args) {
        System.out.println("\n ------get broker info-----");
        val client = getContractApiRestClient(Constants.Test4.User01.API_BASE_URL, Constants.Test4.User01.ACCESS_KEY, Constants.Test4.User01.SECRET_KEY);

        BrokerInfo brokerInfo = client.getBrokerInfo(null);
        System.out.println(brokerInfo);

        System.out.println("\n ------new contract order-----");
        ContractOrderResult orderResult = client.newContractOrder(
                ContractOrderRequest.builder()
                        .symbol("BTC0808")
                        .leverage("10")
                        .orderType(OrderType.LIMIT)
                        .side(OrderSide.BUY_OPEN)
                        .price("8000")
                        .quantity("5")
                        .priceType(PriceType.INPUT)
                        .timeInForce(TimeInForce.GTC)
                        .triggerPrice("")
                        .clientOrderId(UUID.randomUUID().toString())
                        .build()
        );

        System.out.println(orderResult);

        System.out.println("\n ------get contract open orders-----");
        List<ContractOrderResult> openOrders = client.getContractOpenOrders(
                ContractOpenOrderRequest.builder()
                        .symbol("BTC0808")
                        .limit(10)
                        .side(OrderSide.BUY_OPEN)
                        .build()
        );
        System.out.println(openOrders);

        System.out.println("\n ------get contract order info-----");
        ContractOrderResult sOrderResult = client.getContractOrder(OrderType.LIMIT, "", 478799682544402176L);
        System.out.println(sOrderResult);

        System.out.println("\n ------get contract history orders-----");
        List<ContractOrderResult> historyOrders = client.getContractHistoryOrders(
                ContractHistoryOrderRequest.builder()
                        .symbol("BTC0808")
                        .limit(10)
                        .side(OrderSide.BUY_OPEN)
                        .build()
        );
        System.out.println(historyOrders);

        System.out.println("\n ------cancel contract order-----");
        ContractOrderResult orderResultCancel = client.cancelContractOrder(
                CancelContractOrderRequest.builder()
                        .clientOrderId(orderResult.getClientOrderId())
                        .orderType(orderResult.getOrderType())
                        .build()
        );
        System.out.println(orderResultCancel);

        System.out.println("\n ------batch cancel contract order-----");
        BatchCancelOrderResult batchCancelOrderResult = client.batchCancelContractOrder(
                BatchCancelOrderRequest.builder()
                        .symbolList(Lists.newArrayList("BTC0808"))
                        .build()
        );

        System.out.println("\n ------get contract my trades-----");
        List<ContractMatchResult> matchResults = client.getContractMyTrades(
                ContractMyTradeRequest.builder()
                        .symbol("BTC0808")
                        //.side(OrderSide.BUY)
                        .limit(10)
                        .build()
        );
        System.out.println(matchResults);

        System.out.println("\n ------get contract positions-----");
        List<ContractPositionResult> positionResults = client.getContractPositions(
                ContractPositionRequest.builder()
                        .symbol("BTC0808")
                        .side(PositionSide.LONG)
                        .build()
        );
        System.out.println(positionResults);


        System.out.println("\n ------modify contract margin-----");
        ModifyMarginResult modifyMarginResult = client.modifyMargin(
                ModifyMarginRequest.builder()
                        .symbol("BTC0808")
                        .side(PositionSide.LONG)
                        .amount("0.01")
                        .build()
        );
        System.out.println(modifyMarginResult);

        System.out.println("\n ------get contract account-----");
        Map<String, ContractAccountResult> accountResultMap = client.getContractAccount();
        System.out.println(accountResultMap);

        System.out.println("\n ------get contract depth-----");
        OrderBook orderBook = client.getContractOrderBook("BTC0808", null);
        System.out.println(orderBook);

        System.out.println("\n ------get contract recent trades-----");
        List<TradeHistoryItem> tradeHistoryItems = client.getContractTrades("BTC0808", null);
        System.out.println(tradeHistoryItems);

        System.out.println("\n ------get contract klines-----");
        List<Candlestick> candlesticks = client.getContractCandlestickBars("BTC0808", CandlestickInterval.ONE_MINUTE, null, null);
        System.out.println(candlesticks);
    }

    private static BrokerContractApiRestClient getContractApiRestClient(String apiBaseUrl, String accessKey, String secretKey) {
        return BrokerApiClientFactory.newInstance(apiBaseUrl, accessKey, secretKey).newContractRestClient();
    }
}
