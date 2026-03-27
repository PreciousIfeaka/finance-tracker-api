package com.precious.finance_tracker.services.interfaces;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.statement.CreateStatementRequestDto;
import com.precious.finance_tracker.dtos.statement.PagedBankStatementResponseDto;
import com.precious.finance_tracker.dtos.statement.UpdateStatementRequestDto;
import com.precious.finance_tracker.entities.BankStatement;

import java.util.UUID;

public interface IBankStatementService {
    BaseResponseDto<BankStatement> addBankStatement(
            CreateStatementRequestDto dto
    );
    BaseResponseDto<BankStatement> updateBankStatements(
            UUID bankStatementId,
            UpdateStatementRequestDto dto
    );
    BaseResponseDto<BankStatement> getBankStatement(
            UUID id
    );
    BaseResponseDto<PagedBankStatementResponseDto> getAllStatements(
            int page, int limit
    );
    BaseResponseDto<Object> deleteBankStatement(UUID bankStatementId);

    BaseResponseDto<BankStatement> startAnalysis(UUID bankStatementId);

    void analyseBankStatementAndNotify(UUID UserId, UUID bankStatementId);
}
