package io.ldxinsight.mapper;

import io.ldxinsight.dto.RegisterRequest;
import io.ldxinsight.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthMapper {
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toUser(RegisterRequest request);
}