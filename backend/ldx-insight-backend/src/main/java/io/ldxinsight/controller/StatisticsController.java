package io.ldxinsight.controller;

import io.ldxinsight.dto.CategoryStatisDTO;
import io.ldxinsight.dto.DatasetDto;
import io.ldxinsight.dto.StatSummaryDto;
import io.ldxinsight.service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Tag(name = "2. Statistics APIs", description = "APIs thống kê cho Dashboard")
public class StatisticsController {

    private final DatasetService datasetService;

    @Operation(summary = "Lấy thống kê tổng quan (tổng dataset, views, downloads)")
    @GetMapping("/summary")
    public ResponseEntity<StatSummaryDto> getSummary() {
        return ResponseEntity.ok(datasetService.getStatsSummary());
    }

    @Operation(summary = "Thống kê số lượng dataset theo từng danh mục")
    @GetMapping("/by-category")
    public ResponseEntity<List<CategoryStatisDTO>> getStatsByCategory() {
        return ResponseEntity.ok(datasetService.getCategoryStats());
    }

    @Operation(summary = "Lấy Top N dataset được XEM nhiều nhất")
    @GetMapping("/top-viewed")
    public ResponseEntity<List<DatasetDto>> getTopViewed(
            @Parameter(description = "Số lượng dataset muốn lấy, ví dụ: 5")
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(datasetService.getTopViewedDatasets(limit));
    }

    @Operation(summary = "Lấy Top N dataset được TẢI nhiều nhất")
    @GetMapping("/top-downloaded")
    public ResponseEntity<List<DatasetDto>> getTopDownloaded(
            @Parameter(description = "Số lượng dataset muốn lấy, ví dụ: 5")
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(datasetService.getTopDownloadedDatasets(limit));
    }
}