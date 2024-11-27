package com.playdata.orderingservice.common.configs;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreaker;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/*
서킷 브래이커(Circuit Breaker)
- 서비스 호출 실패율이 일정 기준을 넘을 때, 호출을 차단(Open)하여 추가적인 실패를 방지하는 패턴

Open: 실패율이 기준을 초과해서 열림 상태로 전환, 요청을 차단하고 즉시 예외를 변환
Close: 정상상태. 요청이 성공적으로 처리되면 원래의 상태 유지.
Half-Open: 일정 시간이 지나 테스트 요청을 보낸 후 성공 여부에 따라 상태를 결정.
 */

@Configuration
@Slf4j
public class Resilience4JConfig {

    @Bean
    // Resilience4J의 설정을 커스텀할때는 Customizer 인터페이스의 구현체로 리턴해야 한다.
    public Customizer<Resilience4JCircuitBreakerFactory> circuitBreakerFactory() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(4) // 요청 실패율이 설정값을 넘게 되면 서킷브래이커가 Open 됨. %단위
                //실패율 = (실패 요청수 / 총 요청수) x 100,
                .waitDurationInOpenState(Duration.ofMillis(1000))   // Open 상태에서 유지시간 1초
                // 슬라이딩 윈도우(일정한 크기의 요청 집합) 기법을 사용하여 최신 요청 데이터를 지속적으로 갱신
                // COUNT_BASED: 고정된 요청 수를 기반으로 실패율 계산
                // TIME_BASED: 고정된 시간 간격동안 발생한 요청 기준으로 실패율 계산
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(5)   // TIME_BASED 면 초, COUNT_BASED면 회수
                .build();

        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                // 요청 실행 시간이 4초를 초과하면 타임아웃 -> 실패로 간주
                .timeoutDuration(Duration.ofSeconds(4))
                .build();

        // 위에 미리 선언한 config 클래스를 Buoilder로 하나로 합치고, 합친 설정 내용을
        // 최종적으로 factory 메서드로 전달해서 빈으로 등록
        return factory -> factory.configureDefault(
                id -> new Resilience4JConfigBuilder(id)
                .timeLimiterConfig(timeLimiterConfig)
                .circuitBreakerConfig(circuitBreakerConfig)
                .build());
    }
}
