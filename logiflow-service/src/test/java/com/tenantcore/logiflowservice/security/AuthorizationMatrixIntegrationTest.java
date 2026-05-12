package com.tenantcore.logiflowservice.security;

import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.common.security.SecurityConstants;
import com.tenantcore.logiflowservice.api.customer.dto.CustomerResponse;
import com.tenantcore.logiflowservice.api.driver.dto.DriverResponse;
import com.tenantcore.logiflowservice.api.operations.dto.CodSummaryResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ReconciliationResponse;
import com.tenantcore.logiflowservice.api.vehicle.dto.VehicleResponse;
import com.tenantcore.logiflowservice.application.customer.CustomerFacade;
import com.tenantcore.logiflowservice.application.driver.DriverFacade;
import com.tenantcore.logiflowservice.application.operations.OperationsFacade;
import com.tenantcore.logiflowservice.application.reconciliation.ReconciliationFacade;
import com.tenantcore.logiflowservice.application.vehicle.VehicleFacade;
import com.tenantcore.logiflowservice.auth.JwtService;
import com.tenantcore.logiflowservice.config.JwtAuthenticationFilter;
import com.tenantcore.logiflowservice.config.SecurityConfig;
import com.tenantcore.logiflowservice.web.customer.LogiflowCustomerControllerImpl;
import com.tenantcore.logiflowservice.web.driver.LogiflowDriverControllerImpl;
import com.tenantcore.logiflowservice.web.operations.LogiflowOperationsControllerImpl;
import com.tenantcore.logiflowservice.web.reconciliation.LogiflowReconciliationControllerImpl;
import com.tenantcore.logiflowservice.web.vehicle.LogiflowVehicleControllerImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        LogiflowCustomerControllerImpl.class,
        LogiflowDriverControllerImpl.class,
        LogiflowVehicleControllerImpl.class,
        LogiflowReconciliationControllerImpl.class,
        LogiflowOperationsControllerImpl.class
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthorizationMatrixIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerFacade customerFacade;

    @MockBean
    private DriverFacade driverFacade;

    @MockBean
    private VehicleFacade vehicleFacade;

    @MockBean
    private ReconciliationFacade reconciliationFacade;

    @MockBean
    private OperationsFacade operationsFacade;

    @MockBean
    private JwtService jwtService;

    @Test
    void customerList_requiresCustomerViewPermission() throws Exception {
        when(jwtService.parseToken("t1")).thenReturn(claims("demo", List.of()));
        mockMvc.perform(get("/api/logiflow/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer t1")
                        .header(SecurityConstants.TENANT_HEADER, "demo"))
                .andExpect(status().isForbidden());

        when(jwtService.parseToken("t2")).thenReturn(claims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_CUSTOMER_VIEW)));
        when(customerFacade.listCustomers(eq("demo"), any())).thenReturn(PageResponse.of(List.<CustomerResponse>of(), 0, 20, 0));
        mockMvc.perform(get("/api/logiflow/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer t2")
                        .header(SecurityConstants.TENANT_HEADER, "demo"))
                .andExpect(status().isOk());
        verify(customerFacade).listCustomers(eq("demo"), any());
    }

    @Test
    void driverList_requiresDriverViewPermission() throws Exception {
        when(jwtService.parseToken("t3")).thenReturn(claims("demo", List.of()));
        mockMvc.perform(get("/api/logiflow/drivers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer t3")
                        .header(SecurityConstants.TENANT_HEADER, "demo"))
                .andExpect(status().isForbidden());

        when(jwtService.parseToken("t4")).thenReturn(claims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_DRIVER_VIEW)));
        when(driverFacade.listDrivers(eq("demo"), any())).thenReturn(PageResponse.of(List.<DriverResponse>of(), 0, 20, 0));
        mockMvc.perform(get("/api/logiflow/drivers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer t4")
                        .header(SecurityConstants.TENANT_HEADER, "demo"))
                .andExpect(status().isOk());
        verify(driverFacade).listDrivers(eq("demo"), any());
    }

    @Test
    void vehicleList_requiresVehicleViewPermission() throws Exception {
        when(jwtService.parseToken("t5")).thenReturn(claims("demo", List.of()));
        mockMvc.perform(get("/api/logiflow/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer t5")
                        .header(SecurityConstants.TENANT_HEADER, "demo"))
                .andExpect(status().isForbidden());

        when(jwtService.parseToken("t6")).thenReturn(claims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_VEHICLE_VIEW)));
        when(vehicleFacade.listVehicles(eq("demo"), any())).thenReturn(PageResponse.of(List.<VehicleResponse>of(), 0, 20, 0));
        mockMvc.perform(get("/api/logiflow/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer t6")
                        .header(SecurityConstants.TENANT_HEADER, "demo"))
                .andExpect(status().isOk());
        verify(vehicleFacade).listVehicles(eq("demo"), any());
    }

    @Test
    void reconciliationAndCodSummary_requireRespectivePermissions() throws Exception {
        when(jwtService.parseToken("t7")).thenReturn(claims("demo", List.of()));
        mockMvc.perform(get("/api/logiflow/reconciliations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer t7")
                        .header(SecurityConstants.TENANT_HEADER, "demo"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/logiflow/operations/cod/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer t7")
                        .header(SecurityConstants.TENANT_HEADER, "demo"))
                .andExpect(status().isForbidden());

        when(jwtService.parseToken("t8")).thenReturn(claims("demo", List.of(
                SecurityConstants.PERMISSION_LOGIFLOW_RECONCILIATION_VIEW,
                SecurityConstants.PERMISSION_LOGIFLOW_COD_VIEW
        )));
        when(reconciliationFacade.listReconciliations(eq("demo"), any())).thenReturn(PageResponse.of(List.<ReconciliationResponse>of(), 0, 20, 0));
        when(operationsFacade.getCodSummary("demo")).thenReturn(
                new CodSummaryResponse(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );

        mockMvc.perform(get("/api/logiflow/reconciliations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer t8")
                        .header(SecurityConstants.TENANT_HEADER, "demo"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/logiflow/operations/cod/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer t8")
                        .header(SecurityConstants.TENANT_HEADER, "demo"))
                .andExpect(status().isOk());
    }

    @Test
    void tenantMismatch_returnsForbidden() throws Exception {
        when(jwtService.parseToken("t9")).thenReturn(claims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_CUSTOMER_VIEW)));
        mockMvc.perform(get("/api/logiflow/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer t9")
                        .header(SecurityConstants.TENANT_HEADER, "other"))
                .andExpect(status().isForbidden());
    }

    private Claims claims(String tenantCode, List<String> permissions) {
        return Jwts.claims()
                .add(SecurityConstants.USER_ID_CLAIM, UUID.randomUUID().toString())
                .add(SecurityConstants.TENANT_CODE_CLAIM, tenantCode)
                .add(SecurityConstants.USERNAME_CLAIM, "demo.owner")
                .add(SecurityConstants.ROLES_CLAIM, List.of("OWNER"))
                .add(SecurityConstants.PERMISSIONS_CLAIM, permissions)
                .build();
    }
}
