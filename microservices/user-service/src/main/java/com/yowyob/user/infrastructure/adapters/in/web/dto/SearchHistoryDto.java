package com.yowyob.user.infrastructure.adapters.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistoryDto {
    private String query;
}
