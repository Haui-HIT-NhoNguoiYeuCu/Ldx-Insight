package io.ldxinsight.controller;

import io.ldxinsight.dto.StatSummaryDto;
import io.ldxinsight.service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}