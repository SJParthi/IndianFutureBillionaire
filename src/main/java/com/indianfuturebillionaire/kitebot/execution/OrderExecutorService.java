package com.indianfuturebillionaire.kitebot.execution;


import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.OrderParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(OrderExecutorService.class);
    private final KiteConnect kiteConnect;

    public OrderExecutorService(KiteConnect kc) {
        this.kiteConnect = kc;
    }

    public void placeMarketOrder(String side, String symbol, int qty) {
        try {
            if (kiteConnect == null) {
                logger.info("[MOCK] placeMarketOrder => side={}, symbol={}, qty={}", side, symbol, qty);
                return;
            }
            OrderParams params = new OrderParams();
            params.quantity = qty;
            params.tradingsymbol = symbol;
            params.transactionType = side;
            params.orderType = "MARKET";
            params.product = "MIS";
            params.exchange = "NSE";
            // The method returns an Order object in newer versions
            Order placedOrder = kiteConnect.placeOrder(params, "regular");
            logger.info(
                    "LIVE order => side={}, symbol={}, qty={}, orderId={}",
                    side, symbol, qty, placedOrder.orderId
            );
        } catch (KiteException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            logger.error("Order error => side={}, symbol={}, qty={}", side, symbol, qty, e);
        }
    }
}
