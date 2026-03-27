package com.precious.finance_tracker.dtos.statement;

import com.precious.finance_tracker.types.DocumentUrls;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.YearMonth;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateStatementRequestDto {
    private List<DocumentUrls> documentUrls;

    private YearMonth month;

    private List<DocumentUrls> deleteDocuments;
}
