package io.ldxinsight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/datasets")
@RequiredArgsConstructor
@Tag(name = "1. Dataset APIs", description = "APIs quản lý và tìm kiếm Bộ dữ liệu")
public class DatasetController {

    private final DatasetService datasetService;
    // Dùng ObjectMapper do Spring Boot auto-config cung cấp (được inject qua constructor)
    private final ObjectMapper objectMapper;

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

    @Operation(summary = "Lấy download URL (JSON) và ghi nhận 1 lượt tải (tăng download count)")
    @GetMapping("/{id}/download")
    public ResponseEntity<Map<String, String>> downloadAndIncrement(@PathVariable String id) {
        // Service trả về PATH JSON nội bộ, vd: /api/v1/datasets/{id}/download.json
        String jsonPath = datasetService.getDownloadUrlAndIncrement(id);

        // Build absolute URL KHÔNG phụ thuộc HttpServletRequest
        String absoluteJsonUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path(jsonPath)
                .build()
                .toUriString();

        return ResponseEntity.ok(Map.of("downloadUrl", absoluteJsonUrl));
    }

    @Operation(summary = "Tải dữ liệu dưới dạng CSV (nếu là JSON) hoặc file gốc (HTML/Text/JSON)")
    @GetMapping(value = {"/{id}/download.csv", "/{id}/csv"})
    public ResponseEntity<Resource> downloadCsv(
            @Parameter(description = "ID của dataset")
            @PathVariable("id") String id) {
        log.info("CSV download requested for dataset: {} (path: /{}/download.csv or /{}/csv)", id, id, id);
        
        // Kiểm tra dataset có tồn tại không - chỉ throw exception nếu dataset không tồn tại
        DatasetDto dto;
        try {
            dto = datasetService.getDatasetById(id);
            log.debug("Dataset found: {}", dto.getTitle());
        } catch (ResourceNotFoundException e) {
            log.error("Dataset not found: {}", id);
            throw e;
        }
        
        // Wrap toàn bộ logic trong try-catch để fallback về metadata JSON nếu có lỗi
        try {
            String sourceUrl;
            try {
                sourceUrl = datasetService.getDataUrl(id);
                log.debug("Data URL retrieved: {}", sourceUrl);
            } catch (ResourceNotFoundException e) {
                log.warn("No data source found for dataset {}: {}. Falling back to metadata JSON.", id, e.getMessage());
                return returnDatasetMetadataAsJson(dto, id);
            }
            
            if (!StringUtils.hasText(sourceUrl)) {
                log.warn("Empty data URL for dataset: {}. Falling back to metadata JSON.", id);
                return returnDatasetMetadataAsJson(dto, id);
            }

            // Lấy dữ liệu
            byte[] dataBytes;
            try {
                dataBytes = fetchDataBytes(sourceUrl);
                log.debug("Fetched {} bytes from data source", dataBytes.length);
            } catch (Exception e) {
                log.warn("Failed to fetch data from URL {}: {}. Falling back to metadata JSON.", sourceUrl, e.getMessage());
                return returnDatasetMetadataAsJson(dto, id);
            }
            
            if (dataBytes == null || dataBytes.length == 0) {
                log.warn("Empty data received for dataset: {}. Falling back to metadata JSON.", id);
                return returnDatasetMetadataAsJson(dto, id);
            }
            
            // Kiểm tra xem có phải WAF/Proxy block message không
            String contentStr = new String(dataBytes, 0, Math.min(dataBytes.length, 500), StandardCharsets.UTF_8);
            if (contentStr.contains("The requested URL was rejected") || 
                contentStr.contains("Please consult with your administrator") ||
                contentStr.contains("support ID")) {
                log.warn("Data source was blocked by WAF/Proxy. Falling back to metadata JSON.");
                return returnDatasetMetadataAsJson(dto, id);
            }
            
            // Thử parse JSON và convert sang CSV
            com.fasterxml.jackson.databind.JsonNode jsonNode = null;
            try {
                jsonNode = objectMapper.readTree(dataBytes);
                log.debug("Successfully parsed JSON");
                
                // Convert sang CSV
                String csvContent = convertJsonToCsv(jsonNode);
                if (csvContent != null && !csvContent.trim().isEmpty()) {
                    String baseName = StringUtils.hasText(dto.getTitle()) ? dto.getTitle().trim() : ("dataset-" + id);
                    String safeName = sanitizeForFilename(baseName) + ".csv";
                    byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);
                    
                    log.info("Returning CSV file: {} ({} bytes)", safeName, csvBytes.length);
                    ByteArrayResource body = new ByteArrayResource(csvBytes);
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                            .contentLength(csvBytes.length)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    ContentDisposition.attachment().filename(safeName, StandardCharsets.UTF_8).build().toString())
                            .cacheControl(CacheControl.noCache())
                            .body(body);
                }
            } catch (Exception e) {
                log.debug("Cannot parse as JSON or convert to CSV: {}. Will return original content.", e.getMessage());
            }
            
