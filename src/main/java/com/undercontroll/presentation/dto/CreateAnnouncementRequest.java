package com.undercontroll.presentation.dto;

import com.undercontroll.domain.enums.AnnouncementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAnnouncementRequest(

        @NotBlank
        String title,

        @NotBlank
        String description,

        @NotNull
        AnnouncementType type
) {
}
