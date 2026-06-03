package org.dependencytrack.vulnanalysis.efoss;

import org.dependencytrack.plugin.api.ExtensionFactory;
import org.dependencytrack.plugin.api.ExtensionPoint;
import org.dependencytrack.plugin.api.Plugin;

import java.util.Collection;
import java.util.List;

public final class GebericVulnAnalyzerPlugin implements Plugin {

    @Override
    public Collection<? extends ExtensionFactory<? extends ExtensionPoint>> extensionFactories() {
        return List.of(new EfossVulnAnalyzerFactory());
    }

}
