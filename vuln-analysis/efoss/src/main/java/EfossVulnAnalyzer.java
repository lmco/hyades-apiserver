package org.dependencytrack.vulnanalysis.efoss;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;

import org.cyclonedx.proto.v1_7.Bom;
import org.cyclonedx.proto.v1_7.Component;
import org.dependencytrack.vulnanalysis.api.VulnAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;


final class EfossVulnAnalyzer implements VulnAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EfossVulnAnalyzer.class);

    private final HttpClient httpClient;
    private final String apiBaseUrl;
    private final String apiToken;
    private final ArrayList<EfossRequestObject> requestObjects;

    EfossVulnAnalyzer(
            HttpClient httpClient,
            String apiBaseUrl,
            String apiToken) {
        this.httpClient = httpClient;
        this.apiBaseUrl = apiBaseUrl;
        this.apiToken = apiToken;
    }

    @Override
    public Bom analyze(Bom bom) throws InterruptedException {

        for (final Component component : bom.getComponentsList()) {
            EfossRequestObject temp = new EfossRequestObject(component);
            requestObjects.add(temp);
        }


        StringBuilder builder = new StringBuilder("{fossComponentRecords(ids: [");
        for(Iterator<EfossRequestObject> itr = requestObjects.iterator(); itr.hasNext();) {
            EfossRequestObject current = itr.next();
            builder.append("\"");
            builder.append(current.getId());
            if(itr.hasNext())
                builder.append("\", ");
            else
                builder.append("\"]) {id group licenseIds licenses {licenseId licenseName} purl}}");
        }
        String schema = builder.toString();

        final var request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(apiBaseUrl))
                .header("Accept", "application/json")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofByteArray(schema.getBytes()))
                .build();

        final HttpResponse<byte[]> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException("eFOSS API request to %s failed".formatted(apiBaseUrl), e);
        }

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            // TODO: Do some stuff
        }

        throw new IllegalStateException(
                "eFOSS API request to %s failed with status %d".formatted(apiBaseUrl, response.statusCode()));
    }
    
}
