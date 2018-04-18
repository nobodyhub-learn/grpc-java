package io.grpc.examples.routeguide.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * A web server
 *
 * @author yan_h
 * @since 2018-04-18.
 */
@SpringBootApplication
@SpringBootConfiguration
@ComponentScan
@EnableWebMvc
public class RouterGuideSpringApplication {
    public static void main(String[] args) {
        SpringApplication.run(RouterGuideSpringApplication.class, args);
    }
}
