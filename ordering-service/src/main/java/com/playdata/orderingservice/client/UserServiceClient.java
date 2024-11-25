package com.playdata.orderingservice.client;

import com.playdata.orderingservice.common.dto.CommonResDto;
import com.playdata.orderingservice.ordering.dto.UserResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service")  // 호출하고자 하는 서비스의 유레카 등록된 서비스 이름
public interface UserServiceClient {

    // 바디가 없는 추상 메서드 작성
    @GetMapping("/findByEmails")
    CommonResDto<UserResDto> findByEmail(@RequestParam String email);

    @PostMapping("/users/email")
    CommonResDto<List<UserResDto>> getUsersByIds(@RequestBody List<Long> userIds);
}
