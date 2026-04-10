package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.expense.AddExpenseRequestDto;
import com.precious.finance_tracker.dtos.expense.ExpenseByCategoryDto;
import com.precious.finance_tracker.dtos.expense.MonthlyExpenseStatsResponseDto;
import com.precious.finance_tracker.dtos.expense.PagedExpenseResponseDto;
import com.precious.finance_tracker.dtos.expense.UpdateExpenseRequestDto;
import com.precious.finance_tracker.dtos.transactions.CreateTransactionDto;
import com.precious.finance_tracker.entities.Budget;
import com.precious.finance_tracker.entities.Expense;
import com.precious.finance_tracker.entities.Transactions;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.enums.ExpenseCategory;
import com.precious.finance_tracker.enums.TransactionDirection;
import com.precious.finance_tracker.exceptions.ForbiddenException;
import com.precious.finance_tracker.repositories.BudgetRepository;
import com.precious.finance_tracker.repositories.ExpenseRepository;
import com.precious.finance_tracker.repositories.TransactionRepository;
import com.precious.finance_tracker.repositories.UserRepository;
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
class ExpenseServiceTest {

        @Mock
        private UserRepository userRepository;
        @Mock
        private ExpenseRepository expenseRepository;
        @Mock
        private IUserService userService;
        @Mock
        private BudgetRepository budgetRepository;
        @Mock
        private TransactionsService transactionsService;
        @Mock
        private TransactionRepository transactionRepository;

        @InjectMocks
        private ExpenseService expenseService;

        private User mockUser;
        private Expense mockExpense;

        @BeforeEach
        void setUp() {
                mockUser = User.builder()
                                .id(UUID.randomUUID())
                                .email("user@test.com")
                                .build();

                mockExpense = Expense.builder()
                                .id(UUID.randomUUID())
                                .amount(BigDecimal.valueOf(200.00))
                                .category(ExpenseCategory.food)
                                .month(YearMonth.now())
                                .user(mockUser)
                                .build();
        }

        @Test
        void addExpenseData_ShouldSaveExpenseAndTransaction_WithBudgetExceededWarn() {
                AddExpenseRequestDto dto = new AddExpenseRequestDto();
                dto.setAmount(BigDecimal.valueOf(150.00));
                dto.setCategory(ExpenseCategory.food);
                dto.setNote("Lunch");
                dto.setTransactionDateTime(LocalDateTime.now());

                when(userService.getAuthenticatedUser()).thenReturn(mockUser);

                Budget budget = new Budget();
                budget.setAmount(BigDecimal.valueOf(100.00)); // Budget < Expense
                when(budgetRepository.findByUserIdAndMonthAndCategory(eq(mockUser.getId()), any(YearMonth.class),
                                eq(ExpenseCategory.food)))
                                .thenReturn(Optional.of(budget));

                when(expenseRepository.save(any(Expense.class))).thenReturn(mockExpense);

                BaseResponseDto<Expense> result = expenseService.addExpenseData(dto);

                assertTrue(result.getMessage().contains("budget is exceeded"));
                verify(budgetRepository).save(budget);
                verify(transactionsService).createTransaction(any(CreateTransactionDto.class));
                verify(expenseRepository).save(any(Expense.class));
        }

        @Test
        void updateExpense_ShouldUpdateAndHandleTransactions() {
                UUID id = mockExpense.getId();
                UpdateExpenseRequestDto dto = new UpdateExpenseRequestDto();
                dto.setAmount(BigDecimal.valueOf(250.00));
                mockUser.setExpenses(List.of(mockExpense));

                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                when(expenseRepository.findById(id)).thenReturn(Optional.of(mockExpense));
                when(budgetRepository.findByUserIdAndMonthAndCategory(any(), any(), any()))
                                .thenReturn(Optional.empty());

                Transactions mockTransaction = new Transactions();
                when(transactionRepository.findByUserIdAndAmountAndDirectionAndTransactionDateTimeAndDescription(
                                eq(mockUser.getId()), any(), eq(TransactionDirection.debit), any(), any()))
                                .thenReturn(Optional.of(mockTransaction));

                when(expenseRepository.save(any(Expense.class))).thenReturn(mockExpense);

                BaseResponseDto<Expense> result = expenseService.updateExpense(id, dto);

                assertEquals("Success", result.getStatus());
                verify(transactionRepository).save(mockTransaction);
                verify(expenseRepository).save(mockExpense);
                assertEquals(BigDecimal.valueOf(250.00), mockExpense.getAmount());
        }

        @Test
        void updateExpense_ShouldThrowForbidden_WhenUserDoesNotOwnExpense() {
                UUID id = mockExpense.getId();
                UpdateExpenseRequestDto dto = new UpdateExpenseRequestDto();
                mockUser.setExpenses(List.of()); // Empty list

                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                when(expenseRepository.findById(id)).thenReturn(Optional.of(mockExpense));

                assertThrows(ForbiddenException.class, () -> expenseService.updateExpense(id, dto));
        }

