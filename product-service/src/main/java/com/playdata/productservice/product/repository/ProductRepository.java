package com.playdata.productservice.product.repository;

import com.playdata.productservice.product.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    List<Product> findByIdIn(List<Long> ids);
}
