package com.github.kchobantonov.camunda.jsonforms.demo.config;

import java.util.List;

import org.camunda.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter;
import org.camunda.bpm.engine.rest.security.auth.impl.HttpBasicAuthenticationProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.util.UrlPathHelper;

@Configuration
public class CamundaRestSecurity {

  @Bean
  public FilterRegistrationBean<CorsFilter> corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    config.setAllowedOrigins(List.of("http://localhost:5173")); // pnpm run camunda-wc:dev frontend origin
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    source.registerCorsConfiguration("/engine-rest/**", config);
    source.registerCorsConfiguration("/forms/**", config);
    UrlPathHelper urlPathHelper = new UrlPathHelper();
    urlPathHelper.setAlwaysUseFullPath(true);
    source.setUrlPathHelper(urlPathHelper);
    
    FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
    bean.setOrder(0); // make sure this runs **before** authentication filter
    return bean;
  }

  // add basic auth to support pnpm run camunda-wc:dev server calls to engine-rest
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
