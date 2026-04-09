package com.precious.finance_tracker.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.email.NotifyStatementProcessingResultDto;
import com.precious.finance_tracker.dtos.gemini.GeminiRequest;
import com.precious.finance_tracker.dtos.gemini.GeminiResponse;
import com.precious.finance_tracker.dtos.gemini.GeminiTransactionResponseDto;
import com.precious.finance_tracker.dtos.statement.*;
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
                        .findByUserIdAndMonth(user.getId(), dto.getMonth());

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
                this.bankStatementRepository.findByIdAndUserId(bankStatementId, user.getId())
                        .orElseThrow(() -> new NotFoundException("Failed to retrieve bank statement"));

        if (
                bankStatement.getStatus() == StatementAnalysisStatus.ANALYSED
                        || bankStatement.getStatus() == StatementAnalysisStatus.IN_PROGRESS
        ) {
            throw new BadRequestException("Update is not allowed on in-progress or analysed bank statements");
        }

        if (dto.getMonth() != null) {
            Optional<BankStatement> existingMonthStatement =
                    this.bankStatementRepository.findByUserIdAndMonth(user.getId(), dto.getMonth());

            if (existingMonthStatement.isPresent()) {
                throw new ConflictResourceException("You already have a bank statement record for " + dto.getMonth());
            }
        }

        List<DocumentUrls> existingDocuments = new ArrayList<>(bankStatement.getDocumentUrls());

        if (dto.getDeleteDocuments() != null && !dto.getDeleteDocuments().isEmpty()) {
            Set<DocumentUrls> docsToDelete = new HashSet<>(dto.getDeleteDocuments());

            for (DocumentUrls doc:docsToDelete) {
                if (existingDocuments.contains(doc)) {
                    this.s3UploadService.deleteFromS3(doc.getFileKey());
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

        BankStatement bankStatement = this.bankStatementRepository.findByIdAndUserId(id, user.getId())
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
                .findAllByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page - 1, limit));

        return BaseResponseDto.<PagedBankStatementResponseDto>builder()
                .status("Success")
                .message("Successfully retrieved all statements")
                .data(new PagedBankStatementResponseDto(statements))
                .build();
    }

    public BaseResponseDto<Object> deleteBankStatement(UUID bankStatementId) {
        User user = this.userService.getAuthenticatedUser();

        BankStatement bankStatement = this.bankStatementRepository
                .findByIdAndUserId(bankStatementId, user.getId())
                .orElseThrow(() -> new NotFoundException("Statement not found"));

        if (bankStatement.getStatus() == StatementAnalysisStatus.IN_PROGRESS) {
            throw new BadRequestException("Statement analysis is currently in progress, wait for completion");
        }

        for (DocumentUrls doc:bankStatement.getDocumentUrls()) {
            this.s3UploadService.deleteFromS3(doc.getFileKey());
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
                .findByIdAndUserId(bankStatementId, user.getId())
                .orElseThrow(() -> new NotFoundException("Statement not found"));

        if (bankStatement.getDocumentUrls().get(0).getFileKey() == null) {
            throw new BadRequestException("Statement must contain valid file key for analysis");
        }

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
                .findByIdAndUserId(bankStatementId, user.getId())
                .orElseThrow(() -> new NotFoundException("Statement not found"));

        log.info("Starting statement processing for user {}", user.getEmail());
        boolean hasError = false;

        List<GeminiTransactionResponseDto> allTransactions = new ArrayList<>();

        for (DocumentUrls doc : bankStatement.getDocumentUrls()) {
            try {
                String formattedText = getPrompt(bankStatement.getMonth());

                GeminiRequest geminiRequest = this.buildGeminiRequest(formattedText, doc);
                GeminiResponse geminiResponse = this.geminiClientProxy.analyzeStatement(
                        this.geminiApiKey,
                        geminiRequest
                );

                GeminiResponse.Candidate candidate = geminiResponse.getCandidates().get(0);

                if (!"STOP".equalsIgnoreCase(candidate.getFinishReason())) {
                    log.warn(
                            "Gemini did not finish correctly for doc: {}. Reason: {}",
                            doc.getFileKey(),
                            candidate.getFinishReason());
                }

                String rawJson = candidate.getContent().getParts().get(0).getText();
                String cleanJson = rawJson.replaceAll("```json|```", "").trim();

                List<GeminiTransactionResponseDto> transactions = this.deserializeGeminiResponseToDto(
                        cleanJson
                );
                allTransactions.addAll(transactions);
            } catch (Exception e) {
                log.error("Failed to process document: {}", doc.getFileKey(), e);
                hasError = true;
                break;
            }
        }

        if (hasError) {
            handleFailure(bankStatement, user);
        } else {
            handleSuccess(bankStatement, user, allTransactions);
        }
    }

    private void handleSuccess(BankStatement bankStatement, User user, List<GeminiTransactionResponseDto> allTransactions) {
        this.createTransactionRecordFromGeminiTransaction(allTransactions, user);

        bankStatement.setStatus(StatementAnalysisStatus.ANALYSED);
        this.bankStatementRepository.save(bankStatement);

        this.emailService.sendStatementEmailAsync(
                NotifyStatementProcessingResultDto.builder()
                        .name(user.getName())
                        .recipientEmail(user.getEmail())
                        .purpose(EmailPurpose.statement_processing_success)
                        .build()
        );
    }

    private void handleFailure(BankStatement bankStatement, User user) {
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

    private static String getPrompt(YearMonth month) {
        String promptText = """
            # ROLE
            Act as a high-precision financial data extractor. Output ONLY minified JSON.
            
            # TASK
            Extract transactions from the attached bank statement (PDF/CSV/Excel).
            ONLY extract transactions for the month %s. Ignore all other months.
            IF there are no transactions for the selected month, throw an error with the reason.
            
            # FILTERING RULES
            1. EXCLUDE Personal Transfers: Ignore transfers between the user's own accounts (same name/inter-bank).
            2. EXCLUDE Refunds: If a debit is followed by an identical credit refund, ignore both.
            3. OPAY SPECIFIC: Process 'Wallet Account' only. IGNORE 'Savings Account'. If an 'Owealth' entry \
               mirrors an immediate main transaction amount exactly, ignore the Owealth entry.
            4. AGGREGATION: Sum all transaction charges, taxes, interest, or bonuses. Return ONLY one record \
               for each aggregate type.
            
            # CATEGORIZATION
            - DEBITS: Use ONLY: [food, clothing, housing, entertainment, utilities, transportation, healthcare,
              grooming, tax, education, gifting, miscellaneous, bill, personal, shopping].
            - LENDING: Categorize as 'bill'.
            - CREDITS: Use short descriptive text (e.g., 'salary', 'gifting', 'refund').
            - FALLBACK: Use 'miscellaneous' if uncertain, for both credit or debit.
            
            # OUTPUT FORMAT
            Return ONLY a minified JSON array. NO whitespace, NO newlines, NO indentation, No markdowns,
            NO backticks, NO preamble.
            Use the exact short keys: "dt", "amt", "cat", "note", "dir".
            Example: [{"dt":"2026-04-09T10:00:00","amt":5000.0,"cat":"food","note":"Mama Put","dir":"debit"}]
            
            # DATA SCHEMA
            - dt: ISO8601 (yyyy-MM-dd'T'HH:mm:ss)
            - amt: numeric
            - cat: string (enum for debits)
            - note: original description
            - dir: 'credit' or 'debit'
            """;

        return String.format(promptText, month);
    }

    private GeminiRequest buildGeminiRequest(String promptText, DocumentUrls doc) {
        GeminiRequest.Part textPart = new GeminiRequest.Part();
        textPart.setText(promptText);

        GeminiRequest.Part filePart = new GeminiRequest.Part();
        GeminiRequest.FileData fileData = new GeminiRequest.FileData();
        String url = this.s3UploadService.generatePresignedGetUrl(doc.getFileKey());
        fileData.setFileUri(url);
        fileData.setMimeType(doc.getMimeType());

        filePart.setFileData(fileData);

        GeminiRequest.Content content = new GeminiRequest.Content();
        content.setParts(List.of(filePart, textPart));
        content.setRole("user");

        GeminiRequest.GenerationConfig config = new GeminiRequest.GenerationConfig();
        config.setResponseMimeType("application/json");
        config.setMaxOutputTokens(8192);

        GeminiRequest request = new GeminiRequest();
        request.setContents(List.of(content));
        request.setGenerationConfig(config);

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
        log.debug(geminiResponse);
        JsonNode rootNode = objectMapper.readTree(geminiResponse);

        if (rootNode.isArray()) {
            return objectMapper.convertValue(rootNode, new TypeReference<>() {
            });
        }

        if (rootNode.isObject()) {
            Iterator<JsonNode> elements = rootNode.elements();
            while (elements.hasNext()) {
                JsonNode node = elements.next();
                if (node.isArray()) {
                    return objectMapper.convertValue(node, new TypeReference<>() {
                    });
                }
            }
        }

        throw MismatchedInputException.from(
                null,
                GeminiTransactionResponseDto.class,
                "Could not find a JSON array in Gemini response");
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