package com.example.BackendApplication.service;

import java.nio.charset.StandardCharsets;
import java.sql.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.example.BackendApplication.config.AppProperties;
import com.nimbusds.jose.util.StandardCharset;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final AppProperties props;

    public JwtService(AppProperties props){
        this.props=props;
    }

    private SecretKey getKey(){
        return Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharset.UTF_8));
    }

    //Generates a HMAC SHA secret key from the JWT 
    // secret string in our properites

    private SecretKey gtKey(){
        return Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }


    //String generate (): This function Gnerates a token signed with our key carrying the UserId(subject) 
    // and Github Login

    public String generate(Long userId,String githubLogin){
        long nowMs=System.currentTimeMillis();
        long expiryMs=nowMs+(props.getJwt().getExpiryHours() * 3600_00L);

        return Jwts.builder()
        .subject(userId.toString())
        .claim("login",githubLogin)
        .issuedAt(new Date(nowMs))
        .expiration(new Date(expiryMs))
        .signWith(getKey())
        .compact();
    }

    //Validates and parses the claims out of an incoming JWT token

    public Claims validate(String token){
        return Jwts.parser()
        .verifyWith(getKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
    }

    //Extracts the userId(Subject) from a validated token
    public Long extractUserId(String token){
        return Long.parseLong(validate(token).getSubject());
    }
}
