package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.income.AddIncomeRequestDto;
import com.precious.finance_tracker.dtos.income.MonthlyIncomeStatsResponseDto;
import com.precious.finance_tracker.dtos.income.PagedIncomeResponseDto;
import com.precious.finance_tracker.dtos.income.UpdateIncomeRequestDto;
import com.precious.finance_tracker.dtos.transactions.CreateTransactionDto;
import com.precious.finance_tracker.entities.Income;
import com.precious.finance_tracker.entities.Transactions;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.enums.TransactionDirection;
import com.precious.finance_tracker.exceptions.NotFoundException;
import com.precious.finance_tracker.repositories.IncomeRepository;
import com.precious.finance_tracker.repositories.TransactionRepository;
import com.precious.finance_tracker.services.interfaces.ITransactionService;
import com.precious.finance_tracker.services.interfaces.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomeServiceTest {

    @Mock
    private IncomeRepository incomeRepository;
    @Mock
    private IUserService userService;
    @Mock
    private ITransactionService transactionsService;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private IncomeService incomeService;

    private User mockUser;
    private Income mockIncome;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .build();

        mockIncome = Income.builder()
                .id(UUID.randomUUID())
                .amount(BigDecimal.valueOf(500))
                .note("Salary")
                .month(YearMonth.now())
                .user(mockUser)
                .build();
    }

    @Test
    void addIncome_ShouldAddIncomeAndCreateTransaction() {
        AddIncomeRequestDto dto = new AddIncomeRequestDto();
        dto.setAmount(BigDecimal.valueOf(1000));
        dto.setNote("Bonus");
        dto.setTransactionDateTime(LocalDateTime.now());

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(incomeRepository.save(any(Income.class))).thenReturn(mockIncome);

        BaseResponseDto<Income> result = incomeService.addIncome(dto);

        assertEquals("Success", result.getStatus());
        verify(transactionsService).createTransaction(any(CreateTransactionDto.class));
        verify(incomeRepository).save(any(Income.class));
    }

    @Test
    void updateIncome_ShouldUpdateIncomeAndTransactionIfPresent() {
        UUID id = mockIncome.getId();
        UpdateIncomeRequestDto dto = new UpdateIncomeRequestDto();
        dto.setAmount(BigDecimal.valueOf(600));
        dto.setNote("Updated Salary");

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(incomeRepository.findByIdAndUserId(id, mockUser.getId())).thenReturn(Optional.of(mockIncome));

        Transactions mockTransaction = new Transactions();
        when(transactionRepository.findByUserIdAndAmountAndDirectionAndTransactionDateTimeAndDescription(
                eq(mockUser.getId()), any(), eq(TransactionDirection.credit), any(), any()))
                .thenReturn(Optional.of(mockTransaction));

        when(incomeRepository.save(any(Income.class))).thenReturn(mockIncome);

        BaseResponseDto<Income> result = incomeService.updateIncome(id, dto);

        assertEquals("Success", result.getStatus());
        verify(transactionRepository).save(mockTransaction);
        verify(incomeRepository).save(mockIncome);
        assertEquals(BigDecimal.valueOf(600), mockIncome.getAmount());
    }

    @Test
    void getIncomeById_ShouldReturnIncome() {
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(incomeRepository.findByIdAndUserId(mockIncome.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockIncome));

        BaseResponseDto<Income> result = incomeService.getIncomeById(mockIncome.getId());

        assertEquals("Success", result.getStatus());
        assertEquals(mockIncome.getId(), result.getData().getId());
    }

    @Test
    void getIncomeById_ShouldThrowNotFound() {
        UUID id = UUID.randomUUID();
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(incomeRepository.findByIdAndUserId(id, mockUser.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> incomeService.getIncomeById(id));
    }

    @Test
    void getAllIncomesByMonth_ShouldReturnPagedResponse() {
        YearMonth month = YearMonth.now();
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        Page<Income> page = new PageImpl<>(List.of(mockIncome));
        when(incomeRepository.findAllByUserIdAndMonthOrderByTransactionDateTimeDesc(
                eq(mockUser.getId()), eq(month), any(PageRequest.class))).thenReturn(page);
        when(incomeRepository.getTotalIncomeByMonth(mockUser.getId(), month)).thenReturn(BigDecimal.valueOf(500));

        BaseResponseDto<PagedIncomeResponseDto> result = incomeService.getAllIncomesByMonth(1, 10, month);

        assertEquals("Success", result.getStatus());
        assertEquals(1, result.getData().getTotal());
        assertEquals(BigDecimal.valueOf(500), result.getData().getTotalIncome());
    }

    @Test
    void deleteIncomeById_ShouldDeleteIncome() {
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(incomeRepository.findByIdAndUserId(mockIncome.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockIncome));

        BaseResponseDto<Object> result = incomeService.deleteIncomeById(mockIncome.getId());

        assertEquals("Success", result.getStatus());
        verify(incomeRepository).deleteById(mockIncome.getId());
    }

    @Test
    void getMonthlyIncomeStats_ShouldReturnStats() {
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);

        Object[] row1 = { YearMonth.now(), BigDecimal.valueOf(1000) };
        when(incomeRepository.sumIncomeByMonth(mockUser.getId())).thenReturn(List.<Object[]>of(row1));

        BaseResponseDto<List<MonthlyIncomeStatsResponseDto>> result = incomeService.getMonthlyIncomeStats();

        assertEquals("Success", result.getStatus());
        assertEquals(1, result.getData().size());
        assertEquals(BigDecimal.valueOf(1000), result.getData().get(0).total());
    }

    @Test
    void getIncomesForChart_ShouldReturnIncomes_ForMonth() {
        YearMonth month = YearMonth.of(2026, 4);
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(incomeRepository.findAllByUserIdAndMonthOrderByTransactionDateTimeDesc(
                mockUser.getId(), month))
                .thenReturn(List.of(mockIncome));

        BaseResponseDto<List<Income>> result = incomeService.getIncomesForChart(month, null);

        assertEquals("Success", result.getStatus());
        assertEquals(1, result.getData().size());
        assertEquals(mockIncome.getId(), result.getData().get(0).getId());
        verify(incomeRepository).findAllByUserIdAndMonthOrderByTransactionDateTimeDesc(mockUser.getId(), month);
        verify(incomeRepository, never()).findAllByUserIdAndYear(any(), anyInt());
    }

    @Test
    void getIncomesForChart_ShouldReturnIncomes_ForYear() {
        int year = 2026;
        Income secondIncome = Income.builder()
                .id(UUID.randomUUID())
                .amount(BigDecimal.valueOf(800))
                .month(YearMonth.of(2026, 3))
                .user(mockUser)
                .build();

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(incomeRepository.findAllByUserIdAndYear(mockUser.getId(), year))
                .thenReturn(List.of(mockIncome, secondIncome));

        BaseResponseDto<List<Income>> result = incomeService.getIncomesForChart(null, year);

        assertEquals("Success", result.getStatus());
        assertEquals(2, result.getData().size());
        verify(incomeRepository).findAllByUserIdAndYear(mockUser.getId(), year);
        verify(incomeRepository, never()).findAllByUserIdAndMonthOrderByTransactionDateTimeDesc(any(), any());
    }

    @Test
    void getIncomesForChart_ShouldDefaultToCurrentMonth_WhenBothParamsNull() {
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(incomeRepository.findAllByUserIdAndMonthOrderByTransactionDateTimeDesc(
                eq(mockUser.getId()), any(YearMonth.class)))
                .thenReturn(List.of(mockIncome));

        BaseResponseDto<List<Income>> result = incomeService.getIncomesForChart(null, null);

        assertEquals("Success", result.getStatus());
        assertEquals(1, result.getData().size());
    }
}
