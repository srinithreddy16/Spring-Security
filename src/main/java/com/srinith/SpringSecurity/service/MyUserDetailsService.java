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
        System.out.println("=== Loading user by username: " + username + " ===");
        try {
            Users user = repo.findByUsername(username);
            System.out.println("Query executed. User object: " + (user != null ? "NOT NULL" : "NULL"));
            
            if(user == null){
                System.out.println("*** User Not Found: " + username + " ***");
                throw new UsernameNotFoundException("user not found: " + username);
            }

            System.out.println("*** User found - ID: " + user.getId() + ", Username: " + user.getUsername() + " ***");
            System.out.println("Password length: " + (user.getPassword() != null ? user.getPassword().length() : "null"));
            System.out.println("Password starts with $2: " + (user.getPassword() != null && user.getPassword().startsWith("$2")));
            return new UserPrincipal(user);
        } catch (Exception e) {
            System.out.println("*** Exception in loadUserByUsername: " + e.getMessage() + " ***");
            e.printStackTrace();
            throw e;
        }
    }
}
