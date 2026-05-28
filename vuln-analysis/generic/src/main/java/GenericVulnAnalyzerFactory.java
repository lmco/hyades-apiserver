package org.dependencytrack.vulnanalysis.generic;

import java.net.http.HttpClient;

import org.dependencytrack.plugin.api.RuntimeConfigurable;
import org.dependencytrack.plugin.api.ServiceRegistry;
import org.dependencytrack.plugin.api.config.ConfigRegistry;
import org.dependencytrack.vulnanalysis.api.VulnAnalyzer;
import org.dependencytrack.vulnanalysis.api.VulnAnalyzerFactory;

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
    public void init(ServiceRegistry serviceRegistry) {
        configRegistry = serviceRegistry.require(ConfigRegistry.class);
        httpClient = serviceRegistry.require(HttpClient.class);
    }

    @Override
    public VulnAnalyzer create() {
    
    }

    @Override
    public RuntimeConfigSpec runtimeConfigSpec() {
        
    }

}
