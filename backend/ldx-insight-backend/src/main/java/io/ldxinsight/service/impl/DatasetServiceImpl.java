package io.ldxinsight.service.impl;

import io.ldxinsight.dto.*;
import io.ldxinsight.exception.ResourceNotFoundException;
import io.ldxinsight.mapper.DatasetMapper;
import io.ldxinsight.model.Dataset;
import io.ldxinsight.repository.DatasetRepository;
import io.ldxinsight.service.DatasetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetServiceImpl implements DatasetService {

    private final DatasetRepository datasetRepository;
    private final DatasetMapper datasetMapper;
    private final MongoTemplate mongoTemplate;

    /**
     * Thư mục chứa dữ liệu cục bộ.
     * Có thể override qua:
     * - application.yml: ldx.data.dir: /path/to/data
     * - hoặc biến môi trường: LDX_DATA_DIR
     * Mặc định: /mnt/data
     */
    @Value("${ldx.data.dir:${LDX_DATA_DIR:/mnt/data}}")
    private String localDataDir;

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
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().inc("viewCount", 1);
        mongoTemplate.updateFirst(query, update, Dataset.class);
    }

    @Override
    public String getDownloadUrlAndIncrement(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().inc("downloadCount", 1);

        Dataset dataset = mongoTemplate.findAndModify(query, update, Dataset.class);
        if (dataset == null) {
            throw new ResourceNotFoundException("Dataset not found with id: " + id);
        }

        // Nếu không có dataUrl, controller sẽ fallback đọc file local qua getDataUrl(id)
        return "/api/v1/datasets/" + id + "/download.json";
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
        // Sử dụng MongoDB aggregation để group by category và count
        // Lọc bỏ các document có category null hoặc empty
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("category").ne(null).ne("")),
                Aggregation.group("category")
                        .count().as("count"),
                Aggregation.project("count")
                        .and("_id").as("category"),
                Aggregation.sort(Sort.Direction.DESC, "count")
        );

        List<CategoryStatisDTO> results = mongoTemplate
                .aggregate(aggregation, "datasets", CategoryStatisDTO.class)
                .getMappedResults();

        log.info("Category stats: {} categories found", results.size());
        return results;
    }

    @Override
    public List<DatasetDto> getTopViewedDatasets(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Dataset> topViewedPage = datasetRepository.findByOrderByViewCountDesc(pageable);
        return topViewedPage.map(datasetMapper::toDto).getContent();
    }

    @Override
    public List<DatasetDto> getTopDownloadedDatasets(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Dataset> topDownloadedPage = datasetRepository.findByOrderByDownloadCountDesc(pageable);
        return topDownloadedPage.map(datasetMapper::toDto).getContent();
    }

    /**
     * Lấy dataUrl, nếu thiếu thì fallback tìm file JSON cục bộ theo quy ước:
     *  - {localDataDir}/{id}.json
     *  - {localDataDir}/{slug(title)}.json
     * Trả về dạng URI "file:///...".
     */
    @Override
    public String getDataUrl(String id) {
        Dataset dataset = datasetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset not found with id: " + id));

        // 1) Có dataUrl thì dùng ngay
        String stored = dataset.getDataUrl();
        if (StringUtils.hasText(stored)) {
            log.debug("Using stored dataUrl for dataset {}: {}", id, stored);
            return stored.trim();
        }

        // 2) Fallback tìm file local
        String baseDir = StringUtils.hasText(localDataDir) ? localDataDir.trim() : "/mnt/data";
        log.debug("Looking for local JSON file for dataset {} in directory: {}", id, baseDir);

        // Ưu tiên theo id
        Path byId = Paths.get(baseDir, id + ".json");
        Path absoluteById = byId.toAbsolutePath();
        log.debug("Checking file by id: {} (absolute: {})", byId, absoluteById);
        if (Files.exists(byId) && Files.isReadable(byId)) {
            String uri = byId.toUri().toString();
            log.info("Found fallback dataUrl by id for dataset {}: {} (absolute path: {})", id, uri, absoluteById);
            return uri;
        }

        // Sau đó theo slug(title)
        String title = dataset.getTitle();
        if (StringUtils.hasText(title)) {
            String slug = slugify(title);
            Path byTitle = Paths.get(baseDir, slug + ".json");
            Path absoluteByTitle = byTitle.toAbsolutePath();
            log.debug("Checking file by title slug for dataset {}: {} (absolute: {})", id, byTitle, absoluteByTitle);
            if (Files.exists(byTitle) && Files.isReadable(byTitle)) {
                String uri = byTitle.toUri().toString();
                log.info("Found fallback dataUrl by title for dataset {}: {} (absolute path: {})", id, uri, absoluteByTitle);
                return uri;
            }
        }

        log.warn("Dataset {} exists but no dataUrl found. Searched in: {} (absolute: {})", 
                id, baseDir, Paths.get(baseDir).toAbsolutePath());
        throw new ResourceNotFoundException(
                String.format("Dataset does not have a download URL and no local JSON file found. " +
                        "Dataset ID: %s, Searched directory: %s (absolute: %s), " +
                        "Tried files: %s.json, %s.json",
                        id, baseDir, Paths.get(baseDir).toAbsolutePath(), 
                        id, StringUtils.hasText(title) ? slugify(title) : "N/A"));
    }

    /* ====================== Helpers ====================== */

    private Dataset findDatasetById(String id) {
        return datasetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset not found with id: " + id));
    }

    /** Tạo slug từ tiêu đề: bỏ dấu, bỏ ký tự lạ, thay khoảng trắng bằng '-' */
    private String slugify(String input) {
        String noAccent = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String safe = noAccent.replaceAll("[^\\w\\d\\-\\s]", "");
        return safe.trim().replaceAll("\\s+", "-").toLowerCase();
    }
}
