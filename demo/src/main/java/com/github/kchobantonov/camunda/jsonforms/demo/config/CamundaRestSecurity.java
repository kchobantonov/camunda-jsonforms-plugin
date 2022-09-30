package com.github.kchobantonov.camunda.jsonforms.demo.config;

import org.camunda.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter;
import org.camunda.bpm.engine.rest.security.auth.impl.HttpBasicAuthenticationProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamundaRestSecurity {
  // add basic auth so that we can demo from a webpack UI server
  @Bean
  public FilterRegistrationBean<ProcessEngineAuthenticationFilter> processEngineAuthenticationFilter() {
    FilterRegistrationBean<ProcessEngineAuthenticationFilter> registration = new FilterRegistrationBean<>();
    registration.setName("camunda-auth");
    registration.setFilter(new ProcessEngineAuthenticationFilter());
    registration.addInitParameter("authentication-provider", HttpBasicAuthenticationProvider.class.getName());
    registration.addUrlPatterns("/engine-rest/*");
    return registration;
  }
}
