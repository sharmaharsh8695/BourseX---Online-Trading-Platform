package com.harsh.finance_project;

import com.harsh.finance_project.asset.model.Asset;
import com.harsh.finance_project.asset.model.AssetStatus;
import com.harsh.finance_project.asset.repository.AssetRepository;
import com.harsh.finance_project.holding.model.Holding;
import com.harsh.finance_project.holding.repository.HoldingRepository;
import com.harsh.finance_project.order.model.OrderStatus;
import com.harsh.finance_project.order.repository.OrderRepository;
import com.harsh.finance_project.trade.repository.TradeRepository;
import com.harsh.finance_project.user.model.User;
import com.harsh.finance_project.user.model.UserStatus;
import com.harsh.finance_project.user.repository.UserRepository;
import com.harsh.finance_project.wallet.model.Wallet;
import com.harsh.finance_project.wallet.repository.WalletRepository;
import com.harsh.finance_project.wallet.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("api-test")
class TradeXApiIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @BeforeEach
    void cleanDatabase() {
        tradeRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        walletTransactionRepository.deleteAllInBatch();
        walletRepository.deleteAllInBatch();
        holdingRepository.deleteAllInBatch();
        assetRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void userApiCreatesUserAndRejectsInvalidOrDuplicateEmail() throws Exception {
        long userId = createUser("Asha Rao", "asha@example.com");

        mockMvc.perform(post("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Asha Rao","email":"asha@example.com","password":"secret"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));

        mockMvc.perform(post("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bad Email","email":"not-an-email","password":"secret"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getEmail()).isEqualTo("asha@example.com");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void assetApiCreatesReadsUpdatesAndDeletesAssets() throws Exception {
        long assetId = createAsset("Apple", "AAPL", "share", "STOCK", "200.00");

        mockMvc.perform(get("/api/assets").param("id", String.valueOf(assetId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(assetId))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.last").value(true));

        mockMvc.perform(put("/api/assets")
                        .param("id", String.valueOf(assetId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACTIVE","currentPrice":210.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentPrice").value(210.0));

        Asset asset = assetRepository.findById(assetId).orElseThrow();
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.ACTIVE);
        assertThat(asset.getCurrentPrice()).isEqualByComparingTo("210.00");

        mockMvc.perform(get("/api/assets").param("id", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Asset not found with id: 999999"))
                .andExpect(jsonPath("$.path").value("/api/assets"))
                .andExpect(jsonPath("$.timestamp").exists());

        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"symbol":"MSFT","unit":"share","assetType":"STOCK","currentPrice":300.00}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/assets").param("id", String.valueOf(assetId)))
                .andExpect(status().isOk());

        assertThat(assetRepository.existsById(assetId)).isFalse();
    }

    @Test
    void holdingApiCreatesReadsUpdatesAndDeletesHoldings() throws Exception {
        long userId = createUser("Holding User", "holding@example.com");
        long assetId = createAsset("Tesla", "TSLA", "share", "STOCK", "250.00");

        long holdingId = createHolding(userId, assetId, "5.00");

        mockMvc.perform(get("/api/holding").param("id", String.valueOf(holdingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.assetId").value(assetId))
                .andExpect(jsonPath("$.quantity").value(5.0));

        mockMvc.perform(get("/api/holdings/" + holdingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(holdingId));

        mockMvc.perform(get("/api/holding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(holdingId))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/holdings/user/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(holdingId))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(put("/api/holding")
                        .param("id", String.valueOf(holdingId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":8.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(8.0));

        Holding holding = holdingRepository.findById(holdingId).orElseThrow();
        assertThat(holding.getQuantity()).isEqualByComparingTo("8.00");

        mockMvc.perform(post("/api/holding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":999999,"assetId":%d,"quantity":1.00}
                                """.formatted(assetId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 999999"));

        mockMvc.perform(get("/api/holdings/999999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/holdings/" + holdingId))
                .andExpect(status().isOk());

        assertThat(holdingRepository.existsById(holdingId)).isFalse();
    }

    @Test
    void walletApiMovesFundsAndCreatesLedgerRows() throws Exception {
        long userId = createUser("Wallet User", "wallet@example.com");
        createWallet(userId);

        mockMvc.perform(get("/api/wallet/user/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.reservedBalance").value(0.0));

        postWalletAmount(userId, "deposit", "100.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.0))
                .andExpect(jsonPath("$.availableBalance").value(100.0));

        postWalletAmount(userId, "reserve", "40.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.0))
                .andExpect(jsonPath("$.reservedBalance").value(40.0))
                .andExpect(jsonPath("$.availableBalance").value(60.0));

        postWalletAmount(userId, "withdraw", "70.00")
                .andExpect(status().isConflict());

        postWalletAmount(userId, "release", "15.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedBalance").value(25.0));

        postWalletAmount(userId, "capture", "25.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(75.0))
                .andExpect(jsonPath("$.reservedBalance").value(0.0));

        postWalletAmount(userId, "withdraw", "20.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(55.0));

        postWalletAmount(userId, "deposit", "0")
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/wallet/user/" + userId + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(5));

        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo("55.00");
        assertThat(wallet.getReservedBalance()).isEqualByComparingTo("0.00");
        assertThat(walletTransactionRepository.findAll()).hasSize(5);
    }

    @Test
    void portfolioApiCalculatesCashHoldingValuesAndProfitLoss() throws Exception {
        long userId = createUser("Portfolio User", "portfolio@example.com");
        createWallet(userId);
        postWalletAmount(userId, "deposit", "1000.00").andExpect(status().isOk());
        long assetId = createAsset("Apple", "AAPL", "share", "STOCK", "100.00");
        createHolding(userId, assetId, "3.00");

        mockMvc.perform(put("/api/assets")
                        .param("id", String.valueOf(assetId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPrice":120.00}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/portfolio/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.cash").value(1000.0))
                .andExpect(jsonPath("$.totalInvestedValue").value(300.0))
                .andExpect(jsonPath("$.totalCurrentValue").value(360.0))
                .andExpect(jsonPath("$.totalProfitLoss").value(60.0));

        mockMvc.perform(get("/api/portfolio/" + userId + "/holdings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].assetId").value(assetId))
                .andExpect(jsonPath("$.content[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$.content[0].name").value("Apple"))
                .andExpect(jsonPath("$.content[0].quantity").value(3.0))
                .andExpect(jsonPath("$.content[0].averageBuyPrice").value(100.0))
                .andExpect(jsonPath("$.content[0].currentPrice").value(120.0))
                .andExpect(jsonPath("$.content[0].investedValue").value(300.0))
                .andExpect(jsonPath("$.content[0].currentValue").value(360.0))
                .andExpect(jsonPath("$.content[0].profitLoss").value(60.0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void portfolioApiHandlesUsersWithNoHoldings() throws Exception {
        long userId = createUser("Empty Portfolio User", "empty-portfolio@example.com");
        createWallet(userId);

        mockMvc.perform(get("/api/portfolio/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cash").value(0.0))
                .andExpect(jsonPath("$.totalInvestedValue").value(0))
                .andExpect(jsonPath("$.totalCurrentValue").value(0))
                .andExpect(jsonPath("$.totalProfitLoss").value(0));

        mockMvc.perform(get("/api/portfolio/" + userId + "/holdings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void portfolioApiReturnsNotFoundWhenUserOrWalletIsMissing() throws Exception {
        mockMvc.perform(get("/api/portfolio/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with id: 999999"))
                .andExpect(jsonPath("$.path").value("/api/portfolio/999999"))
                .andExpect(jsonPath("$.timestamp").exists());

        long userId = createUser("No Wallet User", "no-wallet@example.com");

        mockMvc.perform(get("/api/portfolio/" + userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void marketBuyOrderExecutesAndCreatesTradeHoldingAndWalletTransactions() throws Exception {
        long userId = createUser("Buyer", "buyer@example.com");
        createWallet(userId);
        postWalletAmount(userId, "deposit", "1000.00").andExpect(status().isOk());
        long assetId = createActiveAsset("Apple", "AAPL", "share", "STOCK", "200.00");

        long orderId = createOrder(userId, assetId, "BUY", "MARKET", "2.00", null);

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.reservedAmount").value(0.0));

        mockMvc.perform(get("/api/trades/order/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].totalAmount").value(400.0));

        Holding holding = holdingRepository.findByUserIdAndAssetId(userId, assetId).orElseThrow();
        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        assertThat(holding.getQuantity()).isEqualByComparingTo("2.00");
        assertThat(wallet.getBalance()).isEqualByComparingTo("600.00");
        assertThat(wallet.getReservedBalance()).isEqualByComparingTo("0.00");
        assertThat(tradeRepository.findAll()).hasSize(1);
        assertThat(walletTransactionRepository.findAll()).hasSize(3);

        mockMvc.perform(post("/api/orders/" + orderId + "/execute"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Only pending orders can be executed"))
                .andExpect(jsonPath("$.path").value("/api/orders/" + orderId + "/execute"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void marketSellOrderExecutesAndCreditsWallet() throws Exception {
        long userId = createUser("Seller", "seller@example.com");
        createWallet(userId);
        long assetId = createActiveAsset("Reliance", "RELIANCE", "share", "STOCK", "200.00");
        createHolding(userId, assetId, "5.00");

        long orderId = createOrder(userId, assetId, "SELL", "MARKET", "2.00", null);

        mockMvc.perform(get("/api/trade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(orderId))
                .andExpect(jsonPath("$.content[0].orderSide").value("SELL"));

        mockMvc.perform(get("/api/trades/user/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        Holding holding = holdingRepository.findByUserIdAndAssetId(userId, assetId).orElseThrow();
        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        assertThat(holding.getQuantity()).isEqualByComparingTo("3.00");
        assertThat(holding.getReservedQuantity()).isEqualByComparingTo("0.00");
        assertThat(wallet.getBalance()).isEqualByComparingTo("400.00");
        assertThat(walletTransactionRepository.findAll()).hasSize(1);
    }

    @Test
    void pendingOrdersReserveResourcesCanBeCancelledOrExecuted() throws Exception {
        long userId = createUser("Limit User", "limit@example.com");
        createWallet(userId);
        postWalletAmount(userId, "deposit", "1000.00").andExpect(status().isOk());
        long assetId = createActiveAsset("Infosys", "INFY", "share", "STOCK", "100.00");

        long buyOrderId = createOrder(userId, assetId, "BUY", "LIMIT", "2.00", "150.00");
        Wallet walletAfterPendingBuy = walletRepository.findByUserId(userId).orElseThrow();
        assertThat(walletAfterPendingBuy.getReservedBalance()).isEqualByComparingTo("300.00");
        assertThat(orderRepository.findById(buyOrderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING);

        mockMvc.perform(post("/api/orders/" + buyOrderId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(walletRepository.findByUserId(userId).orElseThrow().getReservedBalance()).isEqualByComparingTo("0.00");

        long executableBuyOrderId = createOrder(userId, assetId, "BUY", "LIMIT", "1.00", "120.00");
        mockMvc.perform(post("/api/orders/" + executableBuyOrderId + "/execute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        long sellOrderId = createOrder(userId, assetId, "SELL", "LIMIT", "1.00", "130.00");
        Holding pendingSellHolding = holdingRepository.findByUserIdAndAssetId(userId, assetId).orElseThrow();
        assertThat(pendingSellHolding.getReservedQuantity()).isEqualByComparingTo("1.00");

        mockMvc.perform(post("/api/orders/" + sellOrderId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(holdingRepository.findByUserIdAndAssetId(userId, assetId).orElseThrow().getReservedQuantity()).isEqualByComparingTo("0.00");
    }

    @Test
    void orderApiRejectsInvalidOrdersAndInsufficientResources() throws Exception {
        long userId = createUser("Invalid Order User", "invalid-order@example.com");
        createWallet(userId);
        long inactiveAssetId = createAsset("Inactive", "INACT", "share", "STOCK", "50.00");
        long activeAssetId = createActiveAsset("Active", "ACTV", "share", "STOCK", "100.00");

        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(userId, activeAssetId, "BUY", "MARKET", "0", null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(userId, inactiveAssetId, "BUY", "MARKET", "1.00", null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(userId, activeAssetId, "BUY", "LIMIT", "1.00", null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(userId, activeAssetId, "BUY", "MARKET", "1.00", null)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(userId, activeAssetId, "SELL", "MARKET", "1.00", null)))
                .andExpect(status().isConflict());

        User inactiveUser = userRepository.findById(userId).orElseThrow();
        inactiveUser.setStatus(UserStatus.INACTIVE);
        userRepository.save(inactiveUser);

        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(userId, activeAssetId, "BUY", "MARKET", "1.00", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void apiErrorsUseCommonResponseShapeForRepresentativeFailures() throws Exception {
        expectApiError(mockMvc.perform(get("/api/portfolio/999999")),
                404, "Not Found", "User not found with id: 999999", "/api/portfolio/999999");

        expectApiError(mockMvc.perform(get("/api/assets").param("id", "999999")),
                404, "Not Found", "Asset not found with id: 999999", "/api/assets");

        expectApiError(mockMvc.perform(get("/api/orders/999999")),
                404, "Not Found", "Order not found with id: 999999", "/api/orders/999999");

        long userId = createUser("Failure User", "failure@example.com");
        createWallet(userId);
        long activeAssetId = createActiveAsset("Failure Asset", "FAIL", "share", "STOCK", "100.00");

        expectApiError(postWalletAmount(userId, "withdraw", "1.00"),
                409, "Conflict", "Withdrawal amount exceeds available funds", "/api/wallet/user/" + userId + "/withdraw");

        expectApiError(mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(userId, activeAssetId, "SELL", "MARKET", "1.00", null))),
                409, "Conflict", "Holding not found for sell order", "/api/order");

        expectApiError(mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(userId, activeAssetId, "BUY", "MARKET", "0", null))),
                400, "Bad Request", "quantity must be greater than 0.0", "/api/order");
    }

    private long createUser(String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s","password":"secret"}
                                """.formatted(name, email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andReturn();

        return readId(result);
    }

    private long createAsset(String name, String symbol, String unit, String assetType, String currentPrice) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","symbol":"%s","unit":"%s","assetType":"%s","currentPrice":%s}
                                """.formatted(name, symbol, unit, assetType, currentPrice)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value(symbol))
                .andReturn();

        return readId(result);
    }

    private long createActiveAsset(String name, String symbol, String unit, String assetType, String currentPrice) throws Exception {
        long assetId = createAsset(name, symbol, unit, assetType, currentPrice);
        mockMvc.perform(put("/api/assets")
                        .param("id", String.valueOf(assetId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACTIVE"}
                                """))
                .andExpect(status().isOk());
        return assetId;
    }

    private long createHolding(long userId, long assetId, String quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/holding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"assetId":%d,"quantity":%s}
                                """.formatted(userId, assetId, quantity)))
                .andExpect(status().isCreated())
                .andReturn();

        return readId(result);
    }

    private long createWallet(long userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d}
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andReturn();

        return readId(result);
    }

    private ResultActions postWalletAmount(long userId, String operation, String amount) throws Exception {
        return mockMvc.perform(post("/api/wallet/user/" + userId + "/" + operation)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":%s,"reason":"api test"}
                        """.formatted(amount)));
    }

    private long createOrder(long userId, long assetId, String side, String category, String quantity, String requestedPrice) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(userId, assetId, side, category, quantity, requestedPrice)))
                .andExpect(status().isCreated())
                .andReturn();

        return readId(result);
    }

    private String orderJson(long userId, long assetId, String side, String category, String quantity, String requestedPrice) {
        String priceField = requestedPrice == null ? "" : "," + "\"requestedPrice\":" + requestedPrice;
        return """
                {"userId":%d,"assetId":%d,"orderSide":"%s","orderCategory":"%s","quantity":%s%s}
                """.formatted(userId, assetId, side, category, quantity, priceField);
    }

    private long readId(MvcResult result) throws Exception {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(result.getResponse().getContentAsString());

        if (!matcher.find()) {
            throw new IllegalStateException("Response did not contain an id: " + result.getResponse().getContentAsString());
        }

        return Long.parseLong(matcher.group(1));
    }

    private void expectApiError(ResultActions action, int status, String error, String message, String path) throws Exception {
        action.andExpect(status().is(status))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value(path));
    }
}
