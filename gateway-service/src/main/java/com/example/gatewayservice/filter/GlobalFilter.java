package com.example.gatewayservice.filter;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RefreshScope
public class GlobalFilter extends AbstractGatewayFilterFactory<GlobalFilter.Config> {

    // Filter가 빈으로 등록될 때부모 클래스의 생성자로
    // 이미 정적(static)으로 세팅된 특정 설정값을 전달
    public GlobalFilter() {
        super(Config.class);
    }

    @Override
    // 부모는 전달받은 설정값을 필터가 동작할 때 (apply가 호출될때) 사용할 수 있도록
    // 매개밗으로 전달해줌. -> apply에서 config에 들어 있는 값으로 필터의 동작등을 제어할 수 있다.
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {   // pre-filter
            // exchange: 현재 요청과 응답에 대한 정보를 담은 객체
            // ServerHttpRequest, ServerHttpResponse 타입의 객체를 다룰 수 있음.

            // pre-filter 동작 로직
            // chain: 게이트웨이 필터 체인
            // 필터 통과 여부를 결정할 수 있음.
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            log.info("GLOBAL filter active! baseMessage = {}", config.getBaseMessage());
            if(config.isPostLogger()){
                log.info("GLOBAL filter Calles!!! Request URI: {}", request.getURI());
            }

            // 요청을 다음 필터로 전달 (필터 통과)
            return chain.filter(exchange).then(Mono.fromRunnable(()->{
                // then 메서드 내부에 필터 체인 처리 완료 후 실행할 post-filter 로직 정의가 가능.
                if(config.isPostLogger()) {
                    log.info("GLOBAL post filter active! response code: {}", response.getStatusCode());
                }
            }));
        };
    }

    @Getter @Setter @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config{
        // 필터 동작을 동적으로 변경하거나 설정하기 위해 사용(선택)
        private String baseMessage;
        private boolean preLogger;
        private boolean postLogger;


    }
}
