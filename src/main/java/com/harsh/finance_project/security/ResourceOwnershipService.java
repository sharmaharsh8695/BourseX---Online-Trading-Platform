package com.harsh.finance_project.security;

import com.harsh.finance_project.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ResourceOwnershipService {
    private final UserRepository userRepository;

    public ResourceOwnershipService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void requireOwner(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Access denied");
        }

        Long authenticatedUserId = userRepository.findByEmail(authentication.getName())
                .map(user -> user.getId())
                .orElseThrow(() -> new AccessDeniedException("Access denied"));

        if (!authenticatedUserId.equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
