package com.iodesystems.db.example;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

  private static final Logger LOG = getLogger(Application.class);

  public static void main(
      String[] args) throws Exception {
    SpringApplication.run(Application.class, args);

  }

}
