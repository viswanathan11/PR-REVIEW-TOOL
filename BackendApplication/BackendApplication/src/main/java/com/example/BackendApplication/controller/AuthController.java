package com.example.BackendApplication.controller;

import org.apache.catalina.connector.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.BackendApplication.model.User;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @GetMapping("/me")
    public ResponseEntity<User> getMe(@AuthenticationPrincipal User user){
        if(user==null){
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(user);
    }

    @GetMapping("/logout")
    public ResponseEntity<Void> logOut(jakarta.servlet.http.HttpServletResponse response){
        //Create a cookie with the same name, null value and 0 lifespan

        jakarta.servlet.http.Cookie cookie=new jakarta.servlet.http.Cookie("token",null);

        cookie.setHttpOnly(true);
        cookie.setSecure( false);//True in production

        cookie.setPath("/");
        cookie.setMaxAge(0);//Tells browser to delte this cookie immedidately!

        response.addCookie(cookie);

        return ResponseEntity.ok().build();
    }
}
