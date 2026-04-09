package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.budget.DeleteByIdsDto;
import com.precious.finance_tracker.dtos.transactions.*;
import com.precious.finance_tracker.entities.Transactions;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.enums.Currency;
import com.precious.finance_tracker.enums.TransactionDirection;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.NotFoundException;
import com.precious.finance_tracker.repositories.TransactionRepository;
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
class TransactionsServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionsService transactionsService;

    private User mockUser;
    private Transactions mockTransaction;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .name("John")
                .email("john@example.com")
                .currency(Currency.USD)
                .build();

        mockTransaction = Transactions.builder()
                .id(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100.00))
                .direction(TransactionDirection.credit)
                .month(YearMonth.now())
                .transactionDateTime(LocalDateTime.now())
                .user(mockUser)
                .description("Test Income")
                .build();
    }

    @Test
    void createTransaction_ShouldSaveTransaction() {
        CreateTransactionDto dto = new CreateTransactionDto();
        dto.setAmount(BigDecimal.valueOf(50.00));
        dto.setDirection(TransactionDirection.debit);
        dto.setTransactionDateTime(LocalDateTime.now());
        dto.setDescription("Groceries");

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);

        transactionsService.createTransaction(dto);

        verify(transactionRepository).save(any(Transactions.class));
    }

    @Test
    void createTransaction_ShouldThrowBadRequest_WhenCurrencyNotSet() {
        mockUser.setCurrency(null);
        CreateTransactionDto dto = new CreateTransactionDto();

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);

        assertThrows(BadRequestException.class, () -> transactionsService.createTransaction(dto));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void getTransactionById_ShouldReturnTransaction() {
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUserId(mockTransaction.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockTransaction));

        BaseResponseDto<Transactions> result = transactionsService.getTransactionById(mockTransaction.getId());

        assertEquals("Success", result.getStatus());
        assertEquals(mockTransaction.getId(), result.getData().getId());
    }

    @Test
    void getTransactionById_ShouldThrowNotFound_WhenTransactionNotFound() {
        UUID id = UUID.randomUUID();
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUserId(id, mockUser.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> transactionsService.getTransactionById(id));
    }

    @Test
    void updateTransaction_ShouldUpdateAndSave() {
        UpdateTransactionDto dto = UpdateTransactionDto.builder()
                .amount(BigDecimal.valueOf(150.00))
                .description("Updated Income")
                .build();

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUserId(mockTransaction.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockTransaction));
        when(transactionRepository.save(any(Transactions.class))).thenReturn(mockTransaction);

        BaseResponseDto<Transactions> result = transactionsService.updateTransaction(mockTransaction.getId(), dto);

        assertEquals("Success", result.getStatus());
        verify(transactionRepository).save(mockTransaction);
        assertEquals(BigDecimal.valueOf(150.00), mockTransaction.getAmount());
        assertEquals("Updated Income", mockTransaction.getDescription());
    }

    @Test
    void getFilteredTransactions_ShouldReturnPagedTransactions() {
        YearMonth month = YearMonth.now();
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        Page<Transactions> page = new PageImpl<>(List.of(mockTransaction));

        when(transactionRepository.findByUserIdAndMonthAndDirection(
                eq(mockUser.getId()), eq(month), eq(TransactionDirection.credit), any(PageRequest.class)))
                .thenReturn(page);

        TransactionTotals totals = mock(TransactionTotals.class);
        when(totals.getCredit()).thenReturn(BigDecimal.valueOf(100.00));
        when(totals.getDebit()).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.getTotalUserTransactionsAmount(mockUser.getId(), null, month)).thenReturn(totals);

        BaseResponseDto<PagedTransactionResponseDto> result = transactionsService.getFilteredTransactions(
                1, 10, month, TransactionDirection.credit);

        assertEquals("Success", result.getStatus());
        assertEquals(1, result.getData().getTotal());
        assertEquals(BigDecimal.valueOf(100.00), result.getData().getBalance());
    }

    @Test
    void deleteTransactionById_ShouldDeleteTransaction() {
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUserId(mockTransaction.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockTransaction));

        BaseResponseDto<Object> result = transactionsService.deleteTransactionById(mockTransaction.getId());

        assertEquals("Success", result.getStatus());
        verify(transactionRepository).deleteById(mockTransaction.getId());
    }

    @Test
    void deleteTransactionsByIds_ShouldDeleteMultiple() {
        DeleteByIdsDto dto = new DeleteByIdsDto(List.of(UUID.randomUUID(), UUID.randomUUID()));

        BaseResponseDto<Object> result = transactionsService.deleteTransactionsByIds(dto);

        assertEquals("Success", result.getStatus());
        verify(transactionRepository).deleteAllById(dto.ids());
    }
}
