package de.frachtwerk.essencium.backend.configuration;

import de.frachtwerk.essencium.backend.configuration.properties.EssenciumErrorProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EssenciumErrorProperties.class)
public class EssenciumErrorConfiguration {}