package com.precious.finance_tracker.dtos.gemini;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.precious.finance_tracker.enums.TransactionDirection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiTransactionResponseDto {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime datetime;
    private BigDecimal amount;
    private String note;
    private String category;
    private TransactionDirection direction;
}
