package com.ats.lumax;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EquipmentAlarmServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquipmentAlarmServiceApplication.class, args);
        
        
        
    }
    @Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
 
				
			//	registry.addMapping("/**").allowedOrigins("http://192.168.1.100:8080");
 
				registry.addMapping("/**").allowedOrigins("http://localhost:3000","http://localhost:8090");
			}
		};
    }
    
    @Bean
    public ModelMapper modelMapper()
    {
    	return new ModelMapper();
    }
}

