package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.income.AddIncomeRequestDto;
import com.precious.finance_tracker.dtos.income.PagedIncomeResponseDto;
import com.precious.finance_tracker.dtos.income.UpdateIncomeRequestDto;
import com.precious.finance_tracker.entities.Income;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.exceptions.NotFoundException;
import com.precious.finance_tracker.repositories.IncomeRepository;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Data
@Transactional
public class IncomeService {
    private final IncomeRepository incomeRepository;
    private final UserService userService;

    public BaseResponseDto<Income> addIncome(AddIncomeRequestDto dto) {
        User user = this.userService.getAuthenticatedUser();

        Income income = Income.builder()
                .amount(dto.getAmount())
                .type(dto.getType())
                .note(dto.getNote())
                .source(dto.getSource())
                .user(user)
                .build();

        return BaseResponseDto.<Income>builder()
                .status("Success")
                .message("Successfully added income")
                .data(this.incomeRepository.save(income))
                .build();
    }

    public BaseResponseDto<Income> updateIncome(UUID id, UpdateIncomeRequestDto dto) {
        Income income = this.incomeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Income with given ID not found"));

        if (dto.getAmount() != null) income.setAmount(dto.getAmount());
        if (dto.getNote() != null) income.setNote(dto.getNote());
        if (dto.getSource() != null) income.setSource(dto.getSource());
        if (dto.getType() != null) income.setType(dto.getType());

        return BaseResponseDto.<Income>builder()
                .message("Successfully retrieved income data")
                .status("Success")
                .data(this.incomeRepository.save(income))
                .build();
    }

    public Income getIncomeById(UUID id) {
        return this.incomeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Income not found"));
    }

    public BaseResponseDto<PagedIncomeResponseDto> getAllIncome(int page, int limit) {
        User user = this.userService.getAuthenticatedUser();

        List<Income> incomes = this.incomeRepository.findByUserId(user.getId());

        int fromIndex = page * limit;
        int incomesSize = incomes.size();

        int toIndex = Math.min(fromIndex + limit, incomesSize);

        List<Income> pagedIncome = incomes.subList(fromIndex, toIndex);

        PagedIncomeResponseDto pageResp = new PagedIncomeResponseDto(
                new PageImpl<>(
                        pagedIncome, PageRequest.of(page, limit), incomesSize
                )
        );

        return BaseResponseDto.<PagedIncomeResponseDto>builder()
                .status("Success")
                .message("Successfully retrieved incomes")
                .data(pageResp)
                .build();
    }
}
