package com.example.cart;

import org.springframework.boot.SpringApplication;

public class TestCartApplication {

	public static void main(String[] args) {
		SpringApplication.from(CartApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
