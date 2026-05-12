package com.tenantcore.logiflowservice.security;

import com.tenantcore.common.security.SecurityConstants;
import com.tenantcore.logiflowservice.api.customer.dto.CustomerResponse;
import com.tenantcore.logiflowservice.api.driver.dto.DriverResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ReconciliationResponse;
import com.tenantcore.logiflowservice.api.vehicle.dto.VehicleResponse;
import com.tenantcore.logiflowservice.application.customer.CustomerFacade;
import com.tenantcore.logiflowservice.application.driver.DriverFacade;
import com.tenantcore.logiflowservice.application.reconciliation.ReconciliationFacade;
import com.tenantcore.logiflowservice.application.vehicle.VehicleFacade;
import com.tenantcore.logiflowservice.auth.JwtService;
import com.tenantcore.logiflowservice.config.JwtAuthenticationFilter;
import com.tenantcore.logiflowservice.config.SecurityConfig;
import com.tenantcore.logiflowservice.web.customer.LogiflowCustomerControllerImpl;
import com.tenantcore.logiflowservice.web.driver.LogiflowDriverControllerImpl;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        LogiflowCustomerControllerImpl.class,
        LogiflowDriverControllerImpl.class,
        LogiflowVehicleControllerImpl.class,
        LogiflowReconciliationControllerImpl.class
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthorizationWriteIntegrationTest {

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
    private JwtService jwtService;

    @Test
    void customerCreateUpdateDelete_requireCorrespondingPermissions() throws Exception {
        when(jwtService.parseToken("cw0")).thenReturn(claims("demo", List.of()));
        mockMvc.perform(post("/api/logiflow/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer cw0")
                        .header(SecurityConstants.TENANT_HEADER, "demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerCode\":\"C01\",\"customerName\":\"Name\"}"))
                .andExpect(status().isForbidden());

        UUID id = UUID.randomUUID();
        when(jwtService.parseToken("cw1")).thenReturn(claims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_CUSTOMER_CREATE)));
        when(customerFacade.createCustomer(eq("demo"), any())).thenReturn(
                new CustomerResponse(id, "demo", "C01", "Name", null, null, null, "NORMAL", "ACTIVE", LocalDateTime.now())
        );
        mockMvc.perform(post("/api/logiflow/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer cw1")
                        .header(SecurityConstants.TENANT_HEADER, "demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerCode\":\"C01\",\"customerName\":\"Name\"}"))
                .andExpect(status().isOk());
        verify(customerFacade).createCustomer(eq("demo"), any());

        when(jwtService.parseToken("cw2")).thenReturn(claims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_CUSTOMER_UPDATE)));
        when(customerFacade.updateCustomer(eq("demo"), eq(id), any())).thenReturn(
                new CustomerResponse(id, "demo", "C01", "Name2", null, null, null, "NORMAL", "ACTIVE", LocalDateTime.now())
        );
        mockMvc.perform(put("/api/logiflow/customers/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer cw2")
                        .header(SecurityConstants.TENANT_HEADER, "demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Name2\"}"))
                .andExpect(status().isOk());
        verify(customerFacade).updateCustomer(eq("demo"), eq(id), any());

        when(jwtService.parseToken("cw3")).thenReturn(claims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_CUSTOMER_DELETE)));
        mockMvc.perform(delete("/api/logiflow/customers/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer cw3")
                        .header(SecurityConstants.TENANT_HEADER, "demo"))
                .andExpect(status().isOk());
        verify(customerFacade).deleteCustomer(eq("demo"), eq(id));
    }

    @Test
    void driverCreateUpdateDelete_requireCorrespondingPermissions() throws Exception {
        UUID id = UUID.randomUUID();
        when(jwtService.parseToken("dw1")).thenReturn(claims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_DRIVER_CREATE)));
        when(driverFacade.createDriver(eq("demo"), any())).thenReturn(
                new DriverResponse(id, "demo", "D01", "Driver", null, null, null, "ACTIVE", LocalDateTime.now())
        );
        mockMvc.perform(post("/api/logiflow/drivers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer dw1")
                        .header(SecurityConstants.TENANT_HEADER, "demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverCode\":\"D01\",\"fullName\":\"Driver\"}"))
                .andExpect(status().isOk());

        when(jwtService.parseToken("dw2")).thenReturn(claims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_DRIVER_UPDATE)));
        when(driverFacade.updateDriver(eq("demo"), eq(id), any())).thenReturn(
                new DriverResponse(id, "demo", "D01", "Driver2", null, null, null, "ACTIVE", LocalDateTime.now())
        );
        mockMvc.perform(put("/api/logiflow/drivers/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer dw2")
                        .header(SecurityConstants.TENANT_HEADER, "demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Driver2\"}"))
                .andExpect(status().isOk());

        when(jwtService.parseToken("dw3")).thenReturn(claims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_DRIVER_DELETE)));
        mockMvc.perform(delete("/api/logiflow/drivers/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer dw3")
                        .header(SecurityConstants.TENANT_HEADER, "demo"))
                .andExpect(status().isOk());
    }

    @Test
    void vehicleCreateUpdateDelete_requireCorrespondingPermissions() throws Exception {
        UUID id = UUID.randomUUID();
        when(jwtService.parseToken("vw1")).thenReturn(claims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_VEHICLE_CREATE)));
        when(vehicleFacade.createVehicle(eq("demo"), any())).thenReturn(
                new VehicleResponse(id, "demo", "V01", "51A-12345", "VAN", new BigDecimal("1200"), "ACTIVE", LocalDateTime.now())
        );
        mockMvc.perform(post("/api/logiflow/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer vw1")
                        .header(SecurityConstants.TENANT_HEADER, "demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleCode\":\"V01\",\"plateNumber\":\"51A-12345\"}"))
                .andExpect(status().isOk());

        when(jwtService.parseToken("vw2")).thenReturn(claims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_VEHICLE_UPDATE)));
        when(vehicleFacade.updateVehicle(eq("demo"), eq(id), any())).thenReturn(
                new VehicleResponse(id, "demo", "V01", "51A-67890", "TRUCK", new BigDecimal("1500"), "ACTIVE", LocalDateTime.now())
        );
        mockMvc.perform(put("/api/logiflow/vehicles/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer vw2")
                        .header(SecurityConstants.TENANT_HEADER, "demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plateNumber\":\"51A-67890\"}"))
                .andExpect(status().isOk());

        when(jwtService.parseToken("vw3")).thenReturn(claims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_VEHICLE_DELETE)));
        mockMvc.perform(delete("/api/logiflow/vehicles/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer vw3")
                        .header(SecurityConstants.TENANT_HEADER, "demo"))
                .andExpect(status().isOk());
    }

    @Test
    void reconciliationCreateAndUpdateStatus_requirePermissions() throws Exception {
        UUID recId = UUID.randomUUID();
        when(jwtService.parseToken("rw0")).thenReturn(claims("demo", List.of()));
        mockMvc.perform(post("/api/logiflow/reconciliations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer rw0")
                        .header(SecurityConstants.TENANT_HEADER, "demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codRecordIds\":[\"00000000-0000-0000-0000-000000000001\"]}"))
                .andExpect(status().isForbidden());

        when(jwtService.parseToken("rw1")).thenReturn(claims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_RECONCILIATION_CREATE)));
        when(reconciliationFacade.createReconciliation(eq("demo"), any())).thenReturn(
                new ReconciliationResponse(recId, "demo", "REC-1", null, 1, new BigDecimal("100000"), "OPEN", null, null, LocalDateTime.now())
        );
        mockMvc.perform(post("/api/logiflow/reconciliations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer rw1")
                        .header(SecurityConstants.TENANT_HEADER, "demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codRecordIds\":[\"00000000-0000-0000-0000-000000000001\"]}"))
                .andExpect(status().isOk());

        when(jwtService.parseToken("rw2")).thenReturn(claims("demo", List.of(SecurityConstants.PERMISSION_LOGIFLOW_RECONCILIATION_UPDATE)));
        when(reconciliationFacade.updateReconciliationStatus(eq("demo"), eq(recId), any())).thenReturn(
                new ReconciliationResponse(recId, "demo", "REC-1", null, 1, new BigDecimal("100000"), "RECONCILED", LocalDateTime.now(), null, LocalDateTime.now())
        );
        mockMvc.perform(patch("/api/logiflow/reconciliations/{id}/status", recId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer rw2")
                        .header(SecurityConstants.TENANT_HEADER, "demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RECONCILED\"}"))
                .andExpect(status().isOk());
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
