package com.example.gatewayservice.filter;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class LoggerFilter extends AbstractGatewayFilterFactory<LoggerFilter.Config> {

    // Filter가 빈으로 등록될 때부모 클래스의 생성자로
    // 이미 정적(static)으로 세팅된 특정 설정값을 전달
    public LoggerFilter() {
        super(Config.class);
    }

    @Override
    // 부모는 전달받은 설정값을 필터가 동작할 때 (apply가 호출될때) 사용할 수 있도록
    // 매개밗으로 전달해줌. -> apply에서 config에 들어 있는 값으로 필터의 동작등을 제어할 수 있다.
    public GatewayFilter apply(Config config) {
        // 필터의 우선순위를 적용하고 싶다면 OrderedGatewayFilter를 직접 생성한다.
        // 첫번째 매개값을 익명 객체 선언, 두번째 매개값으로 우선순위 상수를 넘긴다.
        // 만역 여러 필터에 순위를 매기고 싶다면 정수로 지정한다. (낮은 숫자일 수록 우선순위 높음)
        GatewayFilter filter = new OrderedGatewayFilter((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            log.info("Logger filter active! baseMessage = {}", config.getBaseMessage());
            if (config.isPreLogger()) {
                log.info("Logger Filter called! Request URI: {}", request.getURI());
            }

            // 요청을 다음 필터로 전달 (필터 통과)
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                // then 메서드 내부에 필터 체인 처리 완료 후 실행할 post-filter 로직 정의가 가능.
                if (config.isPostLogger()) {
                    log.info("Logger Post Filter active! response code: {}", response.getStatusCode());
                }
            }));

        }, Ordered.HIGHEST_PRECEDENCE);
//        }, Ordered.LOWEST_PRECEDENCE);
//        }, Ordered.1);
//        }, Ordered.2);
        return filter;
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
