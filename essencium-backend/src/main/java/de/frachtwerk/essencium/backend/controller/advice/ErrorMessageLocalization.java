package de.frachtwerk.essencium.backend.controller.advice;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class ErrorMessageLocalization {

  @Bean
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource messageSource =
        new ReloadableResourceBundleMessageSource();
    messageSource.addBasenames(
        "classpath:default_translation/translation",
        "classpath:translation/translation",
        "classpath:default_messages/translation",
        "classpath:messages/translation");
    messageSource.setDefaultEncoding("UTF-8");
    return messageSource;
  }

  @Bean
  public LocalValidatorFactoryBean getValidator() {
    LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
    bean.setValidationMessageSource(messageSource());
    return bean;
  }
}
