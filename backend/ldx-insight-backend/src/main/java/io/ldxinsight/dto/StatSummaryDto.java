package io.ldxinsight.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatSummaryDto {
    private long totalDatasets;
    private long totalViews;
    private long totalDownloads;
}