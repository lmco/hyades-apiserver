package org.dependencytrack.vulnanalysis.generic;

import java.net.http.HttpClient;

import org.dependencytrack.plugin.api.RuntimeConfigurable;
import org.dependencytrack.plugin.api.ServiceRegistry;
import org.dependencytrack.plugin.api.config.ConfigRegistry;
import org.dependencytrack.vulnanalysis.api.VulnAnalyzer;
import org.dependencytrack.vulnanalysis.api.VulnAnalyzerFactory;
import org.dependencytrack.vulnanalysis.api.VulnAnalyzerRequirement;

public class GenericVulnAnalyzerFactory implements VulnAnalyzerFactory, RuntimeConfigurable {
    
    private @Nullable ConfigRegistry configRegistry;
    private @Nullable HttpClient httpClient;

    @Override
    public String extensionName() {
        return "generic";
    }

     @Override
    public Class<? extends VulnAnalyzer> extensionClass() {
        return GenericVulnAnalyzer.class;
    }

    @Override
    public boolean isEnabled() {
        requireNonNull(configRegistry);
        return configRegistry.getRuntimeConfig(GenericVulnAnalyzerConfigV1.class).isEnabled();
    }

    @Override
    public EnumSet<VulnAnalyzerRequirement> analyzerRequirements() {
        return EnumSet.of();
    }

    @Override
    public void init(ServiceRegistry serviceRegistry) {
        configRegistry = serviceRegistry.require(ConfigRegistry.class);
        httpClient = serviceRegistry.require(HttpClient.class);
    }

    @Override
    public VulnAnalyzer create() {
        requireNonNull(configRegistry);
        requireNonNull(httpClient);

        final var config = configRegistry.getRuntimeConfig(GenericVulnAnalyzerConfigV1.class);
        if (!config.isEnabled()) {
            throw new IllegalStateException("Analyzer is disabled");
        }

        return new GenericVulnAnalyzer(
            httpClient,
            config.getApiUrl().toString(),
            config.getApiToken()
        );
    }

    @Override
    public RuntimeConfigSpec runtimeConfigSpec() {
        return RuntimeConfigSpec.of(
        new GenericVulnAnalyzerConfigV1()
                .withEnabled(false),
        config -> {
            if (!config.isEnabled()) {
                return;
            }
            if (config.getApiUrl() == null) {
                throw new InvalidRuntimeConfigException("No API URL provided");
            }
            if (config.getApiToken() == null) {
                throw new InvalidRuntimeConfigException("No API token provided");
            }
        });
    }

}
