package io.ldxinsight.repository;

import io.ldxinsight.dto.CategoryStatisDTO;
import io.ldxinsight.model.Dataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetRepository extends MongoRepository<Dataset, String> {

    Page<Dataset> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String titleKeyword, String descriptionKeyword, Pageable pageable
    );

    Page<Dataset> findByCategoryIgnoreCase(String category, Pageable pageable);

    long countByCategoryIgnoreCase(String category);

    @Query("SELECT new io.ldxinsight.dto.CategoryStatDto(d.category, COUNT(d.id)) " +
           "FROM Dataset d " +
           "GROUP BY d.category " +
           "ORDER BY COUNT(d.id) DESC")
    List<CategoryStatisDTO> countDatasetsByCategory();

    Page<Dataset> findByOrderByViewCountDesc(Pageable pageable);

    Page<Dataset> findByOrderByDownloadCountDesc(Pageable pageable);
}