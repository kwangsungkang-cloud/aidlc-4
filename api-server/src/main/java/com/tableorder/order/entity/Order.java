package com.tableorder.order.entity;

import com.tableorder.table.entity.TableSession;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Order {

    public enum Status {
        PENDING, PREPARING, COMPLETED;

        public boolean canTransitionTo(Status target) {
            return switch (this) {
                case PENDING -> target == PREPARING;
                case PREPARING -> target == COMPLETED;
                case COMPLETED -> false;
            };
        }

        public String getTransitionErrorMessage(Status target) {
            if (this == COMPLETED) return "완료된 주문은 변경할 수 없습니다";
            if (this == PENDING && target == COMPLETED) return "준비중 단계를 거쳐야 합니다";
            if (this.ordinal() > target.ordinal()) return "이전 상태로 되돌릴 수 없습니다";
            return "허용되지 않는 상태 전이입니다";
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TableSession session;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void changeStatus(Status newStatus) {
        this.status = newStatus;
    }

    public void addItem(OrderItem item) {
        this.items.add(item);
        item.setOrder(this);
    }
}
