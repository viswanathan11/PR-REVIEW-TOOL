package com.example.BackendApplication.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;





@Configuration
@ConfigurationProperties(prefix="app")
public class AppProperties {
    private String baseUrl;
    private String frontendUrl;
    private final Jwt jwt = new Jwt();
    private final Webhook webhook =new Webhook();
    private final Ai ai=new Ai();


    // ==========================================
    // 1. Nested Static Config Classes
    // ==========================================

    public static class Jwt{
        private String secret;
        private int expiryHours;

        public String getSecret(){
            return secret;
        }

        public void setSecret(String secret){
            this.secret=secret;
        }

        public int getExpiryHours(){
            return expiryHours;
        }

        public void setExpiryHours(int expiryHours){
            this.expiryHours=expiryHours;
        }

    }


    public static class Webhook{
        private String secret;

        public String getSecret(){
            return secret;
        }

        public void setSecret(String secret){
            this.secret=secret;
        }
    }

    public static class Ai{
        private String anthropicApiKey;
        private String model;
        private int maxTokens;
        private long maxDiffChars;

        public String getAnthropicApiKey(){
            return anthropicApiKey;
        }

        public void setAnthropicApiKey(String anthropicApiKey){
            this.anthropicApiKey=anthropicApiKey;
        }

        public String getModel(){
            return model;
        }

        public void setModel(String model){
            this.model=model;
        }

        public int getMaxTokens(){
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens){
            this.maxTokens=maxTokens;
        }

        public long getMaxDiffChars(){
            return maxDiffChars;
        }

        public void setMaxDiffChars(long maxDiffChars){
            this.maxDiffChars=maxDiffChars;
        }
    }
    // ==========================================
    // 2. Getters and Setters for Parent Class
    // =========================================
    public String getBaseUrl(){
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl){
        this.baseUrl=baseUrl;
    }

    public String getFrontendUrl(){
        return frontendUrl;
    }

    public void setFrontendUrl(String frontendUrl){
        this.frontendUrl=frontendUrl;
    }

    public Jwt getJwt(){
        return jwt;
    }

    public Webhook getWebhook(){
        return webhook;
    }

    public Ai getAi(){
        return ai;
    }


}
