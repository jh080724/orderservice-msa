package com.playdata.orderingservice.ordering.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.orderingservice.common.auth.TokenUserInfo;
import com.playdata.orderingservice.common.dto.CommonResDto;
import com.playdata.orderingservice.ordering.controller.SseController;
import com.playdata.orderingservice.ordering.dto.OrderingListResDto;
import com.playdata.orderingservice.ordering.dto.OrderingSaveReqDto;
import com.playdata.orderingservice.ordering.dto.ProductResDto;
import com.playdata.orderingservice.ordering.dto.UserResDto;
import com.playdata.orderingservice.ordering.entity.OrderDetail;
import com.playdata.orderingservice.ordering.entity.OrderStatus;
import com.playdata.orderingservice.ordering.entity.Ordering;
import com.playdata.orderingservice.ordering.repository.OrderingRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderingService {

    private final OrderingRepository orderingRepository;
    private final SseController sseController;
    private final RestTemplate template;

    // 유레카에 등록된 서비스명으로 요청을 보낼 수 있게 url 틀을 만들어 둠.
    private final String USER_API = "http://user-service/";
    private final String PRODUCT_API = "http://product-service/";

    public Ordering createOrdering(List<OrderingSaveReqDto> dtoList,
                                   TokenUserInfo userInfo) {
        // Ordering 객체를 생성하기 위해 회원 정보를 얻어오기.
        // 우리가 가진 유일한 정보는 토큰안에 들어 있던 이메일 뿐임.
        // 이메일을 가지고 User 정보를 조회 요청을 보내자 -> user-service
        ResponseEntity<CommonResDto> responseEntity = template.exchange(
                USER_API + "findByEmail?email=" + userInfo.getEmail(),
                HttpMethod.GET,
                null,
                CommonResDto.class);

        CommonResDto commonDto = responseEntity.getBody();
//        UserResDto userResDto = (UserResDto) commonDto.getResult();
//
//        // Ordering(주문) 객체 생성
//        Ordering ordering = Ordering.builder()
//                .userId(userResDto.getId())
//                .orderDetails(new ArrayList<>()) // 아직 주문 상세 들어가기 전.
//                .build();
        Map<String, Object> userResDto = (Map<String, Object>) commonDto.getResult();
        int userId = (Integer) userResDto.get("id");

        // Ordering(주문) 객체 생성
        Ordering ordering = Ordering.builder()
                .userId(Long.valueOf(userId))
                .orderDetails(new ArrayList<>()) // 아직 주문 상세 들어가기 전.
                .build();

        // 주문 상세 내역에 대한 처리를 반복문으로 지정.
        for (OrderingSaveReqDto dto : dtoList) {

            // dto에는 상품 고유 id가 있으니까 그걸 활용해서
            // product 객체를 조회하자. --> product-service에게 요청해야함.
            ResponseEntity<CommonResDto> prodResponse = template.exchange(
                    PRODUCT_API + dto.getProductId(),
                    HttpMethod.GET,
                    null,
                    CommonResDto.class);

            CommonResDto commonResDto = prodResponse.getBody();
            Map<String, Object> productResDto = (Map<String, Object>) commonResDto.getResult();
            int stockQuantity = (int) productResDto.get("stockQuantity");
            int prodId = (int) productResDto.get("id");

            // 재고 넉넉하게 있는지 확인.
            int quantity = dto.getProductCount();
            if (stockQuantity < quantity) {
                throw new IllegalArgumentException("재고 부족!");
            }

            // 재고가 부족하지 않다면 재고 수량을 주문 수량만큼 빼 주자.
            // product-service에게 재고 수량이 변경되었다고 알려주자.
            // 상품 id와 변경되어야 할 재고 수량을 함께 보내주자.
            Map<String, String> map = new HashMap<>();
//            map.put("productId", String.valueOf(prodId));
            map.put("productId", String.valueOf(dto.getProductId()));
            map.put("stockQuantity", String.valueOf(stockQuantity - quantity));
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-type", "application/json");

            HttpEntity<Object> httpEntity = new HttpEntity<>(map, headers);

            // 재고수량 변경 요청 보내기
            template.exchange(PRODUCT_API + "updateQuantity",
                    HttpMethod.POST, httpEntity, CommonResDto.class);

            // 주문 상세 내역 엔터티를 생성
            OrderDetail orderDetail = OrderDetail.builder()
                    .productId(dto.getProductId())
                    .ordering(ordering)
                    .quantity(quantity)
                    .build();

            // 주문 내역 리스트에 상세 내역을 add 하기.
            // (cascadeType.PERSIST로 세팅했기 때문에 함께 add가 진행될 것.)
            ordering.getOrderDetails().add(orderDetail);
        } // end forEach

        // Ordering 객체를 save하면 내부에 있는 detail 리스트도 함께 INSERT가 진행이 된다.
        Ordering save = orderingRepository.save(ordering);

        //관리지에게 주문이 생성되었다는 알림을 전송
//        sseController.sendOrderMessage(save);

        return save;
//        return orderingRepository.save(ordering);
    }

//    public List<OrderingListResDto> myOrders(TokenUserInfo userInfo) {
//        /*
//         OrderingListResDto -> OrderDetailDto(static 내부 클래스)
//         {
//            id: 주문번호,
//            userEmail: 주문한 사람 이메일,
//            orderStatus: 주문 상태
//            orderDetails: [
//                {
//                    id: 주문상세번호,
//                    productName: 상품명,
//                    count: 수량
//                },
//                {
//                    id: 주문상세번호,
//                    productName: 상품명,
//                    count: 수량
//                },
//                {
//                    id: 주문상세번호,
//                    productName: 상품명,
//                    count: 수량
//                }
//                ...
//            ]
//         }
//         */
//        String userEmail = userInfo.getEmail();
//        User user = userRepository.findByEmail(userEmail).orElseThrow(
//                () -> new EntityNotFoundException("User Not Found")
//        );
//
//        List<Ordering> orderingList = orderingRepository.findByUser(user);
//
//        // Ordering 엔터티를 DTO로 변환하자. 주문 상세에 대한 변환도 필요하다!
//        List<OrderingListResDto> dtos = orderingList.stream()
//                .map(order -> order.fromEntity())
//                .collect(Collectors.toList());
//
//        return dtos;
//    }
//
//    public List<OrderingListResDto> orderList() {
//        List<Ordering> orderList = orderingRepository.findAll();
//
//        List<OrderingListResDto> dtos = orderList.stream()
//                .map(order -> order.fromEntity())
//                .collect(Collectors.toList());
//
//        return dtos;
//    }
//
//    public Ordering orderCancel(long id) {
//        // 상태를 CANCEL로 변경해 주세요.
//        // 클라이언트에게는 변경 상태와 주문 id만 넘겨 주세요.
//        Ordering ordering = orderingRepository.findById(id).orElseThrow(
//                () -> new EntityNotFoundException("주문 없는데요!")
//        );
//
//        ordering.updateStatus(OrderStatus.CANCELED); // 더티 체킹 (save를 하지 않아도 변경을 감지한다.)
//        return ordering;
//    }
}




















