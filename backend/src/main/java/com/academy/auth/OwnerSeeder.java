package com.academy.auth;

import com.academy.config.AcademyProperties;
import com.academy.user.AppUser;
import com.academy.user.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures the single owner account exists and matches the configured credentials.
 * Changing ACADEMY_OWNER_PASSWORD and restarting rotates the password.
 * There is deliberately no sign-up endpoint.
 * Runs after all beans exist but before the web server starts, so no login can race the seeding.
 */
@Component
public class OwnerSeeder implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(OwnerSeeder.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final AcademyProperties props;

    public OwnerSeeder(AppUserRepository users, PasswordEncoder encoder, AcademyProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void afterSingletonsInstantiated() {
        var owner = props.owner();
        String displayName = owner.displayName() == null || owner.displayName().isBlank()
                ? owner.username() : owner.displayName();
        users.findByUsername(owner.username()).ifPresentOrElse(u -> {
            if (!encoder.matches(owner.password(), u.getPasswordHash())) {
                u.setPasswordHash(encoder.encode(owner.password()));
                log.info("Owner password rotated from configuration");
            }
            u.setDisplayName(displayName);
        }, () -> {
            users.save(new AppUser(owner.username(), encoder.encode(owner.password()), displayName));
            log.info("Owner account '{}' created", owner.username());
        });
    }
}
