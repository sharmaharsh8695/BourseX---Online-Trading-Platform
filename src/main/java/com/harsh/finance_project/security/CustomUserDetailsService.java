package com.harsh.finance_project.security;

import com.harsh.finance_project.user.model.User;
import com.harsh.finance_project.user.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {


    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("User not found"));
        return new CustomUserDetails(user);
    }
}

