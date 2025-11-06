package com.hivemq.httpmqttbridge.integration.setup;

import org.testcontainers.containers.PostgreSQLContainer;

public class SharedPostgresContainer extends PostgreSQLContainer<SharedPostgresContainer> {

    private static final String IMAGE_VERSION = "postgres:15";
    private static SharedPostgresContainer container;

    private SharedPostgresContainer() {
        super(IMAGE_VERSION);
        withDatabaseName("testdb");
        withUsername("testuser");
        withPassword("testpass");
        withReuse(true);
    }

    public static SharedPostgresContainer getInstance() {
        if (container == null) {
            container = new SharedPostgresContainer();
            container.start();
        }
        return container;
    }
}