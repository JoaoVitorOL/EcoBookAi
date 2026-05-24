package com.ecobook.config;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

/**
 * Local H2 dialect that treats PostgreSQL named enums as VARCHAR-backed domains.
 * This keeps the production PostgreSQL mapping intact while allowing the local
 * profile to boot against H2 with the same entity metadata.
 */
public class LocalH2Dialect extends H2Dialect {

    /**
     * Registers the custom H2 compatibility types required by the local profile.
     * @param typeContributions Hibernate type contributions to extend
     * @param serviceRegistry Hibernate service registry in use
     */
    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);

        JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
        jdbcTypeRegistry.addDescriptor(SqlTypes.NAMED_ENUM, VarcharJdbcType.INSTANCE);
    }
}
