package com.yowyob.user.infrastructure.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "search_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String query;

    @CreationTimestamp
    private LocalDateTime searchedAt;
}
