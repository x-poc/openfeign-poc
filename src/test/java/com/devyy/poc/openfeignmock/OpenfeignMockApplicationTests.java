package com.devyy.poc.openfeignmock;

import com.devyy.poc.openfeignmock.feign.IFeignClient;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class OpenfeignMockApplicationTests {
    @Autowired
    private IFeignClient feignClient;

    @Test
    void contextLoads() {
        String req = "Wed, 23 Dec 2020 02:34:12 GMT";
        String res = feignClient.testWhiteSpace(req);
        log.info("testWhiteSpace req={}", req);
        log.info("testWhiteSpace res={}", res);
    }
}