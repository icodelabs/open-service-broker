package com.swisscom.cloud.sb.broker.metrics

import com.swisscom.cloud.sb.broker.model.ServiceBinding
import com.swisscom.cloud.sb.broker.model.ServiceInstance
import com.swisscom.cloud.sb.broker.model.repository.*
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.util.Map.Entry

@Service
@CompileStatic
@Slf4j
class BindingMetricsService extends ServiceBrokerMetrics {

    final String BINDING = "binding"
    final String BINDING_REQUEST = "bindingRequest"

    ServiceBindingRepository serviceBindingRepository
    MeterRegistry meterRegistry

    HashMap<String, Long> totalBindingRequestsPerService = new HashMap<>()
    HashMap<String, Long> totalSuccessfulBindingRequestsPerService = new HashMap<>()
    HashMap<String, Long> totalFailedBindingRequestsPerService = new HashMap<>()

    @Autowired
    BindingMetricsService(ServiceInstanceRepository serviceInstanceRepository, CFServiceRepository cfServiceRepository, LastOperationRepository lastOperationRepository, ServiceBindingRepository serviceBindingRepository, PlanRepository planRepository, MeterRegistry meterRegistry) {
        super(serviceInstanceRepository, cfServiceRepository, lastOperationRepository, planRepository)
        this.serviceBindingRepository = serviceBindingRepository
        this.meterRegistry = meterRegistry
        addMetricsToMeterRegistry(meterRegistry, serviceBindingRepository, cfServiceRepository)
    }

    long retrieveMetricsForTotalNrOfSuccessfulBindings(List<ServiceBinding> serviceBindingList) {
        def totalNrOfSuccessfulBindings = serviceBindingList.size()
        log.info("Total nr of provision requests: ${totalNrOfSuccessfulBindings}")
        return totalNrOfSuccessfulBindings
    }

    HashMap<String, Long> retrieveTotalNrOfSuccessfulBindingsPerService(List<ServiceBinding> serviceBindingList, CFServiceRepository cfServiceRepository) {
        HashMap<String, Long> totalHm = new HashMap<>()
        totalHm = harmonizeServicesHashMapsWithServicesInRepository(totalHm, cfServiceRepository)

        serviceBindingList.each { serviceBinding ->
            def serviceInstance = serviceBinding.serviceInstance
            if (serviceInstance != null) {
                def service = serviceInstance.plan.service
                def serviceName = "someService"
                if (service) {
                    serviceName = service.name
                }
                totalHm = addOrUpdateEntryOnHashMap(totalHm, serviceName)
            }
        }
        log.info("${tag()} total bindings per service: ${totalHm}")
        return totalHm
    }

    void setTotalBindingRequestsPerService(ServiceInstance serviceInstance) {
        def cfServiceName = getServiceName(serviceInstance)
        totalBindingRequestsPerService = addOrUpdateEntryOnHashMap(totalBindingRequestsPerService, cfServiceName)
        calculateFailedBindingRequestsPerService()
    }

    void setSuccessfulBindingRequestsPerService(ServiceInstance serviceInstance) {
        def cfServiceName = getServiceName(serviceInstance)
        totalSuccessfulBindingRequestsPerService = addOrUpdateEntryOnHashMap(totalSuccessfulBindingRequestsPerService, cfServiceName)
        calculateFailedBindingRequestsPerService()
    }

    void calculateFailedBindingRequestsPerService() {
        totalFailedBindingRequestsPerService = harmonizeServicesHashMapsWithServicesInRepository(totalFailedBindingRequestsPerService, cfServiceRepository)
        totalBindingRequestsPerService.each { service ->
            def key = service.getKey()
            def totalValue = service.getValue()
            if (totalValue == null) {
                totalValue = 0
            }
            def successValue = totalSuccessfulBindingRequestsPerService.get(key)
            if (totalValue == null) {
                totalValue = 0
            }
            if (successValue == null) {
                successValue = 0
            }
            def failureValue = totalValue - successValue
            totalFailedBindingRequestsPerService.put(key, failureValue)
        }
    }

    double getBindingCount() {
        retrieveMetricsForTotalNrOfSuccessfulBindings(serviceBindingRepository.findAll()).toDouble()
    }

    double getSuccessfulBindingCount(Entry<String, Long> entry, CFServiceRepository cfServiceRepository) {
        def serviceBindings = serviceBindingRepository.findAll()
        if (retrieveTotalNrOfSuccessfulBindingsPerService(serviceBindings, cfServiceRepository).containsKey(entry.getKey())) {
            return retrieveTotalNrOfSuccessfulBindingsPerService(serviceBindings, cfServiceRepository).get(entry.getKey()).toDouble()
        }
        0.0
    }

    void addMetricsToMeterRegistry(MeterRegistry meterRegistry, ServiceBindingRepository serviceBindingRepository, CFServiceRepository cfServiceRepository) {
        List<ServiceBinding> serviceBindingList = serviceBindingRepository.findAll()
        addMetricsGauge(meterRegistry, "${BINDING}.${TOTAL}.${TOTAL}", { getBindingCount() }, TOTAL)

        def totalNrOfSuccessfulBindingsPerService = retrieveTotalNrOfSuccessfulBindingsPerService(serviceBindingList, cfServiceRepository)
        totalNrOfSuccessfulBindingsPerService.each { entry ->
            addMetricsGauge(meterRegistry, "${BINDING}.${SERVICE}.${TOTAL}.${entry.getKey()}", {
                getSuccessfulBindingCount(entry, cfServiceRepository)
            }, SERVICE)
        }

        totalBindingRequestsPerService = harmonizeServicesHashMapsWithServicesInRepository(totalBindingRequestsPerService, cfServiceRepository)
        totalBindingRequestsPerService.each { entry ->
            addMetricsGauge(meterRegistry, "${BINDING_REQUEST}.${SERVICE}.${TOTAL}.${entry.getKey()}", {
                getCountForEntryFromHashMap(totalBindingRequestsPerService, entry)
            }, SERVICE)
        }

        totalSuccessfulBindingRequestsPerService = harmonizeServicesHashMapsWithServicesInRepository(totalSuccessfulBindingRequestsPerService, cfServiceRepository)
        totalSuccessfulBindingRequestsPerService.each { entry ->
            addMetricsGauge(meterRegistry, "${BINDING_REQUEST}.${SERVICE}.${SUCCESS}.${entry.getKey()}", {
                getCountForEntryFromHashMap(totalSuccessfulBindingRequestsPerService, entry)
            }, SERVICE)
        }

        totalFailedBindingRequestsPerService = harmonizeServicesHashMapsWithServicesInRepository(totalFailedBindingRequestsPerService, cfServiceRepository)
        totalFailedBindingRequestsPerService.each { entry ->
            addMetricsGauge(meterRegistry, "${BINDING_REQUEST}.${SERVICE}.${FAIL}.${entry.getKey()}", {
                getCountForEntryFromHashMap(totalFailedBindingRequestsPerService, entry)
            }, SERVICE)
        }
    }

    @Override
    boolean considerServiceInstance(ServiceInstance serviceInstance) {
        return false
    }

    @Override
    String tag() {
        return BindingMetricsService.class.getSimpleName()
    }
}
