package com.tableorder.menu.entity;

import com.tableorder.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category", indexes = {
    @Index(name = "idx_store_order", columnList = "store_id, display_order")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
