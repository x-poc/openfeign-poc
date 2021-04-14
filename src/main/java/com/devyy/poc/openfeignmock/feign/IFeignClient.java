package com.devyy.poc.openfeignmock.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name = "feignClient", url = "http://127.0.0.1:8888/")
public interface IFeignClient {
    @RequestMapping(value = "/poc", method = RequestMethod.POST)
    String testWhiteSpace(@RequestHeader("requestDate") String requestDate);
}