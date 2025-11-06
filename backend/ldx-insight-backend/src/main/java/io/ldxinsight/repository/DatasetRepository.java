package io.ldxinsight.repository;

import io.ldxinsight.model.Dataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasetRepository extends MongoRepository<Dataset, String> {

    Page<Dataset> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String titleKeyword, String descriptionKeyword, Pageable pageable
    );

    Page<Dataset> findByCategoryIgnoreCase(String category, Pageable pageable);

    long countByCategoryIgnoreCase(String category);
}