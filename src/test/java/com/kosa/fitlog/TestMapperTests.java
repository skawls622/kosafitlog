package com.kosa.fitlog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TestMapperTests {

    @Autowired
    private TestMapper testMapper;

    @Test
    void selectOneFromDual() {
        assertThat(testMapper.selectOne()).isEqualTo(1);
    }
}
