package org.dependencytrack.vulnanalysis.generic;

import java.net.http.HttpClient;

import org.cyclonedx.proto.v1_7.Bom;
import org.dependencytrack.vulnanalysis.api.VulnAnalyzer;
import org.slf4j.LoggerFactory;


final class GenericVulnAnalyzer implements VulnAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericVulnAnalyzer.class);

    private final HttpClient httpClient;
    private final String apiBaseUrl;
    private final String apiToken;

    GenericVulnAnalyzer(
            HttpClient httpClient,
            String apiBaseUrl,
            String apiToken) {
        this.httpClient = httpClient;
        this.apiBaseUrl = apiBaseUrl;
        this.apiToken = apiToken;
    }

    @Override
    public Bom analyze(Bom bom) throws InterruptedException {
        // TODO
    }
    
}
