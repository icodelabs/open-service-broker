package com.swisscom.cloud.sb.broker.servicedefinition

import com.swisscom.cloud.sb.broker.model.CFService
import com.swisscom.cloud.sb.broker.model.repository.CFServiceRepository
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component

@Component
@EnableConfigurationProperties
@Slf4j
class ServiceDefinitionInitializer implements ApplicationRunner{

    private CFServiceRepository cfServiceRepository

    private ServiceDefinitionConfig serviceDefinitionConfig

    private ServiceDefinitionProcessor serviceDefinitionProcessor

    @Autowired
    ServiceDefinitionInitializer(CFServiceRepository cfServiceRepository, ServiceDefinitionConfig serviceDefinitionConfig, ServiceDefinitionProcessor serviceDefinitionProcessor) {
        this.cfServiceRepository = cfServiceRepository
        this.serviceDefinitionConfig = serviceDefinitionConfig
        this.serviceDefinitionProcessor = serviceDefinitionProcessor
    }

    @Override
    void run(ApplicationArguments args) throws Exception {
        List<CFService> cfServiceList = cfServiceRepository.findAll()

        checkForMissingServiceDefinitions(cfServiceList)
        addServiceDefinitions()
    }

    void checkForMissingServiceDefinitions(List<CFService> cfServiceList) {
        List<String> configNameList = new ArrayList<>()
        serviceDefinitionConfig.serviceDefinitions.each {
            configNameList << it.name
        }

        List<String> nameList = new ArrayList<>()
        cfServiceList.each {
            nameList << it.name
        }

        if (!configNameList.containsAll(nameList)){
            throw new Exception("Missing service definition configuration exception. Service list - ${nameList}")
        }
    }

    void addServiceDefinitions() {
        serviceDefinitionConfig.serviceDefinitions.each {
            serviceDefinitionProcessor.createOrUpdateServiceDefinitionFromYaml(it)
        }
    }
}
