package com.tenantcore.logiflowservice.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "logiflow_tracking_events")
public class TrackingEventEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_code", nullable = false, length = 50)
    private String tenantCode;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "event_code", nullable = false, length = 100)
    private String eventCode;

    @Column(name = "event_name", nullable = false, length = 255)
    private String eventName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "location_text", columnDefinition = "TEXT")
    private String locationText;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "event_time")
    private LocalDateTime eventTime;
}
