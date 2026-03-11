package com.undercontroll.domain.model;

import com.undercontroll.domain.enums.AnnouncementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Announcement {

    private Integer id;

    private String title;

    private String content;

    private AnnouncementType type;

    private LocalDateTime publishedAt;

    private LocalDateTime updatedAt;

}
