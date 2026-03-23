package com.example.DtaAssigement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling
@SpringBootApplication
@EnableAspectJAutoProxy
public class  DtaAssigementApplication {


	public static void main(String[] args) {

		SpringApplication.run(DtaAssigementApplication.class, args);
		

	}
}
