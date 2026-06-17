package de.frachtwerk.essencium.backend.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "essencium.error")
public class EssenciumErrorProperties {

    private String urnPrefix = "urn:frachtwerk:error:";

    public String getUrnPrefix() {
        return urnPrefix;
    }

    public void setUrnPrefix(String urnPrefix) {
        this.urnPrefix = urnPrefix;
    }
}