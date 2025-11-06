package io.ldxinsight.dto;

import jakarta.validation.constraints.NotEmpty; // Thêm validation
import lombok.Data;
import java.util.List;

@Data
public class CreateDatasetRequest {
    @NotEmpty(message = "Tiêu đề không được để trống")
    private String title;
    private String description;
    @NotEmpty(message = "Nguồn không được để trống")
    private String source;
    private List<String> tags;
    private String category;
    @NotEmpty(message = "Link tải file không được để trống")
    private String dataUrl;
    private String provider;
}