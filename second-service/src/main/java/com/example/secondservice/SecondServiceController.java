package com.example.secondservice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/second-service")
@RequiredArgsConstructor
@Slf4j
public class SecondServiceController {

//    @Value("server.port")
//    private String port;
    private final Environment env;

    @GetMapping("/welcome")
    public String welcome(){
        return "Welcome to SECONDService!!!";
    }

    @GetMapping("/message")
    public String message(@RequestHeader("second-request") String header) {
        // @RequestHeader -> 헤더에 들어 있는 정보를 바로 꺼낼 수 있게 해줌.
        log.info(header);
        return "<h1>Hello message from SECOND service</h1>";
    }

    @GetMapping("/check")
    public String check(HttpServletRequest request) {
        log.info("Server Port: {}", request.getServerPort());
        return "<h1>Hello, SECOND check!!! Ther server Port is" + env.getProperty("server.port") + "</h1>";
    }
}
