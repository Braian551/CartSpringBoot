package com.example.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cart.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Integer> {

}
