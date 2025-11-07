package io.ldxinsight.controller;

import io.ldxinsight.dto.CreateDatasetRequest;
import io.ldxinsight.dto.DatasetDto;
import io.ldxinsight.service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/datasets")
@RequiredArgsConstructor
@Tag(name = "1. Dataset APIs", description = "APIs quản lý và tìm kiếm Bộ dữ liệu")
public class DatasetController {

    private final DatasetService datasetService;

    @Operation(summary = "Tìm kiếm (search) HOẶC lọc (filter) dataset")
    @GetMapping
    public ResponseEntity<Page<DatasetDto>> searchDatasets(
            @Parameter(description = "Từ khóa tìm kiếm (trong tiêu đề, mô tả)")
            @RequestParam(required = false) String q,
            @Parameter(description = "Lọc theo danh mục")
            @RequestParam(required = false) String category,
            @Parameter(description = "Phân trang (ví dụ: ?page=0&size=10&sort=viewCount,desc)")
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        Page<DatasetDto> results = datasetService.searchDatasets(q, category, pageable);
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Lấy chi tiết một Bộ dữ liệu bằng ID")
    @GetMapping("/{id}")
    public ResponseEntity<DatasetDto> getDatasetById(@PathVariable String id) {
        return ResponseEntity.ok(datasetService.getDatasetById(id));
    }

    @Operation(summary = "Lấy danh sách dataset CHỈ theo category (API riêng)")
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<DatasetDto>> getDatasetsByCategory(
            @Parameter(description = "Tên category, ví dụ: 'Y tế'")
            @PathVariable String category,
            @Parameter(description = "Phân trang (ví dụ: ?page=0&size=10)")
            Pageable pageable) {
        
        Page<DatasetDto> results = datasetService.getDatasetsByCategory(category, pageable);
        return ResponseEntity.ok(results);
    }
    
    @Operation(summary = "Ghi nhận 1 lượt xem (tăng view count)")
    @PostMapping("/{id}/view") 
    public ResponseEntity<Void> incrementView(@PathVariable String id) {
        datasetService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Tải file và ghi nhận 1 lượt tải (tăng download count)")
    @GetMapping("/{id}/download")
    public ResponseEntity<Void> downloadAndIncrement(@PathVariable String id) {
        String downloadUrl = datasetService.getDownloadUrlAndIncrement(id);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(downloadUrl))
                .build();
    }


    @Operation(summary = "Tạo một Bộ dữ liệu mới")
    @ApiResponse(responseCode = "201", description = "Tạo thành công")
    @PostMapping
    public ResponseEntity<DatasetDto> createDataset(@Valid @RequestBody CreateDatasetRequest request) {
        DatasetDto createdDataset = datasetService.createDataset(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDataset);
    }

    @Operation(summary = "Cập nhật một Bộ dữ liệu")
    @PutMapping("/{id}")
    public ResponseEntity<DatasetDto> updateDataset(
            @PathVariable String id,
            @Valid @RequestBody CreateDatasetRequest request) {
        DatasetDto updatedDataset = datasetService.updateDataset(id, request);
        return ResponseEntity.ok(updatedDataset);
    }

    @Operation(summary = "Xóa một Bộ dữ liệu")
    @ApiResponse(responseCode = "204", description = "Xóa thành công")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDataset(@PathVariable String id) {
        datasetService.deleteDataset(id);
        return ResponseEntity.noContent().build();
    }
}