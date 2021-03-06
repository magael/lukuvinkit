package ohtu.takarivi.lukuvinkit.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import ohtu.takarivi.lukuvinkit.domain.CustomUser;
import ohtu.takarivi.lukuvinkit.repository.CustomUserRepository;

/**
 * Represents an user details service for Spring Data JPA. This handles users that are defined as CustomUser instances.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private CustomUserRepository customUserRepository;

    /**
     * Initializes this details service.
     */
    @PostConstruct
    public void init() {
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        CustomUser customUser = customUserRepository.findByUsername(username);
        if (customUser == null) {
            throw new UsernameNotFoundException("No such user: " + username);
        }
        return User.withUsername(customUser.getUsername()).password(customUser.getPassword()).roles("USER").build();
    }

}
