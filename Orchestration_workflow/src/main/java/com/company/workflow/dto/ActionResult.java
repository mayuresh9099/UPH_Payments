package com.company.workflow.dto;

import com.company.workflow.action.ActionStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActionResult {

    private final ActionStatus status;
    private final String errorCode;
    private final String errorMessage;
    private final Object responsePayload;

    public static ActionResult success(String message) {
        return ActionResult.builder()
                .status(ActionStatus.SUCCESS)
                .errorMessage(message)
                .build();
    }

    public static ActionResult businessFailure(String errorCode, String errorMessage) {
        return ActionResult.builder()
                .status(ActionStatus.BUSINESS_FAILURE)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }

    public static ActionResult technicalFailure(String errorCode, String errorMessage) {
        return ActionResult.builder()
                .status(ActionStatus.TECHNICAL_FAILURE)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