        @Test
        void getExpenseById_ShouldReturnExpense() {
                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                when(expenseRepository.findByIdAndUserId(mockExpense.getId(), mockUser.getId()))
                                .thenReturn(Optional.of(mockExpense));

                BaseResponseDto<Expense> result = expenseService.getExpenseById(mockExpense.getId());

                assertEquals("Success", result.getStatus());
                assertEquals(mockExpense.getId(), result.getData().getId());
        }

        @Test
        void getAllExpensesByMonth_ShouldReturnPagedResponse() {
                YearMonth month = YearMonth.now();
                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                Page<Expense> page = new PageImpl<>(List.of(mockExpense));

                when(expenseRepository.findAllByUserIdAndMonthOrderByTransactionDateTimeDesc(
                                eq(mockUser.getId()), eq(month), any(PageRequest.class))).thenReturn(page);
                when(expenseRepository.getTotalExpenseByMonth(mockUser.getId(), month))
                                .thenReturn(BigDecimal.valueOf(200.00));

                BaseResponseDto<PagedExpenseResponseDto> result = expenseService.getAllExpensesByMonth(1, 10, month);

                assertEquals("Success", result.getStatus());
                assertEquals(1, result.getData().getTotal());
                assertEquals(BigDecimal.valueOf(200.00), result.getData().getTotalExpenses());
        }

        @Test
        void deleteExpenseById_ShouldDelete() {
                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                when(expenseRepository.findByIdAndUserId(mockExpense.getId(), mockUser.getId()))
                                .thenReturn(Optional.of(mockExpense));

                BaseResponseDto<Object> result = expenseService.deleteExpenseById(mockExpense.getId());

                assertEquals("Success", result.getStatus());
                verify(expenseRepository).deleteById(mockExpense.getId());
        }

        @Test
        void getMonthlyExpenseStats_ShouldReturnStats() {
                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                Object[] row = { YearMonth.now(), BigDecimal.valueOf(200.00) };
                when(expenseRepository.sumExpenseByMonth(mockUser.getId())).thenReturn(List.<Object[]>of(row));

                BaseResponseDto<List<MonthlyExpenseStatsResponseDto>> result = expenseService.getMonthlyExpenseStats();

                assertEquals("Success", result.getStatus());
                assertEquals(1, result.getData().size());
                assertEquals(BigDecimal.valueOf(200.00), result.getData().get(0).total());
        }

        @Test
        void getExpensesByCategory_ShouldReturnAggregates_ForMonth() {
                YearMonth month = YearMonth.of(2026, 4);
                Object[] row = { ExpenseCategory.food, BigDecimal.valueOf(450.00) };
                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                when(expenseRepository.sumExpenseGroupedByCategoryAndMonth(mockUser.getId(), month))
                                .thenReturn(List.<Object[]>of(row));

                BaseResponseDto<List<ExpenseByCategoryDto>> result = expenseService.getExpensesByCategory(month, null);

                assertEquals("Success", result.getStatus());
                assertEquals(1, result.getData().size());
                assertEquals(ExpenseCategory.food, result.getData().get(0).category());
                assertEquals(BigDecimal.valueOf(450.00), result.getData().get(0).total());
                verify(expenseRepository).sumExpenseGroupedByCategoryAndMonth(mockUser.getId(), month);
                verify(expenseRepository, never()).sumExpenseGroupedByCategoryAndYear(any(), anyInt());
        }

        @Test
        void getExpensesByCategory_ShouldReturnAggregates_ForYear() {
                int year = 2026;
                Object[] row1 = { ExpenseCategory.food, BigDecimal.valueOf(1200.00) };
                Object[] row2 = { ExpenseCategory.entertainment, BigDecimal.valueOf(300.00) };
                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                when(expenseRepository.sumExpenseGroupedByCategoryAndYear(mockUser.getId(), year))
                                .thenReturn(List.<Object[]>of(row1, row2));

                BaseResponseDto<List<ExpenseByCategoryDto>> result = expenseService.getExpensesByCategory(null, year);

                assertEquals("Success", result.getStatus());
                assertEquals(2, result.getData().size());
                assertEquals(ExpenseCategory.food, result.getData().get(0).category());
                assertEquals(BigDecimal.valueOf(1200.00), result.getData().get(0).total());
                verify(expenseRepository).sumExpenseGroupedByCategoryAndYear(mockUser.getId(), year);
                verify(expenseRepository, never()).sumExpenseGroupedByCategoryAndMonth(any(), any());
        }

        @Test
        void getExpensesByCategory_ShouldDefaultToCurrentMonth_WhenBothParamsNull() {
                Object[] row = { ExpenseCategory.bill, BigDecimal.valueOf(100.00) };
                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                when(expenseRepository.sumExpenseGroupedByCategoryAndMonth(
                                eq(mockUser.getId()), any(YearMonth.class)))
                                .thenReturn(List.<Object[]>of(row));

                BaseResponseDto<List<ExpenseByCategoryDto>> result = expenseService.getExpensesByCategory(null, null);

                assertEquals("Success", result.getStatus());
                assertEquals(1, result.getData().size());
        }
}
