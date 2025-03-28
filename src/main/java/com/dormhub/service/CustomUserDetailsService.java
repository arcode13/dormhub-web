package com.dormhub.service;

import com.dormhub.model.User;
import com.dormhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan dengan email: " + email));

        
        String role = "ROLE_" + user.getLevel().getNama().toUpperCase().replace(" ", "_");

        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail()) // Username adalah email
                .password(user.getPassword()) // Password terenkripsi
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(role))) // Tambahkan role
                .build();
    }
}
