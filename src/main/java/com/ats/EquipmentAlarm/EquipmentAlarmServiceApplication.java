package com.ats.EquipmentAlarm;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EquipmentAlarmServiceApplication extends SpringBootServletInitializer {
 
    
    public static void main(String[] args) {
        SpringApplication.run(EquipmentAlarmServiceApplication.class, args);
    }
    

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {


				// registry.addMapping("/**").allowedOrigins("http://192.168.1.100:8080");
//			registry.addMapping("/**").allowedOrigins("*").allowedMethods("*");
		registry.addMapping("/**").allowedOrigins("http://10.10.56.33:8080").allowedMethods("*");
			}
		};

	}
	
     
    @Bean
    public ModelMapper modelMapper()
    {
    	return new ModelMapper();
    }
} 

