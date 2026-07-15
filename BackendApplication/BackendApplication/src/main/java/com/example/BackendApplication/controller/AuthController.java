package com.example.BackendApplication.controller;

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
        cookie.setSecure(true); // Must be true in production/HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(0); // Tells browser to delete this cookie immediately!
        response.addCookie(cookie);

        // Explicitly set SameSite=None and Secure via header for cross-site cookie deletion
        response.setHeader("Set-Cookie", "token=; Path=/; HttpOnly; Secure; SameSite=None; Max-Age=0");

        return ResponseEntity.ok().build();
    }
}
