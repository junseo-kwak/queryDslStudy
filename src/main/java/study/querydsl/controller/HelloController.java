package study.querydsl.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.entity.Hello;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello(){
        Hello hello = new Hello();
        
        return "hello";

    }

}
