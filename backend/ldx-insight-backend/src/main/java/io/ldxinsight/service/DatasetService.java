package io.ldxinsight.service;

import io.ldxinsight.dto.*;
import io.ldxinsight.model.Dataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DatasetService {
    Page<DatasetDto> searchDatasets(String keyword, String category, Pageable pageable);

    DatasetDto getDatasetById(String id);
    DatasetDto createDataset(CreateDatasetRequest request);
    DatasetDto updateDataset(String id, CreateDatasetRequest request);
    void deleteDataset(String id);

    void incrementViewCount(String id);
    String getDownloadUrlAndIncrement(String id);

    StatSummaryDto getStatsSummary();

    List<String> getAllCategories();

    Page<DatasetDto> getDatasetsByCategory(String category, Pageable pageable);

    List<CategoryStatisDTO> getCategoryStats();
    List<DatasetDto> getTopViewedDatasets(int limit);
    List<DatasetDto> getTopDownloadedDatasets(int limit);
    String getDataUrl(String id);
}