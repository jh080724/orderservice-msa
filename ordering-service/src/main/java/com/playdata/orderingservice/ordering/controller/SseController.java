package com.playdata.orderingservice.ordering.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.orderingservice.common.auth.TokenUserInfo;
import com.playdata.orderingservice.ordering.dto.UserResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@Slf4j
// Sse(Server Sent Event) Controller
public class SseController {

    // Redis 채널명을 하나로 고정
    // admin1, admin2, admin3 처럼 여러 명의 관리자가 서로 다른 인스턴스에 존재하더라도
    // 같은 채널을 구독하게 함으로써, 주문이 들어 왔을 때 모든 관리자가 메시지를 받을 수 있게끔 처리
    private static final String REDIS_CHANNEL = "admin-notification";

    // 구독을 요청한 각 사용자의 이메일을 키로 하여 emitter 객체를 저장.
    // ConcurrentHashMap: 멀티 스레드 기반 해시맵 (HashMap은 싱글 스레드 기반)
//    Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Qualifier("sse-template") // sse 전용 레디스 빈을 주입
    private final RedisTemplate<String, Object> sseRedisTemplate;

    private final RedisMessageListenerContainer redisMessageListenerContainer;


    // 관리자가 로그인하면 서버와의 연결을 위해 자동 요청 처리(react)
    @GetMapping("/subscribe")
    public SseEmitter subscribe(@AuthenticationPrincipal TokenUserInfo userInfo) {

        SseEmitter emitter = new SseEmitter(1440 * 60 * 1000L); // 알림 서비스 구현 핵심 객체
        String email = userInfo.getEmail();

        log.info("Subscribing to {}:", email);

        // 클라이언트가 연결을 끊거나, emitter의 수명이 다하면 맵에서 제거
        emitter.onCompletion(() -> log.info("SSE Connection completed: {}", email));
        emitter.onTimeout(() -> log.info("SSE Connection timed out: {}", email));

        // 연결 성공 메세지 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected!!!"));

            // 30초마다 heartbeat 메시지를 전송하여 연결 유지
            // 클라이언트에서 사용하는 EventSourcePolyfill이 45초 동안 활동이 없으면, 자체 연결 종료를 방지 ??
            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data("keep-alive")); // 클라이언트 단이 살아 있는지 확인
                } catch (IOException e) {
                    log.warn("Failed to send heartbeat, removing emitter for email: {}", email);
                }
            }, 30, 30, TimeUnit.SECONDS); // 30초마다 heartbeat 메시지 전송

            // redis에 대해서도 subscribe를 진행하자.
            subscribeChannel(emitter);

        } catch (IOException e) {
            log.error("Fail to send connect message to admin: {}", email);
            log.error(e.getMessage());
        }

        return emitter;

    }

    // Redis 메시지를 리스너에 등록하는 메서드
    private void subscribeChannel(SseEmitter emitter) {
        // 메세지가 수신된다면 어떤 객체의 어떤 메서드로 처리할 것인지를 객체 생성때 알려줘야 한다.
        MessageListenerAdapter adapter
                = new MessageListenerAdapter(new RedisMessageSubscriber(emitter), "onMessage");
        redisMessageListenerContainer.addMessageListener(adapter, new PatternTopic(REDIS_CHANNEL));
    }

    public void sendOrderMessage(UserResDto userDto) {
        // 주문처리가 완료되면 호출되는 메서드.
        // Redis 에 주문이 되었다고 메시지를 쏴 주자.
        log.info("Notification sent to Redis: {}", userDto);
        sseRedisTemplate.convertAndSend(REDIS_CHANNEL, userDto);
    }

    /*
    Redis 메시지를 수신하여 클라이언트로 알림을 전송하는 내부 클래스
     */
    public static class RedisMessageSubscriber implements MessageListener {
        private final SseEmitter emitter;

        public RedisMessageSubscriber(SseEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public void onMessage(Message message, byte[] pattern) {  // pub되면 자동호출
            // message 내용을 parsing (json -> java 객체로)
            ObjectMapper objectMapper = new ObjectMapper();

            try {
                UserResDto dto
                        = objectMapper.readValue(message.getBody(), UserResDto.class);//Dto 없는 경우에는 MAP이나 string으로 받아도 됨,.

                // 만약 채널명이 필요하면 pattern에서 얻기, 지금은 채널이 1개라 필요가 없음.
                String s = new String(pattern, "UTF-8");

                // 누구에게 메시지를 전달할 지 알려줘야 한다.(admin@admin.com이 받는다고 가정)
//            SseEmitter emitter = emitters.get("admin@admin.com");
                emitter.send(SseEmitter.event()
                        .name("ordered")
                        .data(dto));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }


}
