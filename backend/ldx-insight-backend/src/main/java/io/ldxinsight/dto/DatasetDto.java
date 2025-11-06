package io.ldxinsight.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class DatasetDto {
    private String id;
    private String title;
    private String description;
    private String source;
    private List<String> tags;
    private String category;
    private long viewCount;
    private long downloadCount;
    private String provider;
    private Instant createdAt;
    private Instant updatedAt;
}