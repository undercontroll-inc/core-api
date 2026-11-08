package com.undercontroll.domain.usecase.order;

import com.undercontroll.domain.enums.ApprovalDecision;

public interface DecidePublicOrderPort {

    PublicOrderView execute(String token, ApprovalDecision decision);
}
