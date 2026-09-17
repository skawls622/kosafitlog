package com.kosa.fitlog;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.zaxxer.hikari.HikariDataSource;

@SpringBootTest
class ConnectionTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void hikariConnectionIsOpen() throws SQLException {
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isClosed()).isFalse();
        }
    }
}
