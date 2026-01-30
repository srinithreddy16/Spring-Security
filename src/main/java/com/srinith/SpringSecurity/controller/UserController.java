package com.srinith.SpringSecurity.controller;

import com.srinith.SpringSecurity.model.Users;
import com.srinith.SpringSecurity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    private UserService service;


    @PostMapping("/register")
    public Users register(@RequestBody Users user){
        System.out.println("Register endpoint called with username: " + user.getUsername());
        return service.register(user);
    }
}
