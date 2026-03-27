package com.precious.finance_tracker.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.email.NotifyStatementProcessingResultDto;
import com.precious.finance_tracker.dtos.gemini.GeminiRequest;
import com.precious.finance_tracker.dtos.gemini.GeminiResponse;
import com.precious.finance_tracker.dtos.gemini.GeminiTransactionResponseDto;
import com.precious.finance_tracker.dtos.statement.CreateStatementRequestDto;
import com.precious.finance_tracker.dtos.statement.PagedBankStatementResponseDto;
import com.precious.finance_tracker.dtos.statement.UpdateStatementRequestDto;
import com.precious.finance_tracker.entities.*;
import com.precious.finance_tracker.enums.EmailPurpose;
import com.precious.finance_tracker.enums.ExpenseCategory;
import com.precious.finance_tracker.enums.StatementAnalysisStatus;
import com.precious.finance_tracker.enums.TransactionDirection;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.ConflictResourceException;
import com.precious.finance_tracker.exceptions.NotFoundException;
import com.precious.finance_tracker.proxies.GeminiClientProxy;
import com.precious.finance_tracker.repositories.*;
import com.precious.finance_tracker.services.interfaces.IBankStatementService;
import com.precious.finance_tracker.services.interfaces.IEmailService;
import com.precious.finance_tracker.services.interfaces.IS3UploadService;
import com.precious.finance_tracker.services.interfaces.IUserService;
import com.precious.finance_tracker.types.DocumentUrls;
import lombok.RequiredArgsConstructor;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class BankStatementAnalysis implements IBankStatementService {
    private static final Logger log = LoggerFactory.getLogger(BankStatementAnalysis.class.getName());

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final IUserService userService;
    private final UserRepository userRepository;
    private final BankStatementRepository bankStatementRepository;
    private final GeminiClientProxy geminiClientProxy;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final TransactionRepository transactionRepository;
    private final IEmailService emailService;
    private final IS3UploadService s3UploadService;

    private final ObjectMapper objectMapper;
    private final JobScheduler jobScheduler;

    public BaseResponseDto<BankStatement> addBankStatement(
            CreateStatementRequestDto dto
    ) {
        User user = this.userService.getAuthenticatedUser();

        int numberOfDocuments = dto.getDocumentUrls().size();

        if (numberOfDocuments > 4) {
            throw new BadRequestException("A maximum of 4 bank statements are allowed per month");
        }

        Optional<BankStatement> existingStatement =
                this.bankStatementRepository
                        .findByUserIdAndMonthAndDeletedAtIsNull(user.getId(), dto.getMonth());

        if (existingStatement.isPresent()) {
            throw new ConflictResourceException("You already have a bank statement record for " + dto.getMonth());
        }

        BankStatement bankStatement = this.bankStatementRepository.save(BankStatement.builder()
                .month(dto.getMonth())
                .status(StatementAnalysisStatus.PENDING)
                .documentUrls(dto.getDocumentUrls())
                .user(user)
                .build());

        return BaseResponseDto.<BankStatement>builder()
                .status("Success")
                .message("Successfully added bank statement record")
                .data(bankStatement)
                .build();
    }

    public BaseResponseDto<BankStatement> updateBankStatements(
            UUID bankStatementId,
            UpdateStatementRequestDto dto
    ) {
        User user = this.userService.getAuthenticatedUser();

        BankStatement bankStatement =
                this.bankStatementRepository.findByIdAndUserIdAndDeletedAtIsNull(bankStatementId, user.getId())
                        .orElseThrow(() -> new NotFoundException("Failed to retrieve bank statement"));

        if (
                bankStatement.getStatus() == StatementAnalysisStatus.ANALYSED
                        || bankStatement.getStatus() == StatementAnalysisStatus.IN_PROGRESS
        ) {
            throw new BadRequestException("Update is not allowed on in-progress or analysed bank statements");
        }

        if (dto.getMonth() != null) {
            Optional<BankStatement> existingMonthStatement =
                    this.bankStatementRepository.findByUserIdAndMonthAndDeletedAtIsNull(user.getId(), dto.getMonth());

            if (existingMonthStatement.isPresent()) {
                throw new ConflictResourceException("You already have a bank statement record for " + dto.getMonth());
            }
        }

        List<DocumentUrls> existingDocuments = new ArrayList<>(bankStatement.getDocumentUrls());

        if (dto.getDeleteDocuments() != null && !dto.getDeleteDocuments().isEmpty()) {
            Set<DocumentUrls> docsToDelete = new HashSet<>(dto.getDeleteDocuments());

            for (DocumentUrls doc:docsToDelete) {
                if (existingDocuments.contains(doc)) {
                    this.s3UploadService.deleteFromS3(doc.getUrl());
                }
            }

            existingDocuments.removeIf(docsToDelete::contains);
        }

        if (dto.getDocumentUrls() != null && !dto.getDocumentUrls().isEmpty()) {
            Set<DocumentUrls> docsToAdd = new HashSet<>(dto.getDocumentUrls());

            List<DocumentUrls> addedDocs = docsToAdd.stream()
                    .filter(docs -> !existingDocuments.contains(docs))
                    .toList();

            existingDocuments.addAll(addedDocs);
        }

        if (existingDocuments.size() > 3) {
            throw new BadRequestException("A maximum of 3 documents is allowed per month");
        }

        bankStatement = bankStatement.toBuilder()
                .documentUrls(existingDocuments)
                .month(dto.getMonth() != null ? dto.getMonth() : bankStatement.getMonth())
                .build();
        this.bankStatementRepository.save(bankStatement);

        return BaseResponseDto.<BankStatement>builder()
                .status("Success")
                .message("Successfully updated bank statement")
                .data(bankStatement)
                .build();
    }

    public BaseResponseDto<BankStatement> getBankStatement(
            UUID id
    ) {
        User user = this.userService.getAuthenticatedUser();

        BankStatement bankStatement = this.bankStatementRepository.findByIdAndUserIdAndDeletedAtIsNull(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Failed to retrieve bank statement"));

        return BaseResponseDto.<BankStatement>builder()
                .status("Success")
                .message("Successfully retrieved bank statement")
                .data(bankStatement)
                .build();
    }

    public BaseResponseDto<PagedBankStatementResponseDto> getAllStatements(
            int page, int limit
    ) {
        User user = this.userService.getAuthenticatedUser();

        Page<BankStatement> statements = this.bankStatementRepository
                .findAllByUserIdAndDeletedAtIsNull(user.getId(), PageRequest.of(page - 1, limit));

        return BaseResponseDto.<PagedBankStatementResponseDto>builder()
                .status("Success")
                .message("Successfully retrieved all statements")
                .data(new PagedBankStatementResponseDto(statements))
                .build();
    }

    public BaseResponseDto<Object> deleteBankStatement(UUID bankStatementId) {
        User user = this.userService.getAuthenticatedUser();

        BankStatement bankStatement = this.bankStatementRepository
                .findByIdAndUserIdAndDeletedAtIsNull(bankStatementId, user.getId())
                .orElseThrow(() -> new NotFoundException("Statement not found"));

        if (bankStatement.getStatus() == StatementAnalysisStatus.IN_PROGRESS) {
            throw new BadRequestException("Statement analysis is currently in progress, wait for completion");
        }

        for (DocumentUrls doc:bankStatement.getDocumentUrls()) {
            this.s3UploadService.deleteFromS3(doc.getUrl());
        }

        this.bankStatementRepository.deleteById(bankStatementId);

        return BaseResponseDto.builder()
                .status("Success")
                .message("Successfully deleted statement")
                .build();
    }

    public BaseResponseDto<BankStatement> startAnalysis(UUID bankStatementId) {
        User user = this.userService.getAuthenticatedUser();

        BankStatement bankStatement = this.bankStatementRepository
                .findByIdAndUserIdAndDeletedAtIsNull(bankStatementId, user.getId())
                .orElseThrow(() -> new NotFoundException("Statement not found"));

        if (
                bankStatement.getStatus() == StatementAnalysisStatus.IN_PROGRESS
                        || bankStatement.getStatus() == StatementAnalysisStatus.ANALYSED
        ) {
            String message = bankStatement.getStatus() == StatementAnalysisStatus.ANALYSED
                    ? "Analysis for this statement record is already completed"
                    : "Analysis is already in progress. Please wait for the notification.";

            return BaseResponseDto.<BankStatement>builder()
                    .status("Success")
                    .message(message)
                    .data(bankStatement)
                    .build();
        }

        bankStatement.setStatus(StatementAnalysisStatus.IN_PROGRESS);
        this.bankStatementRepository.save(bankStatement);

        this.jobScheduler.enqueue(() -> this.analyseBankStatementAndNotify(user.getId(), bankStatementId));

        return BaseResponseDto.<BankStatement>builder()
                .status("Success")
                .message("Successfully added statement processing job to queue. You will be notified on completion")
                .data(bankStatement)
                .build();
    }

    @Job(name = "Analyse bank statement and notify")
    public void analyseBankStatementAndNotify(UUID userId, UUID bankStatementId) {
        User user = this.userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Failed to retrieve user details"));

        BankStatement bankStatement = this.bankStatementRepository
                .findByIdAndUserIdAndDeletedAtIsNull(bankStatementId, user.getId())
                .orElseThrow(() -> new NotFoundException("Statement not found"));

        try {
            String promptText = """
                    I have attached a maximum of 3 bank statement files to this prompt that might be \
                    in different document formats (pdf, csv and excel). They are statements for a user over a given month \
                    across the different banks the user uses. Extract all transactions from these statements,
                    making sure to take note of inter-bank transfers and not consider them. Only consider transactions \
                    that are going out or coming into the users accounts but not transactions between them.
                    That means personal transfers should not be recorded.
                    Make sure to return ONLY a raw list of JSON with the following fields for each transaction:
                     - datetime (transaction datetime in yyyy-MM-dd'T'HH:mm:ss format)
                     - amount
                     - category (for debits, it should be an enum of the following: food, clothing, housing,
                        entertainment, utilities, transportation, healthcare, grooming, tax, education, gifting,
                        miscellaneous, bill, personal, shopping.
                        While for credits, this will be the source of income, you can decide to use any reasonable short \
                        text of choice obtained from the transaction details. You can generally use 'salary', 'gifting', etc).
                        Try your best possible to determine the category from all details of the transaction,
                        If you cannot determine it, mark it as miscellaneous. For money lent out to someone or given to \
                        someone, it should be categorized as bill. If money is to someone's account and the person refunds,
                        do not record it since it cancels out.
                     - note (this is the transaction description).
                     - direction (enum of 'credit' or 'debit')
                    For the tax or interest category, accumulate all transaction charges, tax or interest and \
                    ONLY return one record indicating the sum.
                    When there is any description with 'OWealth', do not consider the transaction unless it is an interest.
                    Do NOT include markdowns, backticks or explanations as the response will be parsed with jackson.
                    """;

            GeminiRequest geminiRequest = this.buildGeminiRequest(promptText, bankStatement);
            GeminiResponse geminiResponse = this.geminiClientProxy.analyzeStatement(
                    this.geminiApiKey,
                    geminiRequest
            );

            String rawJson = geminiResponse.getCandidates().get(0).getContent().getParts().get(0).getText();
            String cleanJson = rawJson.replaceAll("```json|```", "").trim();

            List<GeminiTransactionResponseDto> transactions = this.deserializeGeminiResponseToDto(
                    cleanJson
            );

            this.createTransactionRecordFromGeminiTransaction(transactions, user);

            bankStatement.setStatus(StatementAnalysisStatus.ANALYSED);
            this.bankStatementRepository.save(bankStatement);

            this.emailService.sendStatementEmailAsync(
                    NotifyStatementProcessingResultDto.builder()
                            .name(user.getName())
                            .recipientEmail(user.getEmail())
                            .purpose(EmailPurpose.statement_processing_success)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to process and analyse statements", e);

            bankStatement.setStatus(StatementAnalysisStatus.PENDING);
            this.bankStatementRepository.save(bankStatement);

            this.emailService.sendStatementEmailAsync(
                    NotifyStatementProcessingResultDto.builder()
                            .name(user.getName())
                            .recipientEmail(user.getEmail())
                            .purpose(EmailPurpose.statement_processing_failure)
                            .errorMessage("Bank statement processing failed")
                            .build()
            );
        }
    }

    private GeminiRequest buildGeminiRequest(String promptText, BankStatement bankStatement) {
        GeminiRequest.Part textPart = new GeminiRequest.Part();
        textPart.setText(promptText);

        List<GeminiRequest.Part> fileParts = bankStatement.getDocumentUrls()
                .stream().map(doc -> {
                    GeminiRequest.Part filePart = new GeminiRequest.Part();

                    GeminiRequest.FileData fileData = new GeminiRequest.FileData();
                    fileData.setFileUri(doc.getUrl());
                    fileData.setMimeType(doc.getMimeType());

                    filePart.setFileData(fileData);

                    return filePart;
                })
                .toList();
        List<GeminiRequest.Part> parts = new ArrayList<>(fileParts);
        parts.add(textPart);

        GeminiRequest.Content content = new GeminiRequest.Content();
        content.setParts(parts);
        content.setRole("user");

        GeminiRequest request = new GeminiRequest();
        request.setContents(List.of(content));

        return request;
    }

    public void createTransactionRecordFromGeminiTransaction(
            List<GeminiTransactionResponseDto> geminiTransactions,
            User user
    ) {

        for (GeminiTransactionResponseDto t : geminiTransactions) {
            this.validateTransaction(t);

            if (t.getDirection() == TransactionDirection.credit) {
                Income income = Income.builder()
                        .source(t.getCategory())
                        .amount(t.getAmount())
                        .month(YearMonth.from(t.getDatetime()))
                        .transactionDateTime(t.getDatetime())
                        .note(t.getNote())
                        .user(user)
                        .isRecurring(false)
                        .build();
                this.incomeRepository.save(income);
            } else {
                Expense expense = Expense.builder()
                        .month(YearMonth.from(t.getDatetime()))
                        .amount(t.getAmount())
                        .category(this.parseCategory(t.getCategory()))
                        .transactionDateTime(t.getDatetime())
                        .isRecurring(false)
                        .note(t.getNote())
                        .user(user)
                        .build();
                this.expenseRepository.save(expense);
            }

            Transactions transaction = Transactions.builder()
                    .user(user)
                    .amount(t.getAmount())
                    .description(t.getNote())
                    .direction(t.getDirection())
                    .transactionDateTime(t.getDatetime())
                    .month(YearMonth.from(t.getDatetime()))
                    .build();
            this.transactionRepository.save(transaction);
        }
    }

    private List<GeminiTransactionResponseDto> deserializeGeminiResponseToDto(
            String geminiResponse
    ) throws JsonProcessingException {

        return this.objectMapper.readValue(
                geminiResponse,
                new TypeReference<>() {
                }
        );
    }

    private void validateTransaction(GeminiTransactionResponseDto t) {
        if (t.getAmount() == null || t.getDatetime() == null || t.getDirection() == null) {
            throw new IllegalArgumentException("Invalid transaction from AI");
        }
    }

    private ExpenseCategory parseCategory(String category) {
        try {
            return ExpenseCategory.valueOf(category.trim().toLowerCase());
        } catch (Exception e) {
            return ExpenseCategory.others;
        }
    }
}