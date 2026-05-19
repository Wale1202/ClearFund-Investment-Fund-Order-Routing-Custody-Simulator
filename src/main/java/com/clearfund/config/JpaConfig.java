package com.clearfund.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing so {@code @CreatedDate} / {@code @LastModifiedDate}
 * timestamps on {@link com.clearfund.entity.Auditable} are populated automatically.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
