package com.srinith.SpringSecurity.service;

import com.srinith.SpringSecurity.model.UserPrincipal;
import com.srinith.SpringSecurity.model.Users;
import com.srinith.SpringSecurity.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepo repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        System.out.println("Loading user by username: " + username);
        Users user = repo.findByUsername(username);

        if(user == null){
            System.out.println("User Not Found: " + username);
            throw new UsernameNotFoundException("user not found");
        }

        System.out.println("User found - ID: " + user.getId() + ", Username: " + user.getUsername() + ", Password: " + user.getPassword());
        return new UserPrincipal(user);
    }
}
