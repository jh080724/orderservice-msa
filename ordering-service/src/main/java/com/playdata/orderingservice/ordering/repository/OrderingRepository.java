package com.playdata.orderingservice.ordering.repository;

import com.playdata.orderingservice.ordering.entity.Ordering;
import com.playdata.orderingservice.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderingRepository extends JpaRepository<Ordering, Long> {

    List<Ordering> findByUser(User user);

}

