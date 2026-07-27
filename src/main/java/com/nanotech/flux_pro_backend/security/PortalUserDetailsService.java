package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.repository.PortalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortalUserDetailsService {

    private final PortalUserRepository portalUserRepository;

    public PortalSecurityUser loadByEmail(String email) {
        return portalUserRepository.findByEmailIgnoreCase(email)
                .map(PortalSecurityUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("Portal user not found: " + email));
    }

    public PortalSecurityUser loadById(UUID id) {
        return portalUserRepository.findById(id)
                .map(PortalSecurityUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("Portal user not found: " + id));
    }
}
