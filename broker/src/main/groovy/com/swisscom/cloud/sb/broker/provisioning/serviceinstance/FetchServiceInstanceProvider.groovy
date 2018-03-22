package com.swisscom.cloud.sb.broker.provisioning.serviceinstance

import com.swisscom.cloud.sb.broker.model.ServiceInstance

interface FetchServiceInstanceProvider {
    ServiceInstanceResponseDto getServiceInstanceDetails(ServiceInstance instance)
}