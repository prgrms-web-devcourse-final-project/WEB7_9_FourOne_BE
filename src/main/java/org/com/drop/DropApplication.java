package org.com.drop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DropApplication {

	public static void main(String[] args) {
		SpringApplication.run(DropApplication.class, args);
	}

}
