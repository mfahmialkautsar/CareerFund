package id.careerfund.api.configurations;

import id.careerfund.api.domains.entities.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;


@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {
    @Bean
    public AuditorAware<Long> auditorProvider() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return () -> {
            if (authentication != null) {
                User user = (User) authentication.getPrincipal();
                return Optional.of(user.getId());
            }
            return Optional.empty();
        };
    }
}
