package com.github.frehov.testcontainers.jdbc.wait.strategy;

import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;

import java.util.concurrent.TimeUnit;

public class JdbcConnectionReadyWaitStrategy extends AbstractWaitStrategy {
    private static final String DEFAULT_VALIDATION_QUERY = "select 1";
    private static final String TIMEOUT_ERROR = "Timed out waiting for SQL database to be accessible for query execution";

    @SuppressWarnings("rawtypes")
    private static final Class<JdbcDatabaseContainer> CONTAINER_CLASS = JdbcDatabaseContainer.class;

    private String validationQuery = DEFAULT_VALIDATION_QUERY;

    public JdbcConnectionReadyWaitStrategy() {}
    public JdbcConnectionReadyWaitStrategy(String validationQuery) {
        this.validationQuery = validationQuery;
    }

    @Override
    protected void waitUntilReady() {
        Runnable testDatabaseQuery = () -> {
            try (DatabaseDelegate databaseDelegate = this.getDatabaseDelegate()) {
                databaseDelegate.execute(validationQuery, "", 1, false, false);
            }
        };

        // execute validation query until success or timeout is reached
        try {
            Unreliables.retryUntilSuccess((int) this.startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                this.getRateLimiter().doWhenReady(testDatabaseQuery);
                return true;
            });
        } catch (Exception e) {
            throw new ContainerLaunchException(TIMEOUT_ERROR);
        }
    }

    private DatabaseDelegate getDatabaseDelegate() {
        if (CONTAINER_CLASS.isInstance(this.waitStrategyTarget)) {
            return new JdbcDatabaseDelegate(CONTAINER_CLASS.cast(this.waitStrategyTarget), "");
        }
        throw new IllegalArgumentException("Container was not of supported type " + CONTAINER_CLASS.getSimpleName());
    }
}
