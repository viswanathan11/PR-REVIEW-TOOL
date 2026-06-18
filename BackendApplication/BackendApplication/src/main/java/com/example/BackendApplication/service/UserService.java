package com.example.BackendApplication.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.example.BackendApplication.model.User;
import com.example.BackendApplication.repository.UserRepository;

import jakarta.transaction.Transactional;
@Service
public class UserService {
    
    private final UserRepository userRepository;

    //standard construcotr injection instead of using @Aurowired annotation
    // this approach helps in easy testing , immutability(safety) and no cirucular dependencies
    // the springboot automatically asign the bean to this class construcotr.
    // the class must have only one construcotr which is used for dependency injection
    public UserService(UserRepository userRepository){
        this.userRepository=userRepository;
    }


    //Find user by their githubId and update their token/locin if they exist
    @Transactional
    public User findOrCreate(String githubId,String login,String avatarUrl,String accessToken){

        return userRepository.findByGithubId(githubId)
        .map(existing -> {
            existing.setGithubLogin(login);
            existing.setAvatarUrl(avatarUrl);
            existing.setAccessToken(accessToken);
            existing.setUpdatedAt(Instant.now());

            return userRepository.save(existing);
        })
        .orElseGet(()->{
            User newUser =new User();
            newUser.setGithubId(githubId);
            newUser.setGithubLogin(login);
            newUser.setAvatarUrl(avatarUrl);
            newUser.setAccessToken(accessToken);
            newUser.setCreatedAt(Instant.now());
            newUser.setUpdatedAt(Instant.now());
            return userRepository.save(newUser);
        });
    }


    //Fetches a user by their database Id, throwing an exception if the dont exits

    public User findById(Long id){
        return userRepository.findById(id).orElseThrow(()->new RuntimeException("User not found: "+id));
    }

}
