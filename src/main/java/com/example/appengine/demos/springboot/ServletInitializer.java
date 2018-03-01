package com.example.appengine.demos.springboot;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@Configurable
public class ServletInitializer extends SpringBootServletInitializer {

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(SpringBootExampleApplication.class);
  }

}
