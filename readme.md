# Simple JdbcDatabaseContainer WaitStrategy for Testcontainers

## Why this library? 
This library helps with plugging back the default behaviour of the JdbcDatabaseContainer `waitUntilStarted` method.
Currently manually wiring a Postgresql container from `org.testcontainers:postgresql` overrides the JdbcDatabaseContainer `waitUntilStarted` and bypassing 
connection testing.

## Usage / Installation
Include
- Maven
    ```xml
    <dependency>
        <groupdid>com.github.frehov.testcontainers.jdbc</groupdid>
        <artifactid>jdbc-wait-strategy</artifactid>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    ```
- Gradle
    ```
    implementation("com.github.frehov.testcontainers.jdbc:jdbc-wait-strategy:1.0.0-SNAPSHOT")
    ```

Required dependencies that need to be supplied by the user:
- Maven
  ```xml
  <dependency>
    <groupdid>org.testcontainers</groupdid>
    <artifactid>jdbc</artifactid>
    <version>1.17.6</version>
  </dependency>
  <dependency>
    <groupdid>org.slf4j</groupdid>
    <artifactid>slf4j-api</artifactid>
    <version>1.7.36</version>
  </dependency>
  ```
- Gradle
    ```
    implementation("org.testcontainers:jdbc:1.17.6")
    implementation("org.slf4j:slf4j-api:1.7.36")
    ```

## Usage example - Spring Boot Test abstract baseclass
In this example we have plugged in our `JdbcConnectionReadyWaitStrategy` so that we can plug back the original wait strategy defined
```java
import com.github.frehov.testcontainers.jdbc.wait.strategy.JdbcConnectionReadyWaitStrategy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
@DirtiesContext // Added due to using @DynamicPropertySource usage with @Container.
public abstract class TestBase {
    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:12").waitingFor(new JdbcConnectionReadyWaitStrategy());

    @DynamicPropertySource
    static void postgresDbProperties(DynamicPropertyRegistry registry) {
        // DynamicPropertySource is a spring context customizer enabling us to utilise the testcontainers-junit extension fully.
        // https://www.baeldung.com/spring-dynamicpropertysource#thedynamicpropertysource
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

}
```

## Alternative usage - Testcontainers jdbc driver
See [testcontainers database jdbc page](https://www.testcontainers.org/modules/databases/jdbc/) for more information.

Effective usage is: `jdbc:tc:postgresql:12:///databasename`


To use the Testcontainers JDBC Database driver the following requirements are needed:
```xml
<dependency>
  <groupdid>org.testcontainers</groupdid>
  <artifactid>jdbc</artifactid>
  <version>1.17.6</version>
</dependency>
<dependency>
  <groupdid>org.testcontainers</groupdid>
  <artifactid>postgresql</artifactid> <!-- Substitute/add any other JDBC based containers here -->
  <version>1.17.6</version>
</dependency>
```