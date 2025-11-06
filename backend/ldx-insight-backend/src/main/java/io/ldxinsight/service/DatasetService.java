package io.ldxinsight.service;

import io.ldxinsight.dto.CreateDatasetRequest;
import io.ldxinsight.dto.DatasetDto;
import io.ldxinsight.dto.StatSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DatasetService {
    Page<DatasetDto> searchDatasets(String keyword, String category, Pageable pageable);

    DatasetDto getDatasetById(String id);
    DatasetDto createDataset(CreateDatasetRequest request);
    DatasetDto updateDataset(String id, CreateDatasetRequest request);
    void deleteDataset(String id);

    void incrementViewCount(String id);
    String getDownloadUrlAndIncrement(String id);

    StatSummaryDto getStatsSummary();
}