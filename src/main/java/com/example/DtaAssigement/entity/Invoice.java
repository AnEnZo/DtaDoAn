package com.example.DtaAssigement.entity;

import com.example.DtaAssigement.ennum.PaymentMethod;
import com.example.DtaAssigement.ennum.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    private LocalDateTime paymentTime;

    @ManyToOne(optional = true)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "cashier_id")
    private User cashier;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    // Dùng để theo dõi/regen MoMo dynamic QR cho hoá đơn PENDING
    @Column(name = "momo_order_id")
    private UUID momoOrderId;

    // Track PayPal order ID for PayPal payments
    @Column(name = "paypal_order_id")
    private String paypalOrderId;

}
