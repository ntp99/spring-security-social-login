package com.example.social;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.social.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class SpringSecuritySocialApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringSecuritySocialApplication.class, args);
	}

}
