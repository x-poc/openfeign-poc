# openfeign-poc

## 背景

分享一下最近踩过的一个坑，背景是这样的：

为了消减开源组件所带来的安全风险，项目中会定期审视并升级三方开源组件。我们的项目中使用了 Spring Cloud，版本是 `Hoxton.SR1`，存在已知的漏洞，官方建议升级到最新的 `Hoxton.SR7`（其实际对应的版本从 `2.2.1.RELEASE` 升到了 `2.2.3.RELEASE`）。由于只是一个 Semantic Versioning 语义上的修订版本，Spring 本身的兼容性也做得非常好，跑完 UT 就发到测试环境上去了。

测试中发现了一个奇怪的现象，一个外部调用服务总是报 feign 调用异常，本地调试打断点拿到变量值，手动去模拟请求又是正常的，百思不得其解，由于修改了多处地方，逐一排除后定位到了 Spring Cloud OpenFeign 的版本本身：只需将版本回退，异常就会消失。

## 这是 Spring Cloud 的 bug 吗？

通过抓包工具对比请求报文差异，发现原因居然是 Spring Cloud OpenFeign 把我们的请求内容进行了修改：header 参数中的 `','` 被修改为 `', '`. 以下是这个漏洞的 POC，我提交给了 Spring Cloud OpenFeign 官方。

### Describe the bug

- spring-cloud-openfeign >= `2.2.3.RELEASE` (`Hoxton.SR5`)
- springframework `5.2.12.RELEASE`
- springboot `2.3.7.RELEASE`
- io.github.openfeign `10.10.1`
- jdk `1.8.0_212`

Describe: spring-cloud-openfeign version >= `2.2.3.RELEASE` @RequestHeader will repace `','` to `', '` (One more white space), but it's ok in `2.2.2.RELEASE`.

### Sample

```
"Wed, 23 Dec 2020 02:34:12 GMT"
```

was replaced by

```
"Wed,  23 Dec 2020 02:34:12 GMT"
```

One more white space!

This causes some APIs authentication to fail!

My POC:

```java
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
```

```java
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
```

```java
package com.devyy.poc.openfeignmock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients("com.devyy.poc.openfeignmock.feign")
@SpringBootApplication
public class OpenfeignMockApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpenfeignMockApplication.class, args);
    }
}
```

pom.xml:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.3.7.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.devyy.poc</groupId>
    <artifactId>openfeign-mock</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>openfeign-mock</name>
    <description>Demo project for Spring Boot</description>

    <properties>
        <java.version>1.8</java.version>
        <spring-cloud.version>Hoxton.SR5</spring-cloud.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

application.properties:

```properties
server.port=8888
```

---

My unit test:

```java
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
```

My logs:

OpenfeignMockApplicationTests logs:

```log
2020-12-23 11:53:16.294  INFO 32812 --- [           main] c.d.p.o.OpenfeignMockApplicationTests    : testWhiteSpace req=Wed, 23 Dec 2020 02:34:12 GMT
2020-12-23 11:53:16.297  INFO 32812 --- [           main] c.d.p.o.OpenfeignMockApplicationTests    : testWhiteSpace res=Wed,  23 Dec 2020 02:34:12 GMT
```

PocController logs:

```log
2020-12-23 11:53:16.134  INFO 39676 --- [nio-8888-exec-1] c.d.p.o.controller.PocController         : PocController requestDate=Wed,  23 Dec 2020 02:34:12 GMT
```

然后由于时逢圣诞，老外放长假。一时间得不到 Spring Cloud 的官方回复，便自己 run 起框架，调试了起来，最后定位到问题出在 OpenFeign 框架的一行代码：

https://github.com/OpenFeign/feign/blob/04bd147fba11bdf1cac26aba1d64d189e0867a3f/core/src/main/java/feign/template/HeaderTemplate.java#L151

```java
  @Override
  public String expand(Map<String, ?> variables) {
    String result = super.expand(variables);

    /* remove any trailing commas */
    while (result.endsWith(",")) {
      result = result.replaceAll(",$", "");
    }

    /* space all the commas now */
    result = result.replaceAll(",", ", ");
    return result;
  }
```

问题已经相当明显，我通过 patch 的方式对问题进行了修复，但终究没想明白作者的意图。于是我又去 OpenFeign 项目下提交了 issue 和 pr 并尝试联系代码作者 Kevin Davis 进行确认。Kevin Davis 是 OpenFeign 项目的核心 Contributor. 通过一番深（xing）入（shi）交（wen）流（zui），作者承认这是一个 bug，是为了修改另一个 bug 而引入的新 bug：

> I'm beginning to see that we'll have to "bite the bullet" and upgrade the Template handling sooner than Feign 11. The root cause of all of these issues is that our Template Handling relies on String manipulation for templates that can support Collections of values. Single values work fine.
>
> Since HTTP Headers are generally considered Collection based, this problem will continue to appear. I can see that in #1298 an attempt is make to clear out extra spaces, but that solution will work for the example provided only and not generally across all headers. I'm going to link this to the template update issue in #1019 and see if we can start by separating general URI templates with Headers first.

最后，作者从整体考虑，没有采用我 patch 式的 pr（一个小遗憾），而是对相关实现进行了重写，将普通 URI 模板和 header 进行了分离，解决问题的同时优化了架构，并补全了对应的的 UT 用例。

## 参考链接

- https://github.com/spring-cloud/spring-cloud-openfeign/issues/450
- https://github.com/OpenFeign/feign/issues/1339
- https://github.com/OpenFeign/feign/issues/1270
- https://github.com/OpenFeign/feign/pull/1344
- https://github.com/OpenFeign/feign/pull/1347
