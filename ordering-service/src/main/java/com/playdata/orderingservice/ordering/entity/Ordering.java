package com.playdata.orderingservice.ordering.entity;

import com.playdata.orderingservice.ordering.dto.OrderingListResDto;
import com.playdata.orderingservice.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
public class Ordering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.ORDERED;

    @OneToMany(mappedBy = "ordering", cascade = CascadeType.PERSIST)
    private List<OrderDetail> orderDetails;


    public OrderingListResDto fromEntity() {

        // DB에서 조회해 온 Ordering에서 상세 내역을 확인합니다.
        List<OrderDetail> orderDetailList = this.getOrderDetails();
        List<OrderingListResDto.OrderDetailDto> orderDetailDtos = new ArrayList<>();

        // OrderDetail 엔터티를 OrderDetailDto로 변환합시다.
        // 변환한 후에는 리스트에 추가합니다.
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetailDtos.add(orderDetail.fromEntity());
        }

        // 주문 상세 내역 dto 포장이 완료되면 하나의 주문 내역 자체를 dto로 변환해서 리턴.
        return OrderingListResDto.builder()
                .id(this.id)
                .userEmail(this.user.getEmail())
                .orderStatus(this.orderStatus)
                .orderDetails(orderDetailDtos)
                .build();
    }

    public void updateStatus(OrderStatus status) {
        this.orderStatus = status;
    }

}













