package com.harsh.finance_project;

import com.harsh.finance_project.asset.repository.AssetRepository;
import com.harsh.finance_project.asset.model.Asset;
import com.harsh.finance_project.asset.model.AssetStatus;
import com.harsh.finance_project.asset.model.AssetType;
import com.harsh.finance_project.holding.repository.HoldingRepository;
import com.harsh.finance_project.holding.model.Holding;
import com.harsh.finance_project.order.repository.OrderRepository;
import com.harsh.finance_project.order.model.Order;
import com.harsh.finance_project.order.model.OrderCategory;
import com.harsh.finance_project.order.model.OrderSide;
import com.harsh.finance_project.trade.repository.TradeRepository;
import com.harsh.finance_project.trade.model.Trade;
import com.harsh.finance_project.user.repository.UserRepository;
import com.harsh.finance_project.user.model.UserRole;
import com.harsh.finance_project.wallet.repository.WalletRepository;
import com.harsh.finance_project.wallet.repository.WalletTransactionRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("api-test")
class AuthIntegrationTests {
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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

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
    void registerCreatesUserWithEncodedPasswordAndLoginReturnsJwt() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Auth User","email":"auth@example.com","password":"secret123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("auth@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        var user = userRepository.findByEmail("auth@example.com").orElseThrow();
        assertThat(user.getPassword()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", user.getPassword())).isTrue();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"auth@example.com","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Auth User","email":"auth@example.com","password":"secret123"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"auth@example.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginRejectsNonExistentUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing@example.com","password":"secret123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/assets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validJwtAuthenticatesProtectedEndpoint() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/assets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void userAndAdminRolesControlAssetChanges() throws Exception {
        registerUser("Regular User", "user@example.com");
        String userToken = login("user@example.com");

        mockMvc.perform(post("/api/assets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Apple","symbol":"AAPL","unit":"share","assetType":"STOCK","currentPrice":100}
                                """))
                .andExpect(status().isForbidden());

        registerUser("Admin User", "admin@example.com");
        var admin = userRepository.findByEmail("admin@example.com").orElseThrow();
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);
        String adminToken = login("admin@example.com");

        mockMvc.perform(post("/api/assets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Apple","symbol":"AAPL","unit":"share","assetType":"STOCK","currentPrice":100}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void usersCanAccessOnlyTheirOwnFinancialResources() throws Exception {
        registerUser("User A", "user-a@example.com");
        registerUser("User B", "user-b@example.com");
        var userA = userRepository.findByEmail("user-a@example.com").orElseThrow();
        var userB = userRepository.findByEmail("user-b@example.com").orElseThrow();
        long userAId = userA.getId();
        long userBId = userB.getId();
        String token = login("user-a@example.com");

        LocalDateTime now = LocalDateTime.now();
        walletRepository.save(new com.harsh.finance_project.wallet.model.Wallet(userA, now, now));
        walletRepository.save(new com.harsh.finance_project.wallet.model.Wallet(userB, now, now));
        Asset asset = assetRepository.save(new Asset(
                "Test Asset", "TST", "share", AssetStatus.ACTIVE, AssetType.STOCK,
                BigDecimal.TEN, now, now));
        Holding ownHolding = holdingRepository.save(new Holding(userA, asset, BigDecimal.ONE, BigDecimal.TEN));
        Holding otherHolding = holdingRepository.save(new Holding(userB, asset, BigDecimal.ONE, BigDecimal.TEN));
        Order otherOrder = orderRepository.save(new Order(
                userB, asset, OrderSide.BUY, OrderCategory.MARKET, BigDecimal.ONE, null, now));
        Trade otherTrade = tradeRepository.save(new Trade(otherOrder, BigDecimal.TEN, now));

        mockMvc.perform(get("/api/wallet/user/" + userAId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/portfolio/" + userAId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/holdings/user/" + userAId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/user/" + userAId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/trades/user/" + userAId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/holdings/" + ownHolding.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/wallet/user/" + userBId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/wallet/user/" + userBId + "/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/portfolio/" + userBId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/portfolio/" + userBId + "/holdings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/holdings/user/" + userBId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders/user/" + userBId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/trades/user/" + userBId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/holdings/" + otherHolding.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/holding")
                        .param("id", String.valueOf(otherHolding.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/holding")
                        .param("id", String.valueOf(otherHolding.getId()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":2}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/holdings/" + otherHolding.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders/" + otherOrder.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/orders/" + otherOrder.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/orders/" + otherOrder.getId() + "/execute")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/trades/" + otherTrade.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/trades/order/" + otherOrder.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/holding")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"assetId":%d,"quantity":1}
                                """.formatted(userBId, asset.getId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"assetId":%d,"orderSide":"BUY","orderCategory":"MARKET","quantity":1}
                                """.formatted(userBId, asset.getId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/wallet")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d}
                                """.formatted(userBId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void inactiveUserCannotLoginOrUseAnExistingToken() throws Exception {
        registerUser("Inactive User", "inactive@example.com");
        String token = login("inactive@example.com");

        var user = userRepository.findByEmail("inactive@example.com").orElseThrow();
        user.setStatus(com.harsh.finance_project.user.model.UserStatus.INACTIVE);
        userRepository.save(user);

        mockMvc.perform(get("/api/assets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"inactive@example.com","password":"secret123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedJwtIsRejectedCleanly() throws Exception {
        mockMvc.perform(get("/api/assets")
                        .header("Authorization", "Bearer malformed-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedBearerTokenIsRejectedCleanly() throws Exception {
        mockMvc.perform(get("/api/assets")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidSignatureJwtIsRejectedCleanly() throws Exception {
        String token = Jwts.builder()
                .subject("auth@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60))
                .signWith(Keys.hmacShaKeyFor(
                        "different-super-secret-key-for-invalid-signature"
                                .getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/assets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredJwtIsRejectedCleanly() throws Exception {
        String token = Jwts.builder()
                .subject("auth@example.com")
                .issuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(
                        jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/assets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndLogin() throws Exception {
        registerUser("Auth User", "auth@example.com");
        return login("auth@example.com");
    }

    private void registerUser(String name, String email) throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s","password":"secret123"}
                                """.formatted(name, email)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secret123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return loginResponse.substring(
                loginResponse.indexOf(":\"") + 2,
                loginResponse.lastIndexOf("\"")
        );
    }
}
