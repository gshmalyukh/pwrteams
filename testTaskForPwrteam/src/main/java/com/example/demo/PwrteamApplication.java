package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class PwrteamApplication {

	public static void main(String[] args) {
//		Hooks.onOperatorDebug();
		SpringApplication.run(PwrteamApplication.class, args);
	}
}
