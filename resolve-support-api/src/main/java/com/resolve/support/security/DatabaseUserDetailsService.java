package com.resolve.support.security;

import com.resolve.support.account.entity.User;
import com.resolve.support.account.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private static final java.util.regex.Pattern BCRYPT_HASH =
            java.util.regex.Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.*$");

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));
        if (!BCRYPT_HASH.matcher(user.getPassword()).matches()) {
            throw new UsernameNotFoundException("Invalid email or password.");
        }
        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword()).roles(user.getRole().name()).build();
    }
}
