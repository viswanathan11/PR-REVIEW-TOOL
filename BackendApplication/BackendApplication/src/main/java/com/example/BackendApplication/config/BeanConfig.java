package com.example.BackendApplication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
/*
This BeanConfig.java will provide the necessary Objects

RestTemplate : to make http requests to the Github Api
Jackson's objectMapper : to serialize/deserialize JSON payloads.
*/
@Configuration
public class BeanConfig {
    @Bean 
    public RestTemplate restTenokate(){
        return new RestTemplate();
    }

    @Bean 
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }
}
