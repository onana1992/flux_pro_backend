package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final RbacAuthorityService rbacAuthorityService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmailWithRolesAndOrganization(username.toLowerCase().trim())
                .map(user -> new SecurityUser(user, rbacAuthorityService.resolve(user)))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
