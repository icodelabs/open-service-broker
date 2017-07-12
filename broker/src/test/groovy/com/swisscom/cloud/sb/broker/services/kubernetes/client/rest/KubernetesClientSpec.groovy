package com.swisscom.cloud.sb.broker.services.kubernetes.client.rest

import com.swisscom.cloud.sb.broker.services.kubernetes.redis.config.KubernetesRedisConfig
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

import java.security.KeyStore

class KubernetesClientSpec extends Specification {


    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    KubernetesClient kubernetesClient
    KubernetesRedisConfig kubernetesConfig
    RestTemplate restTemplate

    def setup() {
        kubernetesClient = Spy(KubernetesClient)
        kubernetesClient.enableSSLWithClientCertificate() >> null
        mockRestTemplate(kubernetesClient)
        decorateClient(kubernetesClient)
    }

    def "exchange uses the right endpoint"() {
        given:
        restTemplate.exchange("https://:/endpoint", _, _, _) >> new ResponseEntity("OK", HttpStatus.ACCEPTED)
        expect:
        kubernetesClient.exchange("endpoint", HttpMethod.GET, "body", String.class) != null
    }

    def "exchange returns correct result"() {
        given:
        restTemplate.exchange(_, _, _, _) >> new ResponseEntity("OK", HttpStatus.ACCEPTED)
        and:
        ResponseEntity result = kubernetesClient.exchange("endpoint", HttpMethod.GET, "body", String.class)
        expect:
        result.getBody() == "OK"
    }

    private void decorateClient(KubernetesClient kubernetesClient) {
        File createdFile = folder.newFile("tmp.txt")
        kubernetesConfig = mockConfig(createdFile)
        kubernetesClient.keyStore = Mock(KeyStore)
        kubernetesClient.kubernetesConfig = kubernetesConfig
    }

    private void mockRestTemplate(KubernetesClient kubernetesClient) {
        restTemplate = Stub(RestTemplate)
        kubernetesClient.restTemplate = restTemplate
    }

    private KubernetesRedisConfig mockConfig(File createdFile) {
        kubernetesConfig = Stub(KubernetesRedisConfig)
        kubernetesConfig.getKubernetesClientPFXPath() >> createdFile.getAbsolutePath()
        kubernetesConfig.getKubernetesClientPFXPasswordPath() >> "PASS"
        kubernetesConfig
    }


}
