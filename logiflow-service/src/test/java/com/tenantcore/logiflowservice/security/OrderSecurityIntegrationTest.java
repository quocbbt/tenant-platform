package com.tenantcore.logiflowservice.security;

import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.common.security.SecurityConstants;
import com.tenantcore.logiflowservice.api.order.dto.OrderResponse;
import com.tenantcore.logiflowservice.application.order.OrderFacade;
import com.tenantcore.logiflowservice.auth.JwtService;
import com.tenantcore.logiflowservice.config.JwtAuthenticationFilter;
import com.tenantcore.logiflowservice.config.SecurityConfig;
import com.tenantcore.logiflowservice.web.order.LogiflowOrderControllerImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LogiflowOrderControllerImpl.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class OrderSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderFacade orderFacade;

    @MockBean
    private JwtService jwtService;

    @Test
    void listOrders_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/logiflow/orders")
                        .header(SecurityConstants.TENANT_HEADER, "demo"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listOrders_withTokenButWithoutPermission_returns403() throws Exception {
        when(jwtService.parseToken("token-no-view")).thenReturn(mockClaims("demo", List.of()));

        mockMvc.perform(get("/api/logiflow/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-no-view")
                        .header(SecurityConstants.TENANT_HEADER, "demo"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listOrders_withTenantHeaderMismatch_returns403() throws Exception {
        when(jwtService.parseToken("token-mismatch")).thenReturn(
                mockClaims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_ORDER_VIEW))
        );

        mockMvc.perform(get("/api/logiflow/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-mismatch")
                        .header(SecurityConstants.TENANT_HEADER, "another-tenant"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listOrders_withValidTokenPermissionAndTenant_returns200() throws Exception {
        when(jwtService.parseToken("token-ok")).thenReturn(
                mockClaims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_ORDER_VIEW))
        );
        when(orderFacade.listOrders(eq("demo"), any())).thenReturn(PageResponse.of(List.<OrderResponse>of(), 0, 20, 0));

        mockMvc.perform(get("/api/logiflow/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-ok")
                        .header(SecurityConstants.TENANT_HEADER, "demo"))
                .andExpect(status().isOk());

        verify(orderFacade).listOrders(eq("demo"), any());
    }

    private Claims mockClaims(String tenantCode, List<String> permissions) {
        return Jwts.claims()
                .add(SecurityConstants.USER_ID_CLAIM, UUID.randomUUID().toString())
                .add(SecurityConstants.TENANT_CODE_CLAIM, tenantCode)
                .add(SecurityConstants.USERNAME_CLAIM, "demo.owner")
                .add(SecurityConstants.ROLES_CLAIM, List.of("OWNER"))
                .add(SecurityConstants.PERMISSIONS_CLAIM, permissions)
                .build();
    }
}
