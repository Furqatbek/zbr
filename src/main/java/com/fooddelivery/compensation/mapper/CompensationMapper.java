package com.fooddelivery.compensation.mapper;

import com.fooddelivery.compensation.dto.CompensationLogDto;
import com.fooddelivery.compensation.dto.CompensationRuleDto;
import com.fooddelivery.compensation.dto.CustomerCreditDto;
import com.fooddelivery.compensation.entity.CompensationLog;
import com.fooddelivery.compensation.entity.CompensationRule;
import com.fooddelivery.compensation.entity.CustomerCredit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for compensation entities and DTOs.
 */
@Mapper(componentModel = "spring")
public interface CompensationMapper {

    /**
     * Map compensation rule entity to DTO.
     */
    @Mapping(target = "conditions", source = "conditionJson")
    CompensationRuleDto toRuleDto(CompensationRule rule);

    /**
     * Map list of compensation rules to DTOs.
     */
    List<CompensationRuleDto> toRuleDtoList(List<CompensationRule> rules);

    /**
     * Map compensation log entity to DTO.
     */
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderNumber", source = "order.externalOrderNo")
    @Mapping(target = "ruleId", source = "rule.id")
    @Mapping(target = "ruleName", source = "rule.name")
    CompensationLogDto toLogDto(CompensationLog log);

    /**
     * Map list of compensation logs to DTOs.
     */
    List<CompensationLogDto> toLogDtoList(List<CompensationLog> logs);

    /**
     * Map customer credit entity to DTO.
     */
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    CustomerCreditDto toCreditDto(CustomerCredit credit);
}
