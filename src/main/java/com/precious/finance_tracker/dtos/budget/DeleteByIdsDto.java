package com.precious.finance_tracker.dtos.budget;

import java.util.List;
import java.util.UUID;

public record DeleteByIdsDto(List<UUID> ids) {
}
