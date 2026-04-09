package com.precious.finance_tracker.dtos.gemini;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("dt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime datetime;

    @JsonProperty("amt")
    private BigDecimal amount;

    @JsonProperty("cat")
    private String category;

    @JsonProperty("note")
    private String note;

    @JsonProperty("dir")
    private TransactionDirection direction;
}
