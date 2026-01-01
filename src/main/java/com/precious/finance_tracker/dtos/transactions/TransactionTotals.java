package com.precious.finance_tracker.dtos.transactions;


import java.math.BigDecimal;

public interface TransactionTotals {
    BigDecimal getCredit();
    BigDecimal getDebit();
}
