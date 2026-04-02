package com.precious.finance_tracker.dtos.budget;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class DeleteByIdsDto {
    private final List<UUID> ids;

    public DeleteByIdsDto(List<UUID> ids) {
        this.ids = ids;
    }
}
