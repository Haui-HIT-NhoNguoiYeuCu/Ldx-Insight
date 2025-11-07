package io.ldxinsight.controller;

import io.ldxinsight.dto.CreateDatasetRequest;
import io.ldxinsight.dto.DatasetDto;
import io.ldxinsight.exception.ResourceNotFoundException;
import io.ldxinsight.service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Operation(summary = "Lấy tất cả các category từ cơ sở dữ liệu")
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(datasetService.getAllCategories());
    }

    @Operation(summary = "Lấy danh sách dataset CHỈ theo category (API riêng)")
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<DatasetDto>> getDatasetsByCategory(
            @Parameter(description = "Tên category, ví dụ: 'Y tế'")
            @PathVariable String category,
            @Parameter(description = "Phân trang (ví dụ: ?page=0&size=10)")
            Pageable pageable) {
        return ResponseEntity.ok(datasetService.getDatasetsByCategory(category, pageable));
    }

    @Operation(summary = "Ghi nhận 1 lượt xem (tăng view count)")
    @PostMapping("/{id}/view")
    public ResponseEntity<Void> incrementView(@PathVariable String id) {
        datasetService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Lấy download URL và ghi nhận 1 lượt tải (tăng download count)")
    @GetMapping("/{id}/download")
    public ResponseEntity<Map<String, String>> downloadAndIncrement(@PathVariable String id) {
        // Service trả về PATH CSV nội bộ, vd: /api/v1/datasets/{id}/download.csv
        String csvPath = datasetService.getDownloadUrlAndIncrement(id);

        // Build absolute URL KHÔNG phụ thuộc vào HttpServletRequest
        String absoluteCsvUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path(csvPath)
                .build()
                .toUriString();

        Map<String, String> response = new HashMap<>();
        response.put("downloadUrl", absoluteCsvUrl);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Tải dữ liệu dưới dạng CSV (stream)")
    @GetMapping(value = "/{id}/download.csv", produces = "text/csv")
    public ResponseEntity<Resource> streamCsv(@PathVariable String id) {
        // Lấy dataUrl từ service (tránh phụ thuộc DTO có/không có getter)
        String sourceUrl = datasetService.getDataUrl(id);
        if (!StringUtils.hasText(sourceUrl)) {
            throw new ResourceNotFoundException("Dataset does not have a download URL");
        }

        final byte[] bytes;
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<byte[]> upstream = restTemplate.getForEntity(URI.create(sourceUrl), byte[].class);
            if (!upstream.getStatusCode().is2xxSuccessful() || upstream.getBody() == null) {
                throw new ResourceNotFoundException("Cannot fetch CSV content from dataUrl");
            }
            bytes = upstream.getBody();
        } catch (RestClientException ex) {
            throw new ResourceNotFoundException("Cannot fetch CSV content from dataUrl");
        }

        // Tên file: ưu tiên title nếu có
        DatasetDto dto = datasetService.getDatasetById(id);
        String baseName = StringUtils.hasText(dto.getTitle()) ? dto.getTitle().trim() : ("dataset-" + id);
        String safeName = sanitizeForFilename(baseName) + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv")); // đúng MIME CSV
        headers.setContentDisposition(ContentDisposition
                .attachment()
                .filename(safeName, StandardCharsets.UTF_8)
                .build());
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        Resource body = new InputStreamResource(new java.io.ByteArrayInputStream(bytes));
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    private String sanitizeForFilename(String input) {
        return input.replaceAll("[\\\\/:*?\"<>|]", "_");
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
        return ResponseEntity.ok(datasetService.updateDataset(id, request));
    }

    @Operation(summary = "Xóa một Bộ dữ liệu")
    @ApiResponse(responseCode = "204", description = "Xóa thành công")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDataset(@PathVariable String id) {
        datasetService.deleteDataset(id);
        return ResponseEntity.noContent().build();
    }
}
