package io.grpc.examples.routeguide.spring;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A simple controller, response to HTTP request
 *
 * @author yan_h
 * @since 2018-04-18.
 */
@RestController
@RequestMapping("/ping")
public class RouteGuideController {

    @RequestMapping("")
    public String hello() {
        return "Hello!";
    }
}
