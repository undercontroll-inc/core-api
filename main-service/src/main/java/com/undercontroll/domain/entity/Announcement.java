package com.undercontroll.domain.entity;

import com.undercontroll.domain.entity.enums.AnnouncementType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "announcement")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String title;

    private String content;

    @Enumerated(EnumType.STRING)
    private AnnouncementType type;

    @CreationTimestamp
    private LocalDateTime publishedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
