package com.devyy.poc.openfeignmock.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/poc")
public class PocController {

    @PostMapping
    public String poc(@RequestHeader("requestDate") String requestDate) {
        log.info("PocController requestDate={}", requestDate);
        return requestDate;
    }
}