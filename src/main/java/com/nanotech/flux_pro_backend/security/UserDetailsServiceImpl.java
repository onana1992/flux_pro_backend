package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.utilisateur.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return utilisateurRepository.findByEmail(username.toLowerCase().trim())
                .map(SecurityUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable: " + username));
    }
}
