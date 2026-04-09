package com.precious.finance_tracker.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.statement.CreateStatementRequestDto;
import com.precious.finance_tracker.dtos.statement.UpdateStatementRequestDto;
import com.precious.finance_tracker.entities.BankStatement;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.enums.StatementAnalysisStatus;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.ConflictResourceException;
import com.precious.finance_tracker.proxies.GeminiClientProxy;
import com.precious.finance_tracker.repositories.BankStatementRepository;
import com.precious.finance_tracker.repositories.ExpenseRepository;
import com.precious.finance_tracker.repositories.IncomeRepository;
import com.precious.finance_tracker.repositories.TransactionRepository;
import com.precious.finance_tracker.repositories.UserRepository;
import com.precious.finance_tracker.services.interfaces.IEmailService;
import com.precious.finance_tracker.services.interfaces.IS3UploadService;
import com.precious.finance_tracker.services.interfaces.IUserService;
import com.precious.finance_tracker.types.DocumentUrls;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankStatementAnalysisTest {

    @Mock
    private IUserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BankStatementRepository bankStatementRepository;
    @Mock
    private GeminiClientProxy geminiClientProxy;
    @Mock
    private IncomeRepository incomeRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private IEmailService emailService;
    @Mock
    private IS3UploadService s3UploadService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private JobScheduler jobScheduler;

    @InjectMocks
    private BankStatementAnalysis bankStatementAnalysis;

    private User mockUser;
    private BankStatement mockStatement;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bankStatementAnalysis, "geminiApiKey", "test-api-key");

        mockUser = User.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .email("alice@test.com")
                .build();

        mockStatement = BankStatement.builder()
                .id(UUID.randomUUID())
                .month(YearMonth.now())
                .status(StatementAnalysisStatus.PENDING)
                .documentUrls(List.of(new DocumentUrls("test-key", "application/pdf")))
                .user(mockUser)
                .build();
    }

    @Test
    void addBankStatement_ShouldCreateStatement_WhenValid() {
        CreateStatementRequestDto dto = new CreateStatementRequestDto();
        dto.setMonth(YearMonth.now());
        dto.setDocumentUrls(List.of(new DocumentUrls("key1", "pdf")));

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(bankStatementRepository.findByUserIdAndMonth(mockUser.getId(), dto.getMonth()))
                .thenReturn(Optional.empty());
        when(bankStatementRepository.save(any(BankStatement.class))).thenReturn(mockStatement);

        BaseResponseDto<BankStatement> response = bankStatementAnalysis.addBankStatement(dto);

        assertEquals("Success", response.getStatus());
        verify(bankStatementRepository).save(any(BankStatement.class));
    }

    @Test
    void addBankStatement_ShouldThrowConflict_WhenMonthExists() {
        CreateStatementRequestDto dto = new CreateStatementRequestDto();
        dto.setMonth(YearMonth.now());
        dto.setDocumentUrls(List.of(new DocumentUrls("key1", "pdf")));

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(bankStatementRepository.findByUserIdAndMonth(mockUser.getId(), dto.getMonth()))
                .thenReturn(Optional.of(mockStatement));

        assertThrows(ConflictResourceException.class, () -> bankStatementAnalysis.addBankStatement(dto));
        verify(bankStatementRepository, never()).save(any());
    }

    @Test
    void addBankStatement_ShouldThrowBadRequest_WhenMoreThan4Docs() {
        CreateStatementRequestDto dto = new CreateStatementRequestDto();
        dto.setMonth(YearMonth.now());
        dto.setDocumentUrls(List.of(
                new DocumentUrls("1", "pdf"), new DocumentUrls("2", "pdf"),
                new DocumentUrls("3", "pdf"), new DocumentUrls("4", "pdf"), new DocumentUrls("5", "pdf")));

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);

        assertThrows(BadRequestException.class, () -> bankStatementAnalysis.addBankStatement(dto));
    }

    @Test
    void startAnalysis_ShouldEnqueueJobAndSetStatus() {
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(bankStatementRepository.findByIdAndUserId(mockStatement.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockStatement));

        BaseResponseDto<BankStatement> response = bankStatementAnalysis.startAnalysis(mockStatement.getId());

        assertEquals("Success", response.getStatus());
        verify(jobScheduler).enqueue(any(JobLambda.class));
        assertEquals(StatementAnalysisStatus.IN_PROGRESS, mockStatement.getStatus());
        verify(bankStatementRepository).save(mockStatement);
    }

    @Test
    void updateBankStatements_ShouldThrowBadRequest_WhenInProgress() {
        mockStatement.setStatus(StatementAnalysisStatus.IN_PROGRESS);
        UpdateStatementRequestDto dto = new UpdateStatementRequestDto();

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(bankStatementRepository.findByIdAndUserId(mockStatement.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockStatement));

        assertThrows(BadRequestException.class,
                () -> bankStatementAnalysis.updateBankStatements(mockStatement.getId(), dto));
    }

    @Test
    void deleteBankStatement_ShouldDeleteAndRemoveFromS3() {
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(bankStatementRepository.findByIdAndUserId(mockStatement.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockStatement));

        BaseResponseDto<Object> response = bankStatementAnalysis.deleteBankStatement(mockStatement.getId());

        assertEquals("Success", response.getStatus());
        verify(s3UploadService).deleteFromS3("test-key");
        verify(bankStatementRepository).deleteById(mockStatement.getId());
    }
}
