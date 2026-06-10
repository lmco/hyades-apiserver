package org.dependencytrack.vulnanalysis.efoss;

import java.net.http.HttpClient;
import java.util.EnumSet;

import org.dependencytrack.plugin.api.RuntimeConfigurable;
import org.dependencytrack.plugin.api.ServiceRegistry;
import org.dependencytrack.plugin.api.config.ConfigRegistry;
import org.dependencytrack.plugin.api.config.InvalidRuntimeConfigException;
import org.dependencytrack.plugin.api.config.RuntimeConfigSpec;
import org.dependencytrack.vulnanalysis.api.VulnAnalyzer;
import org.dependencytrack.vulnanalysis.api.VulnAnalyzerFactory;
import org.dependencytrack.vulnanalysis.api.VulnAnalyzerRequirement;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public class EfossVulnAnalyzerFactory implements VulnAnalyzerFactory, RuntimeConfigurable {
    
    private @Nullable ConfigRegistry configRegistry;
    private @Nullable HttpClient httpClient;

    @Override
    public String extensionName() {
        return "efoss";
    }

     @Override
    public Class<? extends VulnAnalyzer> extensionClass() {
        return EfossVulnAnalyzer.class;
    }

    @Override
    public boolean isEnabled() {
        requireNonNull(configRegistry);
        return configRegistry.getRuntimeConfig(EfossVulnAnalyzerConfigV1.class).isEnabled();
    }

    @Override
    public EnumSet<VulnAnalyzerRequirement> analyzerRequirements() {
        return EnumSet.noneOf(VulnAnalyzerRequirement.class);
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

        final var config = configRegistry.getRuntimeConfig(EfossVulnAnalyzerConfigV1.class);
        if (!config.isEnabled()) {
            throw new IllegalStateException("Analyzer is disabled");
        }

        return new EfossVulnAnalyzer(
            httpClient,
            config.getApiUrl().toString(),
            config.getApiToken()
        );
    }

    @Override
    public RuntimeConfigSpec runtimeConfigSpec() {
        return RuntimeConfigSpec.of(
        new EfossVulnAnalyzerConfigV1()
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
