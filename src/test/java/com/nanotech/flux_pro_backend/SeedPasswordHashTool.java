package com.nanotech.flux_pro_backend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Dev-only helper to print a BCrypt hash for manual SQL seed fixes. */
public final class SeedPasswordHashTool {

    private SeedPasswordHashTool() {
    }

    public static void main(String[] args) {
        String password = args.length > 0 ? args[0] : "Mintp@2025";
        System.out.println(new BCryptPasswordEncoder(12).encode(password));
    }
}
