package io.ldxinsight.service.impl;

import io.ldxinsight.dto.CreateDatasetRequest;
import io.ldxinsight.dto.DatasetDto;
import io.ldxinsight.dto.StatSummaryDto;
import io.ldxinsight.exception.ResourceNotFoundException;
import io.ldxinsight.mapper.DatasetMapper;
import io.ldxinsight.model.Dataset;
import io.ldxinsight.repository.DatasetRepository;
import io.ldxinsight.service.DatasetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;

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
            page = datasetRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword, pageable);
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
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update().inc("viewCount", 1);
        mongoTemplate.updateFirst(query, update, Dataset.class);
    }

    @Override
    public String getDownloadUrlAndIncrement(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update().inc("downloadCount", 1);

        Dataset dataset = mongoTemplate.findAndModify(query, update, Dataset.class);

        if (dataset == null) {
            throw new ResourceNotFoundException("Dataset not found with id: " + id);
        }
        return dataset.getDataUrl();
    }

    @Override
    public StatSummaryDto getStatsSummary() {
        long totalDatasets = datasetRepository.count();

        GroupOperation group = Aggregation.group()
                .sum("viewCount").as("totalViews")
                .sum("downloadCount").as("totalDownloads");

        Aggregation aggregation = Aggregation.newAggregation(group);
        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, "datasets", Map.class);

        long totalViews = 0;
        long totalDownloads = 0;

        Map<String, Long> resultMap = results.getUniqueMappedResult();
        if (resultMap != null) {
            totalViews = resultMap.getOrDefault("totalViews", 0L);
            totalDownloads = resultMap.getOrDefault("totalDownloads", 0L);
        }

        return new StatSummaryDto(totalDatasets, totalViews, totalDownloads);
    }

    private Dataset findDatasetById(String id) {
        return datasetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset not found with id: " + id));
    }

    @Override
    public List<String> getAllCategories() {
        // Sử dụng MongoTemplate để thực hiện 1 truy vấn "distinct"
        // tương đương: db.datasets.distinct("category")
        List<String> categories = mongoTemplate.query(Dataset.class)
                .distinct("category")
                .as(String.class)
                .all();

        return categories;
    }

    @Override
    public Page<DatasetDto> getDatasetsByCategory(String category, Pageable pageable) {
        Page<Dataset> page = datasetRepository.findByCategoryIgnoreCase(category, pageable);
        return page.map(datasetMapper::toDto);
    }

}