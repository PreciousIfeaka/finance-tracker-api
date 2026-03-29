package com.precious.finance_tracker.services.interfaces;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.dashboard.CurrentMonthSummaryDto;
import com.precious.finance_tracker.dtos.dashboard.WeeklySummaryDto;

import java.time.YearMonth;
import java.util.List;

public interface ISummaryService {
    BaseResponseDto<CurrentMonthSummaryDto> getCurrentMonthSummary(YearMonth month);
    BaseResponseDto<List<WeeklySummaryDto>> getWeeklyTotals(YearMonth month);
}
