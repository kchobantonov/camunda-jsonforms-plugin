package com.github.kchobantonov.camunda.jsonforms.demo.config;

import java.io.IOException;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class CssEncodingConfig {

  @Bean
  public FilterRegistrationBean<Filter> cssCharsetFilter() {
    FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();

    registrationBean.setFilter(new Filter() {
      @Override
      public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
          throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest &&
            response instanceof HttpServletResponse httpResponse) {

          String requestUri = httpRequest.getRequestURI();
          if (requestUri.endsWith(".css")) {
            httpResponse.setContentType("text/css; charset=UTF-8");
          }
        }
        chain.doFilter(request, response);
      }
    });

    registrationBean.addUrlPatterns("*.css"); // Only intercept CSS files
    registrationBean.setName("CssCharsetFilter");
    registrationBean.setOrder(1); // Optional: lower number means higher priority

    return registrationBean;
  }
}
