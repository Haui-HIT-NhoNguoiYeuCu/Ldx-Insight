package io.ldxinsight.mapper;

import io.ldxinsight.dto.CreateDatasetRequest;
import io.ldxinsight.dto.DatasetDto;
import io.ldxinsight.model.Dataset;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DatasetMapper {

    DatasetDto toDto(Dataset dataset);

    List<DatasetDto> toDtoList(List<Dataset> datasets);

    Dataset toEntity(CreateDatasetRequest request);

    void updateFromRequest(CreateDatasetRequest request, @MappingTarget Dataset dataset);
}