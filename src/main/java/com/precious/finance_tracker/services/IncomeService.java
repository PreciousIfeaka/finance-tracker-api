package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.income.AddIncomeRequestDto;
import com.precious.finance_tracker.dtos.income.MonthlyIncomeStatsResponseDto;
import com.precious.finance_tracker.dtos.income.PagedIncomeResponseDto;
import com.precious.finance_tracker.dtos.income.UpdateIncomeRequestDto;
import com.precious.finance_tracker.entities.Income;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.ConflictResourceException;
import com.precious.finance_tracker.exceptions.ForbiddenException;
import com.precious.finance_tracker.exceptions.NotFoundException;
import com.precious.finance_tracker.repositories.IncomeRepository;
import com.precious.finance_tracker.services.interfaces.IIncomeService;
import com.precious.finance_tracker.services.interfaces.IUserService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Data
public class IncomeService implements IIncomeService {
    private static Logger log = LoggerFactory.getLogger(IncomeService.class.getName());

    private final IncomeRepository incomeRepository;
    private final IUserService userService;

    @Transactional
    public BaseResponseDto<Income> addIncome(AddIncomeRequestDto dto) {
        User user = this.userService.getAuthenticatedUser();

        Optional<Income> existingIncome = this.incomeRepository.findRecurringIncome(
                user.getId(), dto.getDateOfReceipt(), dto.getAmount(), dto.getSource()
        );

        if (existingIncome.isPresent()) {
            throw new ConflictResourceException("Similar income entry already exists.");
        } else if (user.getCurrency() == null) {
            throw new BadRequestException("Currency has not been set in user profile");
        } else if (dto.getAmount().compareTo(BigDecimal.valueOf(50)) < 0) {
            throw new BadRequestException("Minimum amount is " + user.getCurrency() + "50.");
        } else if (dto.getDateOfReceipt().isAfter(LocalDate.now())) {
            throw new BadRequestException("Date of receipt cannot be in the future");
        }

        Income income = Income.builder()
                .amount(dto.getAmount())
                .type(dto.getType())
                .note(dto.getNote())
                .source(dto.getSource())
                .date(dto.getDateOfReceipt())
                .user(user)
                .build();

        return BaseResponseDto.<Income>builder()
                .status("Success")
                .message("Successfully added income")
                .data(this.incomeRepository.save(income))
                .build();
    }

    @Transactional
    public BaseResponseDto<Income> updateIncome(UUID id, UpdateIncomeRequestDto dto) {
        User user = this.userService.getAuthenticatedUser();

        if (dto.getAmount() != null && dto.getAmount().compareTo(BigDecimal.valueOf(50)) < 0) {
            throw new BadRequestException("Minimum amount is " + user.getCurrency() + "50.");
        }

        Income income = this.incomeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Income with given ID not found"));

        if (!user.getIncomes().contains(income)) {
            throw new ForbiddenException("Forbidden access to specified income");
        }

        if (dto.getAmount() != null) income.setAmount(dto.getAmount());
        if (dto.getNote() != null) income.setNote(dto.getNote());
        if (dto.getSource() != null) income.setSource(dto.getSource());
        if (dto.getType() != null) income.setType(dto.getType());

        return BaseResponseDto.<Income>builder()
                .message("Successfully updated income data")
                .status("Success")
                .data(this.incomeRepository.save(income))
                .build();
    }

    public BaseResponseDto<Income> getIncomeById(UUID id) {
        User user = this.userService.getAuthenticatedUser();

        Income income = this.incomeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Income not found"));

        if (!income.getUser().getEmail().equals(user.getEmail())) {
            throw new ForbiddenException("Forbidden access to this income data");
        }

        return BaseResponseDto.<Income>builder()
                .status("Success")
                .message("Successfully retrieved income")
                .data(income)
                .build();
    }

    public BaseResponseDto<PagedIncomeResponseDto> getAllIncomesByMonth(
            int page, int limit, LocalDate date
    ) {
        User user = this.userService.getAuthenticatedUser();

        Page<Income> incomes = this.incomeRepository.findByUserAndDate(
                user.getId(), date.getYear(), date.getMonthValue(), PageRequest.of(page, limit)
        );

        return BaseResponseDto.<PagedIncomeResponseDto>builder()
                .status("Success")
                .message("Successfully retrieved incomes")
                .data(new PagedIncomeResponseDto(incomes))
                .build();
    }

    public BaseResponseDto<PagedIncomeResponseDto> getAllIncomes(int page, int limit) {
        User user = this.userService.getAuthenticatedUser();

        Page<Income> incomes = this.incomeRepository
                .findByUserIdAndDeletedAtIsNull(user.getId(), PageRequest.of(page, limit));

        return BaseResponseDto.<PagedIncomeResponseDto>builder()
                .status("Success")
                .message("Successfully retrieved all incomes")
                .data(new PagedIncomeResponseDto(incomes))
                .build();
    }

    @Transactional
    public BaseResponseDto<Object> deleteIncomeById(UUID id) {
        Income income = this.getIncomeById(id).getData();

        income.setDeletedAt(LocalDateTime.now());

        this.incomeRepository.save(income);

        return BaseResponseDto.builder()
                .status("Success")
                .message("Successfully deleted income with ID " + income.getId())
                .data(null)
                .build();
    }

    public BaseResponseDto<List<MonthlyIncomeStatsResponseDto>> getMonthlyIncomeStats() {
        User user = this.userService.getAuthenticatedUser();

        List<Object[]> results = this.incomeRepository.sumIncomeByMonth(user.getId());

        List<MonthlyIncomeStatsResponseDto> stats = results.stream()
                .map(row -> new MonthlyIncomeStatsResponseDto(
                        (String) row[0],               // month in "YYYY-MM"
                        (BigDecimal) row[1]            // total amount
                ))
                .toList();

        return BaseResponseDto.<List<MonthlyIncomeStatsResponseDto>>builder()
                .status("Success")
                .message("Successfully retrieved monthly income stats")
                .data(stats)
                .build();

    }
}
