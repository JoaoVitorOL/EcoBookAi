package com.ecobook.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "admin.bootstrap")
public class AdminBootstrapProperties {
    private boolean enabled;
    private String email;
    private String password;
    private String nome = "Administrador EcoBook";
}
