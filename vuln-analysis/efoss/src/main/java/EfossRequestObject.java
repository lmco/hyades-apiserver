package org.dependencytrack.vulnanalysis.efoss;

import com.github.packageurl.PackageURL;

import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.proto.v1_7.Component;


public class EfossRequestObject {

    private PackageURL purl;
    private String type;
    private String group;
    private LicenseChoice licenseChoice = new LicenseChoice();

    EfossRequestObject(Component component) {
        this.purl = new PackageURL(component.getPurl());
        this.group = component.getGroup();

        this.licenseChoice.setLicenses(component.getLicenses().getLicenses());
    }

    public String getId(){
        StringBuilder builder = new StringBuilder(purl.getType());
        builder.append(group);
        builder.append(purl.getName());
        builder.append(purl.getVersion());

        return builder.toString();
    }
}
