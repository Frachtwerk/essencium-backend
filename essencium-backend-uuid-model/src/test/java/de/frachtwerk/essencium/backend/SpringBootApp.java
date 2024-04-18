package de.frachtwerk.essencium.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"de.frachtwerk.essencium.backend"})
@EntityScan(basePackages = {"de.frachtwerk.essencium.backend"})
@ConfigurationPropertiesScan(basePackages = {"de.frachtwerk.essencium.backend"})
@EnableJpaRepositories(basePackages = {"de.frachtwerk.essencium.backend"})
public class SpringBootApp {
  public static void main(String[] args) {
    SpringApplication.run(SpringBootApp.class, args);
  }
}
