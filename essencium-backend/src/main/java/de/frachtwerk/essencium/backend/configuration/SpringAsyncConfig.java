package de.frachtwerk.essencium.backend.configuration;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync(mode = AdviceMode.PROXY, proxyTargetClass = true)
public class SpringAsyncConfig {}
