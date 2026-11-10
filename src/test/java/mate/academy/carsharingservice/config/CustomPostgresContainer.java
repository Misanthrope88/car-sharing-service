package mate.academy.carsharingservice.config;

import org.testcontainers.postgresql.PostgreSQLContainer;

public class CustomPostgresContainer extends PostgreSQLContainer {
    private static final String IMAGE = "postgres:16-alpine";
    private static final String DATABASE_NAME = "car_sharing_test";
    private static final String USERNAME = "test";
    private static final String PASSWORD = "test";

    private static CustomPostgresContainer container;

    private CustomPostgresContainer() {
        super(IMAGE);
        withDatabaseName(DATABASE_NAME);
        withUsername(USERNAME);
        withPassword(PASSWORD);
    }

    public static synchronized CustomPostgresContainer getInstance() {
        if (container == null) {
            container = new CustomPostgresContainer();
            container.start();
        }
        return container;
    }
}
