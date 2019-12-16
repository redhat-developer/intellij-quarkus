package org.acme;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/hello")
public class ExampleResource {

    @GetMapping
    public String hello() {
        return "hello";
    }
}