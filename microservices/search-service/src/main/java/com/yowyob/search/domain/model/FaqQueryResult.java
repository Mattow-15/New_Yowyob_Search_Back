package com.yowyob.search.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqQueryResult {
    private List<FaqItem> questions;
    private boolean generated;
}
