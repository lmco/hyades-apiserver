package org.dependencytrack.vulnanalysis.efoss;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;

import org.cyclonedx.proto.v1_7.Bom;
import org.dependencytrack.vulnanalysis.api.VulnAnalyzer;
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

        // String schema = "type Query{hello: String}";

        // SchemaParser schemaParser = new SchemaParser();
        // TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

        // RuntimeWiring runtimeWiring = newRuntimeWiring()
        //         .type("Query", builder -> builder.dataFetcher("hello", new StaticDataFetcher("world")))
        //         .build();

        // SchemaGenerator schemaGenerator = new SchemaGenerator();
        // GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        // GraphQL build = GraphQL.newGraphQL(graphQLSchema).build();
        // ExecutionResult executionResult = build.execute("{hello}");

        // System.out.println(executionResult.getData().toString());

        final var request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(apiBaseUrl))
                .header("Accept", "application/json")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
    }
    
}
