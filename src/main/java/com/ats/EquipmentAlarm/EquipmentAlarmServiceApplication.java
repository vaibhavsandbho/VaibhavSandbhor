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
<<<<<<< Updated upstream
			registry.addMapping("/**").allowedOrigins("*").allowedMethods("*");
=======
			registry.addMapping("/**").allowedOrigins("http://localhost:4200","http://localhost:3000","http://localhost:3004", "http://localhost:8090","http://192.168.10.122:3000","http://192.168.10.122:3004","http://10.255.20.26:8080","http://10.255.20.26:80","http://10.255.20.27:80","http://10.255.20.27:8080","http://192.168.10.102:8080","http://localhost:8080","http://localhost","http://192.168.10.102:80","http://192.168.10.102:8080","http://192.168.11.154:8080");
>>>>>>> Stashed changes
	//	registry.addMapping("/**").allowedOrigins("http://192.168.10.115s:8080");
			}
		};

	}
	
     
    @Bean
    public ModelMapper modelMapper()
    {
    	return new ModelMapper();
    }
} 

