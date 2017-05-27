package com.swisscom.cf.broker.cfextensions.serviceusage

import com.google.common.base.Optional
import com.swisscom.cf.broker.binding.BindRequest
import com.swisscom.cf.broker.binding.BindResponse
import com.swisscom.cf.broker.binding.UnbindRequest
import com.swisscom.cf.broker.model.DeprovisionRequest
import com.swisscom.cf.broker.model.ProvisionRequest
import com.swisscom.cf.broker.model.ServiceInstance
import com.swisscom.cf.broker.provisioning.DeprovisionResponse
import com.swisscom.cf.broker.provisioning.ProvisionResponse
import com.swisscom.cf.broker.services.common.*
import com.swisscom.cloud.servicebroker.model.usage.ServiceUsage
import com.swisscom.cloud.servicebroker.model.usage.ServiceUsageType
import spock.lang.Specification

class ServiceUsageLookupSpec extends Specification {
    ServiceProviderLookup serviceProviderLookup
    ServiceUsageLookup serviceUsageLookup

    def setup() {
        serviceProviderLookup = Mock(ServiceProviderLookup)
        serviceUsageLookup = new ServiceUsageLookup(serviceProviderLookup)
    }

    def "throws an exception when discovered serviceProvider does not provide usage info"() {
        given:
        serviceProviderLookup.findServiceProvider(_) >> Stub(ServiceProvider)
        when:
        serviceUsageLookup.usage(new ServiceInstance())
        then:
        def ex = thrown(RuntimeException)
        ex
    }

    def "happy path: usage info dto is created correctly"() {
        given:
        def service = new DummyService("1", ServiceUsageType.TRANSACTIONS)
        serviceProviderLookup.findServiceProvider(_) >> service

        when:
        ServiceUsage serviceUsage = serviceUsageLookup.usage(new ServiceInstance(), Optional.absent())

        then:
        serviceUsage.value == service.serviceUsageValue
        serviceUsage.type == service.serviceUsageType
    }

    private class DummyService implements ServiceProvider, ServiceUsageProvider {

        private String serviceUsageValue
        private ServiceUsageType serviceUsageType

        DummyService(String serviceUsageValue, ServiceUsageType serviceUsageType) {
            this.serviceUsageValue = serviceUsageValue
            this.serviceUsageType = serviceUsageType
        }

        @Override
        ServiceUsage findUsage(ServiceInstance serviceInstance, Optional<Date> enddate) {
            return new ServiceUsage(type: serviceUsageType, value: serviceUsageValue)
        }

        String getServiceUsageValue() {
            return serviceUsageValue
        }

        ServiceUsageType getServiceUsageType() {
            return serviceUsageType
        }

        @Override
        BindResponse bind(BindRequest request) {
            return null
        }

        @Override
        void unbind(UnbindRequest request) {

        }

        @Override
        ProvisionResponse provision(ProvisionRequest request) {
            return null
        }

        @Override
        DeprovisionResponse deprovision(DeprovisionRequest request) {
            return null
        }
    }
}
