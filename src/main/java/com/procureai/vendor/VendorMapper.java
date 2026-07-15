package com.procureai.vendor;

import com.procureai.vendor.dto.VendorRequest;
import com.procureai.vendor.dto.VendorResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface VendorMapper {

    VendorResponse toResponse(Vendor vendor);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "onboardedAt", ignore = true)
    Vendor toEntity(VendorRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "onboardedAt", ignore = true)
    void updateEntity(VendorRequest request, @MappingTarget Vendor vendor);
}
