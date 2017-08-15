package com.swisscom.cloud.sb.broker.servicedefinition.dto

import groovy.transform.CompileStatic

@CompileStatic
class PlanDto extends com.swisscom.cloud.sb.broker.cfapi.dto.PlanDto {
    String guid
    String templateId
    String internalName
    String serviceProviderName
    int displayIndex
    boolean asyncRequired
    int maxBackups
    List<ParameterDto> parameters
    List<ParameterDto> containerParams
}

