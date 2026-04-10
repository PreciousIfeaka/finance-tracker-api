package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.budget.*;
import com.precious.finance_tracker.entities.Budget;
import com.precious.finance_tracker.entities.Income;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.enums.ExpenseCategory;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.repositories.BudgetRepository;
import com.precious.finance_tracker.repositories.ExpenseRepository;
import com.precious.finance_tracker.repositories.IncomeRepository;
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
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

        @Mock
        private IUserService userService;
        @Mock
        private IncomeRepository incomeRepository;
        @Mock
        private BudgetRepository budgetRepository;
        @Mock
        private ExpenseRepository expenseRepository;

        @InjectMocks
        private BudgetService budgetService;

        private User mockUser;
        private Budget mockBudget;

        @BeforeEach
        void setUp() {
                mockUser = User.builder()
                                .id(UUID.randomUUID())
                                .email("budget@test.com")
                                .build();

                mockBudget = Budget.builder()
                                .id(UUID.randomUUID())
                                .amount(BigDecimal.valueOf(500))
                                .category(ExpenseCategory.bill)
                                .month(YearMonth.now())
                                .user(mockUser)
                                .build();
        }

        @Test
        void createBudget_ShouldCreateBudget_WhenRulesAreMet() {
                CreateBudgetRequestDto dto = new CreateBudgetRequestDto(BigDecimal.valueOf(200), ExpenseCategory.food,
                                false);
                YearMonth currentMonth = YearMonth.now();

                when(userService.getAuthenticatedUser()).thenReturn(mockUser);

                Income income = new Income();
                when(incomeRepository.findAllByUserIdAndMonthOrderByTransactionDateTimeDesc(eq(mockUser.getId()),
                                eq(currentMonth)))
                                .thenReturn(List.of(income));

                when(incomeRepository.getTotalIncomeByMonth(mockUser.getId(), currentMonth))
                                .thenReturn(BigDecimal.valueOf(1000));

                when(budgetRepository.getTotalBudgetByMonth(mockUser.getId(), currentMonth))
                                .thenReturn(BigDecimal.valueOf(0)); // Note: Mockito defaults to 0 if not mocked, but we
                                                                    // should be explicit, however the repo might return
                                                                    // null in real life. Assuming it returns 0 or we
                                                                    // handle it in service. Let's return ZERO.
                when(budgetRepository.getTotalBudgetByMonth(mockUser.getId(), currentMonth))
                                .thenReturn(BigDecimal.ZERO);

                when(budgetRepository.findByUserIdAndMonthAndCategory(mockUser.getId(), currentMonth,
                                ExpenseCategory.food))
                                .thenReturn(Optional.empty());
                when(budgetRepository.findByUserIdAndMonthAndCategory(mockUser.getId(), currentMonth,
                                ExpenseCategory.all))
                                .thenReturn(Optional.empty());

                Page<Budget> emptyPage = new PageImpl<>(List.of());
                when(budgetRepository.findAllByUserIdAndMonthOrderByCreatedAtDesc(eq(mockUser.getId()),
                                eq(currentMonth), any(PageRequest.class)))
                                .thenReturn(emptyPage);

                when(budgetRepository.findByUserIdAndAmountAndCategory(mockUser.getId(), BigDecimal.valueOf(200),
                                ExpenseCategory.food))
                                .thenReturn(Optional.empty());

                when(expenseRepository.sumExpenseByUserIdAndMonthAndCategory(mockUser.getId(), currentMonth,
                                ExpenseCategory.food))
                                .thenReturn(BigDecimal.ZERO);

                when(budgetRepository.save(any(Budget.class))).thenReturn(mockBudget);

                BaseResponseDto<Budget> result = budgetService.createBudget(dto);

                assertEquals("Success", result.getStatus());
                verify(budgetRepository).save(any(Budget.class));
        }

        @Test
        void createBudget_ShouldThrowBadRequest_WhenNoIncome() {
                CreateBudgetRequestDto dto = new CreateBudgetRequestDto(BigDecimal.valueOf(200), ExpenseCategory.food,
                                false);
                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                when(incomeRepository.findAllByUserIdAndMonthOrderByTransactionDateTimeDesc(eq(mockUser.getId()),
                                any()))
                                .thenReturn(List.of());

                assertThrows(BadRequestException.class, () -> budgetService.createBudget(dto));
                verify(budgetRepository, never()).save(any());
        }

        @Test
        void createBudget_ShouldThrowBadRequest_WhenBudgetExceedsIncome() {
                CreateBudgetRequestDto dto = new CreateBudgetRequestDto(BigDecimal.valueOf(2000), ExpenseCategory.food,
                                false);
                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                when(incomeRepository.findAllByUserIdAndMonthOrderByTransactionDateTimeDesc(eq(mockUser.getId()),
                                any()))
                                .thenReturn(List.of(new Income()));

                when(incomeRepository.getTotalIncomeByMonth(eq(mockUser.getId()), any()))
                                .thenReturn(BigDecimal.valueOf(1000));
                when(budgetRepository.getTotalBudgetByMonth(eq(mockUser.getId()), any())).thenReturn(BigDecimal.ZERO);

                assertThrows(BadRequestException.class, () -> budgetService.createBudget(dto));
                verify(budgetRepository, never()).save(any());
        }

        @Test
        void getBudget_ShouldReturnBudget() {
                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                when(budgetRepository.findByIdAndUserId(mockBudget.getId(), mockUser.getId()))
                                .thenReturn(Optional.of(mockBudget));

                BaseResponseDto<Budget> result = budgetService.getBudget(mockBudget.getId());

                assertEquals("Success", result.getStatus());
                assertEquals(mockBudget.getId(), result.getData().getId());
        }

        @Test
        void updateBudget_ShouldUpdateWhenValid() {
                UpdateBudgetRequestDto dto = new UpdateBudgetRequestDto(BigDecimal.valueOf(600), ExpenseCategory.bill,
                                true);
                UUID id = mockBudget.getId();

                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                when(budgetRepository.findByIdAndUserId(id, mockUser.getId())).thenReturn(Optional.of(mockBudget));

                when(incomeRepository.getTotalIncomeByMonth(mockUser.getId(), mockBudget.getMonth()))
                                .thenReturn(BigDecimal.valueOf(2000));
                when(budgetRepository.getTotalBudgetByMonth(mockUser.getId(), mockBudget.getMonth()))
                                .thenReturn(BigDecimal.valueOf(500));

                when(expenseRepository.sumExpenseByUserIdAndMonthAndCategory(mockUser.getId(), mockBudget.getMonth(),
                                ExpenseCategory.bill))
                                .thenReturn(BigDecimal.valueOf(100));

                BaseResponseDto<Budget> result = budgetService.updateBudget(id, dto);

                assertEquals("Success", result.getStatus());
                assertEquals(BigDecimal.valueOf(600), mockBudget.getAmount());
                assertTrue(mockBudget.getIsRecurring());
                verify(budgetRepository).save(mockBudget);
        }

        @Test
        void getAllBudgets_ShouldReturnPagedResponse() {
                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                Page<Budget> page = new PageImpl<>(List.of(mockBudget));
                when(budgetRepository.findAllByUserId(eq(mockUser.getId()), any(PageRequest.class))).thenReturn(page);
                when(budgetRepository.sumBudget(mockUser.getId())).thenReturn(BigDecimal.valueOf(500));

                BaseResponseDto<PagedBudgetResponseDto> result = budgetService.getAllBudgets(1, 10);

                assertEquals("Success", result.getStatus());
                assertEquals(1, result.getData().getTotal());
                assertEquals(BigDecimal.valueOf(500), result.getData().getTotalBudget());
        }

        @Test
        void deleteBudgetById_ShouldDelete() {
                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                when(budgetRepository.findByIdAndUserId(mockBudget.getId(), mockUser.getId()))
                                .thenReturn(Optional.of(mockBudget));

                BaseResponseDto<Object> result = budgetService.deleteBudgetById(mockBudget.getId());

                assertEquals("Success", result.getStatus());
                verify(budgetRepository).deleteById(mockBudget.getId());
        }

        @Test
        void getBudgetsByCategory_ShouldReturnAggregates_ForMonth() {
                YearMonth month = YearMonth.of(2026, 4);
                Object[] row = { ExpenseCategory.food, BigDecimal.valueOf(500.00), 0 };
                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                when(budgetRepository.sumBudgetGroupedByCategoryAndMonth(mockUser.getId(), month))
                                .thenReturn(List.<Object[]>of(row));

                BaseResponseDto<List<BudgetByCategoryDto>> result = budgetService.getBudgetsByCategory(month, null);

                assertEquals("Success", result.getStatus());
                assertEquals(1, result.getData().size());
                assertEquals(ExpenseCategory.food, result.getData().get(0).category());
                assertEquals(BigDecimal.valueOf(500.00), result.getData().get(0).total());
                assertFalse(result.getData().get(0).isExceeded());
                verify(budgetRepository).sumBudgetGroupedByCategoryAndMonth(mockUser.getId(), month);
                verify(budgetRepository, never()).sumBudgetGroupedByCategoryAndYear(any(), anyInt());
        }

        @Test
        void getBudgetsByCategory_ShouldReturnAggregates_ForYear_WithExceededFlag() {
                int year = 2026;
                Object[] row1 = { ExpenseCategory.bill, BigDecimal.valueOf(1200.00), 1 };
                Object[] row2 = { ExpenseCategory.entertainment, BigDecimal.valueOf(400.00), 0 };
                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                when(budgetRepository.sumBudgetGroupedByCategoryAndYear(mockUser.getId(), year))
                                .thenReturn(List.<Object[]>of(row1, row2));

                BaseResponseDto<List<BudgetByCategoryDto>> result = budgetService.getBudgetsByCategory(null, year);

                assertEquals("Success", result.getStatus());
                assertEquals(2, result.getData().size());
                assertTrue(result.getData().get(0).isExceeded(), "bill should be exceeded");
                assertFalse(result.getData().get(1).isExceeded(), "entertainment should not be exceeded");
                verify(budgetRepository).sumBudgetGroupedByCategoryAndYear(mockUser.getId(), year);
                verify(budgetRepository, never()).sumBudgetGroupedByCategoryAndMonth(any(), any());
        }

        @Test
        void getBudgetsByCategory_ShouldDefaultToCurrentMonth_WhenBothParamsNull() {
                Object[] row = { ExpenseCategory.food, BigDecimal.valueOf(300.00), 0 };
                when(userService.getAuthenticatedUser()).thenReturn(mockUser);
                when(budgetRepository.sumBudgetGroupedByCategoryAndMonth(
                                eq(mockUser.getId()), any(YearMonth.class)))
                                .thenReturn(List.<Object[]>of(row));

                BaseResponseDto<List<BudgetByCategoryDto>> result = budgetService.getBudgetsByCategory(null, null);

                assertEquals("Success", result.getStatus());
                assertEquals(1, result.getData().size());
        }
}
