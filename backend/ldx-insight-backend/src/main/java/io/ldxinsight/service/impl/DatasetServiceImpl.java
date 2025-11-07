package io.ldxinsight.service.impl;

import io.ldxinsight.dto.*;
import io.ldxinsight.exception.ResourceNotFoundException;
import io.ldxinsight.mapper.DatasetMapper;
import io.ldxinsight.model.Dataset;
import io.ldxinsight.repository.DatasetRepository;
import io.ldxinsight.service.DatasetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DatasetServiceImpl implements DatasetService {

    private final DatasetRepository datasetRepository;
    private final DatasetMapper datasetMapper;
    private final MongoTemplate mongoTemplate;

    @Override
    public Page<DatasetDto> searchDatasets(String keyword, String category, Pageable pageable) {
        Page<Dataset> page;
        if (StringUtils.hasText(keyword)) {
            page = datasetRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    keyword, keyword, pageable);
        } else if (StringUtils.hasText(category)) {
            page = datasetRepository.findByCategoryIgnoreCase(category, pageable);
        } else {
            page = datasetRepository.findAll(pageable);
        }
        return page.map(datasetMapper::toDto);
    }

    @Override
    public DatasetDto getDatasetById(String id) {
        Dataset dataset = findDatasetById(id);
        return datasetMapper.toDto(dataset);
    }

    @Override
    public DatasetDto createDataset(CreateDatasetRequest request) {
        Dataset dataset = datasetMapper.toEntity(request);
        dataset = datasetRepository.save(dataset);
        return datasetMapper.toDto(dataset);
    }

    @Override
    public DatasetDto updateDataset(String id, CreateDatasetRequest request) {
        Dataset dataset = findDatasetById(id);
        datasetMapper.updateFromRequest(request, dataset);
        dataset = datasetRepository.save(dataset);
        return datasetMapper.toDto(dataset);
    }

    @Override
    public void deleteDataset(String id) {
        if (!datasetRepository.existsById(id)) {
            throw new ResourceNotFoundException("Dataset not found with id: " + id);
        }
        datasetRepository.deleteById(id);
    }

    @Override
    public void incrementViewCount(String id) {
        // Với MongoDB, trường id ánh xạ tới _id trong collection
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().inc("viewCount", 1);
        mongoTemplate.updateFirst(query, update, Dataset.class);
    }

    @Override
    public String getDownloadUrlAndIncrement(String id) {
        // tăng downloadCount
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().inc("downloadCount", 1);

        Dataset dataset = mongoTemplate.findAndModify(query, update, Dataset.class);
        if (dataset == null) {
            throw new ResourceNotFoundException("Dataset not found with id: " + id);
        }

        // Optional: vẫn kiểm tra có dataUrl để đảm bảo dataset thực sự có dữ liệu để tải
        String dataUrl = dataset.getDataUrl();
        if (dataUrl == null || dataUrl.trim().isEmpty()) {
            throw new ResourceNotFoundException("Dataset does not have a download URL");
        }

        // Trả về endpoint CSV trong hệ thống (controller sẽ stream CSV từ dataUrl)
        // CHÚ Ý: chỉ trả về PATH, controller sẽ convert thành absolute URL.
        return "/api/datasets/" + id + "/download.csv";
    }


    @Override
    public StatSummaryDto getStatsSummary() {
        long totalDatasets = datasetRepository.count();

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group()
                        .sum("viewCount").as("totalViews")
                        .sum("downloadCount").as("totalDownloads")
        );

        Map<String, Object> resultMap = mongoTemplate
                .aggregate(aggregation, "datasets", Map.class)
                .getUniqueMappedResult();

        long totalViews = 0L;
        long totalDownloads = 0L;

        if (resultMap != null) {
            Number views = (Number) resultMap.getOrDefault("totalViews", 0);
            Number dls = (Number) resultMap.getOrDefault("totalDownloads", 0);
            totalViews = views == null ? 0L : views.longValue();
            totalDownloads = dls == null ? 0L : dls.longValue();
        }

        return new StatSummaryDto(totalDatasets, totalViews, totalDownloads);
    }

    @Override
    public List<String> getAllCategories() {
        // db.datasets.distinct("category")
        return mongoTemplate.query(Dataset.class)
                .distinct("category")
                .as(String.class)
                .all();
    }

    @Override
    public Page<DatasetDto> getDatasetsByCategory(String category, Pageable pageable) {
        Page<Dataset> page = datasetRepository.findByCategoryIgnoreCase(category, pageable);
        return page.map(datasetMapper::toDto);
    }

    @Override
    public List<CategoryStatisDTO> getCategoryStats() {
        return datasetRepository.countDatasetsByCategory();
    }

    @Override
    public List<DatasetDto> getTopViewedDatasets(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Dataset> topViewedPage = datasetRepository.findByOrderByViewCountDesc(pageable);
        return topViewedPage.map(datasetMapper::toDto).getContent();
    }
    @Override
    public String getDataUrl(String id) {
        Dataset dataset = datasetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset not found with id: " + id));
        String dataUrl = dataset.getDataUrl();
        if (!org.springframework.util.StringUtils.hasText(dataUrl)) {
            throw new ResourceNotFoundException("Dataset does not have a download URL");
        }
        return dataUrl.trim();
    }

    @Override
    public List<DatasetDto> getTopDownloadedDatasets(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Dataset> topDownloadedPage = datasetRepository.findByOrderByDownloadCountDesc(pageable);
        return topDownloadedPage.map(datasetMapper::toDto).getContent();
    }

    private Dataset findDatasetById(String id) {
        return datasetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset not found with id: " + id));
    }
}
