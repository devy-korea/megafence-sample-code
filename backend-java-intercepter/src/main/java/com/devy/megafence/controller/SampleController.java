package com.devy.megafence.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SampleController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/Samples/BackendWithReplace")
    public String replace() {
        return "replace";
    }

    @GetMapping("/Samples/BackendWithLanding")
    public String landingSample() {
        return "landingSample";
    }

    @GetMapping("/Landing")
    public String landing() {
        return "landing";
    }
}
