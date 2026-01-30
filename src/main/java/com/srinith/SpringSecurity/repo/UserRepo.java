package com.srinith.SpringSecurity.repo;

import com.srinith.SpringSecurity.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserRepo extends JpaRepository<Users, Integer>{

    Users findByUsername(String username);
}
