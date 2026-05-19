package com.clearfund.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settlement simulation tuning, bound from the {@code clearfund.settlement.*}
 * section of application.yml.
 */
@ConfigurationProperties(prefix = "clearfund.settlement")
public record SettlementProperties(@DefaultValue("2") int offsetDays) {
}