            // Nếu không parse được JSON hoặc convert không được, trả về file gốc
            String contentPreview = new String(dataBytes, 0, Math.min(dataBytes.length, 200), StandardCharsets.UTF_8).toLowerCase();
            String extension = ".txt";
            String contentType = MediaType.TEXT_PLAIN_VALUE;
            
            if (contentPreview.contains("<html") || contentPreview.contains("<!doctype")) {
                extension = ".html";
                contentType = MediaType.TEXT_HTML_VALUE;
                log.info("Content is HTML, returning as HTML file");
            } else if (contentPreview.trim().startsWith("{") || contentPreview.trim().startsWith("[")) {
                extension = ".json";
                contentType = MediaType.APPLICATION_JSON_VALUE;
                log.info("Content looks like JSON but couldn't parse, returning as JSON file");
            } else {
                log.info("Content is not JSON/HTML, returning as text file");
            }
            
            String baseName = StringUtils.hasText(dto.getTitle()) ? dto.getTitle().trim() : ("dataset-" + id);
            String safeName = sanitizeForFilename(baseName) + extension;
            
            log.info("Returning original content as file: {} ({} bytes, type: {})", safeName, dataBytes.length, contentType);
            ByteArrayResource body = new ByteArrayResource(dataBytes);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType + "; charset=utf-8"))
                    .contentLength(dataBytes.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(safeName, StandardCharsets.UTF_8).build().toString())
                    .cacheControl(CacheControl.noCache())
                    .body(body);
                    
        } catch (Exception e) {
            // Fallback cuối cùng: trả về metadata JSON nếu có bất kỳ lỗi nào
            log.error("Unexpected error processing dataset {}: {}. Falling back to metadata JSON.", id, e.getMessage(), e);
            return returnDatasetMetadataAsJson(dto, id);
        }
    }
    
    /**
     * Trả về metadata của dataset dưới dạng JSON file
     */
    private ResponseEntity<Resource> returnDatasetMetadataAsJson(DatasetDto dto, String id) {
        try {
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
            byte[] jsonBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
            String baseName = StringUtils.hasText(dto.getTitle()) ? dto.getTitle().trim() : ("dataset-" + id);
            String safeName = sanitizeForFilename(baseName) + ".json";
            
            log.info("Returning dataset metadata as JSON file: {} ({} bytes)", safeName, jsonBytes.length);
            ByteArrayResource body = new ByteArrayResource(jsonBytes);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(jsonBytes.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(safeName, StandardCharsets.UTF_8).build().toString())
                    .cacheControl(CacheControl.noCache())
                    .body(body);
        } catch (Exception e) {
            log.error("Error creating metadata JSON: {}", e.getMessage(), e);
            // Nếu không thể tạo JSON, trả về error message đơn giản
            String errorMsg = "{\"error\":\"Unable to process dataset\",\"id\":\"" + id + "\"}";
            byte[] errorBytes = errorMsg.getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(errorBytes.length)
                    .body(new ByteArrayResource(errorBytes));
        }
    }

    @Operation(summary = "Tải dữ liệu dưới dạng file (JSON/Text)")
    @GetMapping(value = "/{id}/download.json", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<Resource> streamJson(@PathVariable String id) {
        // Kiểm tra dataset có tồn tại không trước
        DatasetDto dto = datasetService.getDatasetById(id);
        
        String sourceUrl;
        try {
            sourceUrl = datasetService.getDataUrl(id);
        } catch (ResourceNotFoundException e) {
            // Nếu dataset tồn tại nhưng không có dataUrl/file, trả về lỗi rõ ràng hơn
            throw new ResourceNotFoundException("Dataset exists but no data source found: " + e.getMessage());
        }
        
        if (!StringUtils.hasText(sourceUrl)) {
            throw new ResourceNotFoundException("Dataset does not have a download URL");
        }

        byte[] bytes = fetchDataBytes(sourceUrl); // Lấy dữ liệu, không kiểm tra format strict

        // Thử parse JSON, nếu thành công thì format lại, nếu không thì giữ nguyên
        try {
            Object tree = objectMapper.readTree(bytes);
            bytes = objectMapper.writeValueAsBytes(tree);
        } catch (Exception e) {
            // Không phải JSON hợp lệ, giữ nguyên dữ liệu gốc (có thể là HTML, text, etc.)
            log.debug("Data is not valid JSON, returning as-is: {}", e.getMessage());
        }

        String baseName = StringUtils.hasText(dto.getTitle()) ? dto.getTitle().trim() : ("dataset-" + id);
        String safeName = sanitizeForFilename(baseName) + ".txt";

        ByteArrayResource body = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(safeName, StandardCharsets.UTF_8).build().toString())
                .cacheControl(CacheControl.noCache())
                .body(body);
    }

    /* ---------------- Helpers ---------------- */

    private byte[] fetchDataBytes(String sourceUrl) {
        try {
            if (isHttp(sourceUrl)) {
                RestTemplate rt = new RestTemplate();
                HttpHeaders h = new HttpHeaders();
                h.set(HttpHeaders.USER_AGENT, "Ldx-Insight/1.0 (+spring)");
                h.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML, MediaType.ALL));

                ResponseEntity<byte[]> up = rt.exchange(URI.create(sourceUrl), HttpMethod.GET, new HttpEntity<>(h), byte[].class);
                if (!up.getStatusCode().is2xxSuccessful() || up.getBody() == null) {
                    throw new ResourceNotFoundException("Upstream error: " + up.getStatusCode());
                }

                byte[] body = up.getBody();
                if (body.length == 0) {
                    throw new ResourceNotFoundException("Upstream content is empty");
                }
                
                // Log warning nếu là HTML nhưng vẫn trả về
                String head = new String(body, 0, Math.min(body.length, 400), StandardCharsets.UTF_8).toLowerCase();
                if (head.contains("<html") || head.contains("<!doctype html")) {
                    log.warn("Upstream returned HTML content (may be blocked by WAF/Proxy), but returning as-is. URL: {}", sourceUrl);
                }
                
                return body;
            } else {
                // Local file / file://
                byte[] body = readLocal(sourceUrl);
                if (body.length == 0) {
                    throw new ResourceNotFoundException("Local file is empty");
                }
                return body;
            }
        } catch (ResourceNotFoundException e) {
            // Re-throw ResourceNotFoundException để giữ nguyên message
            throw e;
        } catch (Exception e) {
            // Log chi tiết lỗi để debug
            log.error("Error fetching data content from dataUrl: {}", sourceUrl, e);
            String errorMsg = String.format("Cannot fetch data content from dataUrl: %s. Source URL: %s. Error: %s", 
                    e.getClass().getSimpleName(), sourceUrl, e.getMessage());
            throw new ResourceNotFoundException(errorMsg);
        }
    }

    private boolean isHttp(String url) {
        String u = url.trim().toLowerCase();
        return u.startsWith("http://") || u.startsWith("https://");
    }

    private byte[] readLocal(String url) throws Exception {
        try {
            if (url.startsWith("file:")) {
                URI uri = new URI(url);
                Path path;
                try {
                    // Thử parse URI trực tiếp
                    path = Path.of(uri);
                } catch (Exception e) {
                    // Nếu không được, thử lấy path từ URI
                    String pathStr = uri.getPath();
                    // Xử lý Windows path (file:///C:/path -> C:/path)
                    if (pathStr != null && pathStr.startsWith("/") && pathStr.length() > 2 && pathStr.charAt(2) == ':') {
                        pathStr = pathStr.substring(1); // Bỏ dấu / đầu tiên
                    }
                    path = Paths.get(pathStr);
                }
                if (!Files.exists(path) || !Files.isReadable(path)) {
                    throw new ResourceNotFoundException("Local file not found or not readable: " + url + " (resolved to: " + path + ")");
                }
                return Files.readAllBytes(path);
            }
        } catch (URISyntaxException e) {
            // fallthrough: thử coi như đường dẫn hệ điều hành
        } catch (ResourceNotFoundException e) {
            throw e; // Re-throw ResourceNotFoundException
        }
        // Xử lý như đường dẫn hệ điều hành thông thường
        Path p = Paths.get(url);
        if (!Files.exists(p) || !Files.isReadable(p)) {
            throw new ResourceNotFoundException("Local file not found or not readable: " + url + " (absolute path: " + p.toAbsolutePath() + ")");
        }
        return Files.readAllBytes(p);
    }

    private String sanitizeForFilename(String input) {
        return input.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * Chuyển đổi JSON sang CSV format
     * Hỗ trợ:
     * - JSON array of objects: mỗi object là một row, keys là headers
     * - JSON object: single row với keys là headers
     * - Nested objects: được flatten hoặc stringify
     */
    private String convertJsonToCsv(com.fasterxml.jackson.databind.JsonNode jsonNode) {
        StringBuilder csv = new StringBuilder();
        
        if (jsonNode.isArray()) {
            // JSON array: mỗi element là một row
            java.util.List<String> headers = new java.util.ArrayList<>();
            java.util.List<java.util.Map<String, String>> rows = new java.util.ArrayList<>();
            
            // Thu thập tất cả keys từ tất cả objects để tạo headers
            for (com.fasterxml.jackson.databind.JsonNode item : jsonNode) {
                if (item.isObject()) {
                    java.util.Map<String, String> row = new java.util.LinkedHashMap<>();
                    item.fields().forEachRemaining(entry -> {
                        String key = entry.getKey();
                        String value = formatJsonValue(entry.getValue());
                        if (!headers.contains(key)) {
                            headers.add(key);
                        }
                        row.put(key, value);
                    });
                    rows.add(row);
                }
            }
            
            // Viết header
            if (!headers.isEmpty()) {
                csv.append(String.join(",", escapeCsvValues(headers)));
                csv.append("\n");
            }
            
            // Viết rows
            for (java.util.Map<String, String> row : rows) {
                java.util.List<String> values = new java.util.ArrayList<>();
                for (String header : headers) {
                    values.add(row.getOrDefault(header, ""));
                }
                csv.append(String.join(",", escapeCsvValues(values)));
                csv.append("\n");
            }
            
        } else if (jsonNode.isObject()) {
            // JSON object: single row
            java.util.List<String> headers = new java.util.ArrayList<>();
            java.util.List<String> values = new java.util.ArrayList<>();
            
            jsonNode.fields().forEachRemaining(entry -> {
                headers.add(entry.getKey());
                values.add(formatJsonValue(entry.getValue()));
            });
            
            // Viết header
            csv.append(String.join(",", escapeCsvValues(headers)));
            csv.append("\n");
            // Viết values
            csv.append(String.join(",", escapeCsvValues(values)));
            csv.append("\n");
            
        } else {
            // Primitive value: single cell
            csv.append("value\n");
            csv.append(escapeCsvValue(formatJsonValue(jsonNode)));
            csv.append("\n");
        }
        
        return csv.toString();
    }

    /**
     * Format giá trị JSON thành string cho CSV
     */
    private String formatJsonValue(com.fasterxml.jackson.databind.JsonNode node) {
        if (node.isNull()) {
            return "";
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isNumber()) {
            return node.asText();
        } else if (node.isBoolean()) {
            return String.valueOf(node.asBoolean());
        } else if (node.isArray() || node.isObject()) {
            // Nested objects/arrays: convert to JSON string
            try {
                return objectMapper.writeValueAsString(node);
            } catch (Exception e) {
                return node.toString();
            }
        } else {
            return node.asText();
        }
    }

    /**
     * Escape và quote các giá trị CSV
     */
    private java.util.List<String> escapeCsvValues(java.util.List<String> values) {
        return values.stream()
                .map(this::escapeCsvValue)
                .collect(java.util.stream.Collectors.toList());
    }

    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        // Nếu chứa comma, newline, hoặc quote thì cần quote và escape
        if (value.contains(",") || value.contains("\n") || value.contains("\"") || value.contains("\r")) {
            // Escape quotes bằng cách double them
            String escaped = value.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }
        return value;
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
