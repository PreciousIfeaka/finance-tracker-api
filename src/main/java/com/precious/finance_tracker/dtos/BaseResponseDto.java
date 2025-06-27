package com.precious.finance_tracker.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BaseResponseDto<T> {
    @NotBlank
    private String status;

    @NotBlank
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;
}
