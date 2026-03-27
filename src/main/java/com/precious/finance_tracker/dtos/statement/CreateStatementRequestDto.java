package com.precious.finance_tracker.dtos.statement;

import com.precious.finance_tracker.types.DocumentUrls;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.YearMonth;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateStatementRequestDto {
    @NonNull
    private YearMonth month;

    @NonNull
    private List<DocumentUrls> documentUrls;
}
