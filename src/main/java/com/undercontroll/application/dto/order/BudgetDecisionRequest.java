package com.undercontroll.application.dto.order;

import com.undercontroll.domain.enums.ApprovalDecision;
import io.swagger.v3.oas.annotations.media.Schema;

public record BudgetDecisionRequest(
        @Schema(allowableValues = {"APPROVED", "DECLINED"}) ApprovalDecision decision,
        String note
) {
}
