package com.harsh.finance_project;

import com.harsh.finance_project.asset.model.Asset;
import com.harsh.finance_project.asset.model.AssetStatus;
import com.harsh.finance_project.asset.model.AssetType;
import com.harsh.finance_project.asset.repository.AssetRepository;
import com.harsh.finance_project.holding.model.Holding;
import com.harsh.finance_project.holding.repository.HoldingRepository;
import com.harsh.finance_project.order.model.Order;
import com.harsh.finance_project.order.model.OrderCategory;
import com.harsh.finance_project.order.model.OrderSide;
import com.harsh.finance_project.order.model.OrderStatus;
import com.harsh.finance_project.order.repository.OrderRepository;
import com.harsh.finance_project.trade.model.Trade;
import com.harsh.finance_project.trade.repository.TradeRepository;
import com.harsh.finance_project.user.model.User;
import com.harsh.finance_project.user.model.UserRole;
import com.harsh.finance_project.user.repository.UserRepository;
import com.harsh.finance_project.wallet.model.Wallet;
import com.harsh.finance_project.wallet.model.WalletStatus;
import com.harsh.finance_project.wallet.model.WalletTransactionType;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("api-test")
class ApiAuditIntegrationTests {
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
    void walletLifecycleRejectsDuplicateAndInsufficientOperationsAndRecordsLedger() throws Exception {
        String token = registerAndLogin("Wallet Owner", "wallet-audit@example.com");
        long userId = userId("wallet-audit@example.com");

        mockMvc.perform(post("/api/wallet")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(walletJson(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.availableBalance").value(0.0));

        mockMvc.perform(post("/api/wallet")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(walletJson(userId)))
                .andExpect(status().isBadRequest());

        walletAmount(token, userId, "deposit", "100").andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.0));
        walletAmount(token, userId, "withdraw", "101").andExpect(status().isConflict());
        walletAmount(token, userId, "withdraw", "35").andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(65.0))
                .andExpect(jsonPath("$.reservedBalance").value(0.0))
                .andExpect(jsonPath("$.availableBalance").value(65.0));
        walletAmount(token, userId, "deposit", "0").andExpect(status().isBadRequest());

        walletAmount(token, userId, "reserve", "1").andExpect(status().isForbidden());
        walletAmount(token, userId, "release", "1").andExpect(status().isForbidden());
        walletAmount(token, userId, "capture", "1").andExpect(status().isForbidden());

        mockMvc.perform(get("/api/wallet/user/" + userId + "/transactions")
                        .header("Authorization", bearer(token))
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.hasNext").value(false));

        assertThat(walletRepository.findByUserId(userId).orElseThrow().getBalance())
                .isEqualByComparingTo("65");
        assertThat(walletRepository.findByUserId(userId).orElseThrow().getReservedBalance())
                .isEqualByComparingTo("0");
        assertThat(walletTransactionRepository.findAll())
                .extracting(transaction -> transaction.getTransactionType().name())
                .containsExactlyInAnyOrder("DEPOSIT", "WITHDRAWAL");
        assertThat(walletTransactionRepository.findAll())
                .allSatisfy(transaction -> assertThat(transaction.getStatus().name()).isEqualTo("COMPLETED"));
        assertLedgerEntry(WalletTransactionType.DEPOSIT, "100", "0");
        assertLedgerEntry(WalletTransactionType.WITHDRAWAL, "65", "0");
    }

    @Test
    void adminRoleDoesNotBypassOwnershipAndOnlyAdminCanUseAdminOperations() throws Exception {
        String ownerToken = registerAndLogin("Resource Owner", "owner-audit@example.com");
        long ownerId = userId("owner-audit@example.com");
        long otherUserId = createUser("Other User", "other-audit@example.com");
        String adminToken = registerAdminAndLogin("admin-audit@example.com");
        long adminId = userId("admin-audit@example.com");
        long assetId = createAsset("Audit Asset", "AUD", AssetStatus.ACTIVE);

        createWallet(ownerId);
        long holdingId = createHolding(ownerId, assetId);
        long orderId = createPendingOrder(ownerId, assetId);
        createTrade(orderId);

        mockMvc.perform(get("/api/wallet/user/" + ownerId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/wallet/user/" + ownerId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/holdings/" + holdingId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/trades/order/" + orderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/portfolio/" + ownerId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/wallet/user/" + otherUserId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/holdings/user/" + otherUserId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders/user/" + otherUserId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/trades/user/" + otherUserId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/portfolio/" + otherUserId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/assets")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assetJson("User Asset", "USR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/assets")
                        .header("Authorization", bearer(ownerToken))
                        .param("id", String.valueOf(assetId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPrice\":12}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/assets")
                        .header("Authorization", bearer(ownerToken))
                        .param("id", String.valueOf(assetId)))
                .andExpect(status().isForbidden());

        MvcResult adminAsset = mockMvc.perform(post("/api/assets")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assetJson("Admin Asset", "ADM")))
                .andExpect(status().isCreated())
                .andReturn();
        mockMvc.perform(put("/api/assets")
                        .header("Authorization", bearer(adminToken))
                        .param("id", String.valueOf(assetId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPrice\":12}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/assets")
                        .header("Authorization", bearer(adminToken))
                        .param("id", String.valueOf(readId(adminAsset))))
                .andExpect(status().isOk())
                .andExpect(content().string("Asset Deleted Successfully"));
        mockMvc.perform(get("/api/order")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/holding")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/trade")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/order")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/holding")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/trade")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/wallet/user/" + adminId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void failedMarketSellRollsBackOrderTradeHoldingWalletAndLedgerChanges() throws Exception {
        String token = registerAndLogin("Rollback User", "rollback-audit@example.com");
        long userId = userId("rollback-audit@example.com");
        long assetId = createAsset("Rollback Asset", "RBK", AssetStatus.ACTIVE);
        createWallet(userId);
        long holdingId = createHolding(userId, assetId);

        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        wallet.setStatus(WalletStatus.SUSPENDED);
        walletRepository.save(wallet);

        mockMvc.perform(post("/api/order")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(userId, assetId, "SELL", "MARKET", "1", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Wallet is not active"));

        assertThat(orderRepository.count()).isZero();
        assertThat(tradeRepository.count()).isZero();
        assertThat(walletTransactionRepository.count()).isZero();
        assertThat(holdingRepository.findById(holdingId).orElseThrow().getQuantity())
                .isEqualByComparingTo("5");
        assertThat(holdingRepository.findById(holdingId).orElseThrow().getReservedQuantity())
                .isEqualByComparingTo("0");
        assertThat(walletRepository.findByUserId(userId).orElseThrow().getBalance())
                .isEqualByComparingTo("0");
    }

    @Test
    void paginationMetadataSizeBoundsAndEmptyResultsAreConsistentAcrossEndpoints() throws Exception {
        String adminToken = registerAdminAndLogin("page-admin@example.com");
        long adminId = userId("page-admin@example.com");
        createWallet(adminId);
        createAsset("Page Asset One", "PG1", AssetStatus.ACTIVE);
        createAsset("Page Asset Two", "PG2", AssetStatus.ACTIVE);

        mockMvc.perform(get("/api/assets")
                        .header("Authorization", bearer(adminToken))
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.last").value(true));

        mockMvc.perform(get("/api/assets")
                        .header("Authorization", bearer(adminToken))
                        .param("page", "0")
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(100))
                .andExpect(jsonPath("$.content.length()").value(2));

        assertEmptyPage("/api/holding", adminToken);
        assertEmptyPage("/api/holdings/user/" + adminId, adminToken);
        assertEmptyPage("/api/order", adminToken);
        assertEmptyPage("/api/orders/user/" + adminId, adminToken);
        assertEmptyPage("/api/trade", adminToken);
        assertEmptyPage("/api/trades/user/" + adminId, adminToken);
        assertEmptyPage("/api/wallet/user/" + adminId + "/transactions", adminToken);
        assertEmptyPage("/api/portfolio/" + adminId + "/holdings", adminToken);
    }

    @Test
    void limitOrderExecutionAndExecutedOrderCancellationRulesAreEnforced() throws Exception {
        String token = registerAndLogin("Limit Audit User", "limit-audit@example.com");
        long userId = userId("limit-audit@example.com");
        long assetId = createAsset("Limit Asset", "LMT", AssetStatus.ACTIVE);
        createWallet(userId);
        walletAmount(token, userId, "deposit", "1000").andExpect(status().isOk());

        long pendingOrderId = createPendingBuy(token, userId, assetId, "2", "120");
        mockMvc.perform(get("/api/orders/" + pendingOrderId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reservedAmount").value(240.0));
        mockMvc.perform(get("/api/order")
                        .header("Authorization", bearer(token))
                        .param("id", String.valueOf(pendingOrderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pendingOrderId));

        mockMvc.perform(post("/api/orders/" + pendingOrderId + "/execute")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));
        long tradeId = tradeRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/trade")
                        .header("Authorization", bearer(token))
                        .param("id", String.valueOf(tradeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tradeId));
        mockMvc.perform(get("/api/trades/" + tradeId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(pendingOrderId));
        mockMvc.perform(post("/api/orders/" + pendingOrderId + "/cancel")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only pending orders can be cancelled"));

        assertThat(tradeRepository.count()).isEqualTo(1);
        assertThat(walletTransactionRepository.findAll())
                .extracting(transaction -> transaction.getTransactionType().name())
                .containsExactlyInAnyOrder("DEPOSIT", "RESERVE", "CAPTURE_RESERVED");
        assertThat(holdingRepository.findByUserIdAndAssetId(userId, assetId).orElseThrow().getQuantity())
                .isEqualByComparingTo("2");
        assertThat(walletRepository.findByUserId(userId).orElseThrow().getBalance())
                .isEqualByComparingTo("760");
    }

    @Test
    void publicHoldingMutationsAreUnavailable() throws Exception {
        String token = registerAndLogin("Holding Boundary User", "holding-boundary@example.com");
        long userId = userId("holding-boundary@example.com");
        long assetId = createAsset("Boundary Asset", "BND", AssetStatus.ACTIVE);
        createWallet(userId);
        long holdingId = createHolding(userId, assetId);

        mockMvc.perform(post("/api/holding")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"assetId":%d,"quantity":1}
                                """.formatted(userId, assetId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/holding")
                        .header("Authorization", bearer(token))
                        .param("id", String.valueOf(holdingId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":10}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/holdings/" + holdingId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());

        assertThat(holdingRepository.findById(holdingId).orElseThrow().getQuantity())
                .isEqualByComparingTo("5");
    }

    private void assertEmptyPage(String path, String token) throws Exception {
        mockMvc.perform(get(path)
                        .header("Authorization", bearer(token))
                        .param("page", "0")
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(100))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.last").value(true));
    }

    private void assertLedgerEntry(WalletTransactionType type, String balance, String reservedBalance) {
        var transaction = walletTransactionRepository.findAll().stream()
                .filter(row -> row.getTransactionType() == type)
                .findFirst()
                .orElseThrow();
        assertThat(transaction.getBalanceAfter()).isEqualByComparingTo(balance);
        assertThat(transaction.getReservedBalanceAfter()).isEqualByComparingTo(reservedBalance);
        assertThat(transaction.getReason()).isEqualTo("audit");
    }

    private String registerAndLogin(String name, String email) throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s","password":"secret123"}
                                """.formatted(name, email)))
                .andExpect(status().isCreated());
        return login(email);
    }

    private String registerAdminAndLogin(String email) throws Exception {
        registerAndLogin("Audit Admin", email);
        User admin = userRepository.findByEmail(email).orElseThrow();
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);
        return login(email);
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secret123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        Matcher matcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new IllegalStateException("Login response did not contain a token");
        }
        return matcher.group(1);
    }

    private long createUser(String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s","password":"secret123"}
                                """.formatted(name, email)))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    private long userId(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    private long createAsset(String name, String symbol, AssetStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return assetRepository.save(new Asset(name, symbol, "share", status, AssetType.STOCK,
                new BigDecimal("100"), now, now)).getId();
    }

    private void createWallet(long userId) throws Exception {
        String token = login(userRepository.findById(userId).orElseThrow().getEmail());
        mockMvc.perform(post("/api/wallet")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(walletJson(userId)))
                .andExpect(status().isCreated());
    }

    private long createHolding(long userId, long assetId) {
        User user = userRepository.findById(userId).orElseThrow();
        Asset asset = assetRepository.findById(assetId).orElseThrow();
        return holdingRepository.save(new Holding(user, asset, new BigDecimal("5"),
                asset.getCurrentPrice())).getId();
    }

    private long createPendingOrder(long userId, long assetId) {
        LocalDateTime now = LocalDateTime.now();
        return orderRepository.save(new Order(userRepository.findById(userId).orElseThrow(),
                assetRepository.findById(assetId).orElseThrow(), OrderSide.BUY, OrderCategory.LIMIT,
                BigDecimal.ONE, new BigDecimal("100"), now)).getId();
    }

    private void createTrade(long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        tradeRepository.save(new Trade(order, new BigDecimal("100"), LocalDateTime.now()));
    }

    private long createPendingBuy(String token, long userId, long assetId, String quantity, String price)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/order")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(userId, assetId, "BUY", "LIMIT", quantity, price)))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    private org.springframework.test.web.servlet.ResultActions walletAmount(
            String token, long userId, String operation, String amount) throws Exception {
        return mockMvc.perform(post("/api/wallet/user/" + userId + "/" + operation)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":%s,"reason":"audit"}
                        """.formatted(amount)));
    }

    private String walletJson(long userId) {
        return "{\"userId\":" + userId + "}";
    }

    private String assetJson(String name, String symbol) {
        return """
                {"name":"%s","symbol":"%s","unit":"share","assetType":"STOCK","currentPrice":100}
                """.formatted(name, symbol);
    }

    private String orderJson(long userId, long assetId, String side, String category,
                             String quantity, String requestedPrice) {
        String price = requestedPrice == null ? "" : ",\"requestedPrice\":" + requestedPrice;
        return """
                {"userId":%d,"assetId":%d,"orderSide":"%s","orderCategory":"%s","quantity":%s%s}
                """.formatted(userId, assetId, side, category, quantity, price);
    }

    private long readId(MvcResult result) throws Exception {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)")
                .matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new IllegalStateException("Response did not contain an id");
        }
        return Long.parseLong(matcher.group(1));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
