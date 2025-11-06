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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/datasets")
@RequiredArgsConstructor
@Tag(name = "1. Dataset APIs", description = "APIs quản lý và tìm kiếm Bộ dữ liệu")
public class DatasetController {

    private final DatasetService datasetService;

    @Operation(summary = "Tìm kiếm, lọc và phân trang Bộ dữ liệu",
            description = "API này công khai, không cần xác thực.",
            security = @SecurityRequirement(name = "bearerAuth", scopes = {}))
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

    @Operation(summary = "Lấy chi tiết một Bộ dữ liệu",
            description = "API này công khai, không cần xác thực.",
            security = @SecurityRequirement(name = "bearerAuth", scopes = {}))
    @GetMapping("/{id}")
    public ResponseEntity<DatasetDto> getDatasetById(@PathVariable String id) {
        return ResponseEntity.ok(datasetService.getDatasetById(id));
    }

    @Operation(summary = "Tạo một Bộ dữ liệu mới (Yêu cầu xác thực)")
    @ApiResponse(responseCode = "201", description = "Tạo thành công")
    @PostMapping
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DatasetDto> createDataset(@Valid @RequestBody CreateDatasetRequest request) {
        DatasetDto createdDataset = datasetService.createDataset(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDataset);
    }

    @Operation(summary = "Cập nhật một Bộ dữ liệu (Yêu cầu xác thực)")
    @PutMapping("/{id}")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DatasetDto> updateDataset(
            @PathVariable String id,
            @Valid @RequestBody CreateDatasetRequest request) {
        DatasetDto updatedDataset = datasetService.updateDataset(id, request);
        return ResponseEntity.ok(updatedDataset);
    }

    @Operation(summary = "Xóa một Bộ dữ liệu (Yêu cầu xác thực)")
    @ApiResponse(responseCode = "204", description = "Xóa thành công")
    @DeleteMapping("/{id}")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDataset(@PathVariable String id) {
        datasetService.deleteDataset(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ghi nhận 1 lượt xem (tăng view count)",
            description = "API này công khai, không cần xác thực.",
            security = @SecurityRequirement(name = "bearerAuth", scopes = {}))
    @PostMapping("/{id}/view")
    public ResponseEntity<Void> incrementView(@PathVariable String id) {
        datasetService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Tải file và ghi nhận 1 lượt tải (tăng download count)",
            description = "API này công khai, không cần xác thực.",
            security = @SecurityRequirement(name = "bearerAuth", scopes = {}))
    @ApiResponse(responseCode = "302", description = "Chuyển hướng đến link tải file")
    @GetMapping("/{id}/download")
    public ResponseEntity<Void> downloadAndIncrement(@PathVariable String id) {
        String downloadUrl = datasetService.getDownloadUrlAndIncrement(id);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(downloadUrl))
                .build();
    }

    @Operation(summary = "Lấy danh sách tất cả các 'thư mục' (categories) duy nhất",
            description = "API này công khai, không cần xác thực.",
            security = @SecurityRequirement(name = "bearerAuth", scopes = {}))
    @GetMapping("/categories")
//    @PreAuthorize("permitAll()") // Cho phép nếu dùng @EnableMethodSecurity
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(datasetService.getAllCategories());
    }

    @Operation(summary = "Lấy danh sách dataset CHỈ theo category (API riêng)",
            description = "API này công khai, không cần xác thực.",
            security = @SecurityRequirement(name = "bearerAuth", scopes = {}))
    @GetMapping("/category/{category}") // <-- ĐƯỜNG DẪN MỚI
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<DatasetDto>> getDatasetsByCategory(
            @Parameter(description = "Tên category, ví dụ: 'Y tế'")
            @PathVariable String category,
            @Parameter(description = "Phân trang (ví dụ: ?page=0&size=10)")
            Pageable pageable) {

        Page<DatasetDto> results = datasetService.getDatasetsByCategory(category, pageable);
        return ResponseEntity.ok(results);
    }
}