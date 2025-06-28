package com.precious.finance_tracker.dtos.user;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PagedUserResponseDto {
    private List<UserResponseDto> users;

    private final int page;

    private final int limit;

    private final Long total;

    public PagedUserResponseDto(
            Page<UserResponseDto> pagedData
    ) {
        this.users = pagedData.getContent();
        this.page = pagedData.getNumber() + 1;
        this.limit = pagedData.getSize();
        this.total = pagedData.getTotalElements();
    }
}
