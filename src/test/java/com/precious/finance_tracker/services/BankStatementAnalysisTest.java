package com.precious.finance_tracker.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.gemini.GeminiRequest;
import com.precious.finance_tracker.dtos.gemini.GeminiResponse;
import com.precious.finance_tracker.dtos.gemini.GeminiTransactionResponseDto;
import com.precious.finance_tracker.dtos.statement.CreateStatementRequestDto;
import com.precious.finance_tracker.dtos.statement.PagedBankStatementResponseDto;
import com.precious.finance_tracker.dtos.statement.UpdateStatementRequestDto;
import com.precious.finance_tracker.entities.BankStatement;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.enums.EmailPurpose;
import com.precious.finance_tracker.enums.StatementAnalysisStatus;
import com.precious.finance_tracker.enums.TransactionDirection;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.ConflictResourceException;
import com.precious.finance_tracker.exceptions.NotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
                new DocumentUrls("3", "pdf"), new DocumentUrls("4", "pdf"),
                new DocumentUrls("5", "pdf")));

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);

        assertThrows(BadRequestException.class, () -> bankStatementAnalysis.addBankStatement(dto));
    }

    @Test
    void getBankStatement_ShouldReturnStatement() {
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(bankStatementRepository.findByIdAndUserId(mockStatement.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockStatement));

        BaseResponseDto<BankStatement> response = bankStatementAnalysis.getBankStatement(mockStatement.getId());

        assertEquals("Success", response.getStatus());
        assertEquals(mockStatement.getId(), response.getData().getId());
    }

    @Test
    void getBankStatement_ShouldThrowNotFound_WhenStatementNotFound() {
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(bankStatementRepository.findByIdAndUserId(any(), eq(mockUser.getId())))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bankStatementAnalysis.getBankStatement(UUID.randomUUID()));
    }

    @Test
    void getAllStatements_ShouldReturnPagedStatements() {
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        Page<BankStatement> page = new PageImpl<>(List.of(mockStatement));
        when(bankStatementRepository.findAllByUserIdOrderByCreatedAtDesc(
                eq(mockUser.getId()), any(PageRequest.class))).thenReturn(page);

        BaseResponseDto<PagedBankStatementResponseDto> response = bankStatementAnalysis.getAllStatements(1, 10);

        assertEquals("Success", response.getStatus());
        assertEquals(1, response.getData().getTotal());
    }

    @Test
    void updateBankStatements_ShouldUpdateAndSave_WhenPending() {
        UpdateStatementRequestDto dto = new UpdateStatementRequestDto();
        dto.setDocumentUrls(List.of(new DocumentUrls("new-key", "application/pdf")));

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(bankStatementRepository.findByIdAndUserId(mockStatement.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockStatement));
        when(bankStatementRepository.save(any(BankStatement.class))).thenReturn(mockStatement);

        BaseResponseDto<BankStatement> response = bankStatementAnalysis.updateBankStatements(mockStatement.getId(),
                dto);

        assertEquals("Success", response.getStatus());
        verify(bankStatementRepository).save(any(BankStatement.class));
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
    void updateBankStatements_ShouldThrowBadRequest_WhenAlreadyAnalysed() {
        mockStatement.setStatus(StatementAnalysisStatus.ANALYSED);
        UpdateStatementRequestDto dto = new UpdateStatementRequestDto();

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(bankStatementRepository.findByIdAndUserId(mockStatement.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockStatement));

        assertThrows(BadRequestException.class,
                () -> bankStatementAnalysis.updateBankStatements(mockStatement.getId(), dto));
    }

    @Test
    void startAnalysis_ShouldEnqueueJobAndSetStatus() {
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(bankStatementRepository.findByIdAndUserId(mockStatement.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockStatement));

        BaseResponseDto<BankStatement> response = bankStatementAnalysis.startAnalysis(mockStatement.getId());

        assertEquals("Success", response.getStatus());
        assertEquals(StatementAnalysisStatus.IN_PROGRESS, mockStatement.getStatus());
        verify(bankStatementRepository).save(mockStatement);
        verify(jobScheduler).enqueue(any(JobLambda.class));
    }

    @Test
    void startAnalysis_ShouldReturnImmediately_WhenAlreadyAnalysed() {
        mockStatement.setStatus(StatementAnalysisStatus.ANALYSED);

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(bankStatementRepository.findByIdAndUserId(mockStatement.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockStatement));

        BaseResponseDto<BankStatement> response = bankStatementAnalysis.startAnalysis(mockStatement.getId());

        assertEquals("Success", response.getStatus());
        verify(jobScheduler, never()).enqueue(any(JobLambda.class));
        verify(bankStatementRepository, never()).save(any());
    }

    @Test
    void startAnalysis_ShouldReturnImmediately_WhenAlreadyInProgress() {
        mockStatement.setStatus(StatementAnalysisStatus.IN_PROGRESS);

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(bankStatementRepository.findByIdAndUserId(mockStatement.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockStatement));

        BaseResponseDto<BankStatement> response = bankStatementAnalysis.startAnalysis(mockStatement.getId());

        assertEquals("Success", response.getStatus());
        verify(jobScheduler, never()).enqueue(any(JobLambda.class));
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

    @Test
    void deleteBankStatement_ShouldThrowBadRequest_WhenInProgress() {
        mockStatement.setStatus(StatementAnalysisStatus.IN_PROGRESS);

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);
        when(bankStatementRepository.findByIdAndUserId(mockStatement.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockStatement));

        assertThrows(BadRequestException.class,
                () -> bankStatementAnalysis.deleteBankStatement(mockStatement.getId()));
        verify(bankStatementRepository, never()).deleteById(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void analyseBankStatementAndNotify_ShouldSendSuccessEmail_WhenAnalysisSucceeds() throws Exception {
        GeminiTransactionResponseDto tx = new GeminiTransactionResponseDto();
        tx.setAmount(BigDecimal.valueOf(100));
        tx.setDatetime(LocalDateTime.now());
        tx.setDirection(TransactionDirection.credit);
        tx.setCategory("salary");
        tx.setNote("Salary payment");

        GeminiResponse.Part part = new GeminiResponse.Part();
        part.setText(
                "[{\"dt\":\"2026-04-01T10:00:00\",\"amt\":100.0,\"cat\":\"salary\",\"note\":\"Salary payment\",\"dir\":\"credit\"}]");

        GeminiResponse.Content content = new GeminiResponse.Content();
        content.setParts(List.of(part));

        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate();
        candidate.setContent(content);
        candidate.setFinishReason("STOP");

        GeminiResponse geminiResponse = new GeminiResponse();
        geminiResponse.setCandidates(List.of(candidate));

        when(userRepository.findById(mockUser.getId())).thenReturn(Optional.of(mockUser));
        when(bankStatementRepository.findByIdAndUserId(mockStatement.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockStatement));
        when(s3UploadService.generatePresignedGetUrl("test-key")).thenReturn("https://s3.presigned.url");
        when(geminiClientProxy.analyzeStatement(any(), any(GeminiRequest.class))).thenReturn(geminiResponse);

        JsonNode arrayNode = mock(JsonNode.class);
        when(arrayNode.isArray()).thenReturn(true);
        when(objectMapper.readTree(anyString())).thenReturn(arrayNode);
        when(objectMapper.convertValue(eq(arrayNode), any(TypeReference.class))).thenReturn(List.of(tx));

        bankStatementAnalysis.analyseBankStatementAndNotify(mockUser.getId(), mockStatement.getId());

        assertEquals(StatementAnalysisStatus.ANALYSED, mockStatement.getStatus());
        verify(incomeRepository).save(any());
        verify(emailService)
                .sendStatementEmailAsync(argThat(dto -> dto.getPurpose() == EmailPurpose.statement_processing_success));
    }

    @Test
    void analyseBankStatementAndNotify_ShouldSendFailureEmail_WhenGeminiFails() {
        when(userRepository.findById(mockUser.getId())).thenReturn(Optional.of(mockUser));
        when(bankStatementRepository.findByIdAndUserId(mockStatement.getId(), mockUser.getId()))
                .thenReturn(Optional.of(mockStatement));
        when(s3UploadService.generatePresignedGetUrl("test-key")).thenReturn("https://s3.presigned.url");
        when(geminiClientProxy.analyzeStatement(any(), any(GeminiRequest.class)))
                .thenThrow(new RuntimeException("Gemini API unreachable"));

        bankStatementAnalysis.analyseBankStatementAndNotify(mockUser.getId(), mockStatement.getId());

        assertEquals(StatementAnalysisStatus.PENDING, mockStatement.getStatus());
        verify(emailService)
                .sendStatementEmailAsync(argThat(dto -> dto.getPurpose() == EmailPurpose.statement_processing_failure));
    }
}
