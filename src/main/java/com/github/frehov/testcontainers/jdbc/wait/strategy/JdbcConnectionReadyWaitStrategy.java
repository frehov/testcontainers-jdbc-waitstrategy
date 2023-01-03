package com.github.frehov.testcontainers.jdbc.wait.strategy;

import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;

import java.util.concurrent.TimeUnit;

public class JdbcConnectionReadyWaitStrategy extends AbstractWaitStrategy {

    private final static Logger logger = LoggerFactory.getLogger(JdbcConnectionReadyWaitStrategy.class);

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
        if (!isWaitStrategyTargetSupported()) {
            logger.trace("Unable to downcast \"{}\" to \"{}\"", this.waitStrategyTarget.getClass().getName(), CONTAINER_CLASS.getName());
            throw new IllegalArgumentException(String.format(
                    "Container [%s] was not castable to type: %s", this.waitStrategyTarget.getClass().getName(), CONTAINER_CLASS.getName())
            );
        }
        JdbcDatabaseContainer<?> container = CONTAINER_CLASS.cast(this.waitStrategyTarget);
        logger.debug("Executing connection validation-query \"{}\" on database \"{}\"", validationQuery, container.getJdbcUrl());

        Runnable runValidationQuery = () -> {
            try (JdbcDatabaseDelegate delegate = new JdbcDatabaseDelegate(container, "")) {
                delegate.execute(validationQuery, "", 1, false, false);
            }
        };

        // execute validation query until success or timeout is reached
        try {
            Unreliables.retryUntilSuccess((int) this.startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                this.getRateLimiter().doWhenReady(runValidationQuery);
                return true;
            });
        } catch (Exception e) {
            throw new ContainerLaunchException(TIMEOUT_ERROR);
        }
    }

    private boolean isWaitStrategyTargetSupported() {
        return CONTAINER_CLASS.isInstance(this.waitStrategyTarget);
    }
}
