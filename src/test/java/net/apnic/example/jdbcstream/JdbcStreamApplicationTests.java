package net.apnic.example.jdbcstream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = JdbcStreamApplication.class)
@TestExecutionListeners(listeners = { DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class })
public class JdbcStreamApplicationTests {
    @Autowired
    JdbcStream jdbcStream;

    @Autowired
    JdbcStreamApplication.QueryStream queryStream;

    @Autowired
    ApplicationContext context;

    @Before
    public void setUp() {
        jdbcStream.execute((Connection conn) -> {
            ScriptUtils.executeSqlScript(conn, context.getResource("data.sql"));
            return null;
        });
    }

	@Test
	public void contextLoads() {
	}

    private Set<String> streamData(Stream<SqlRowSet> stream) {
        return stream.map(row -> row.getString("entry"))
                .filter(s -> Character.isAlphabetic(s.charAt(0)))
                .collect(Collectors.toSet());
    }

	@Test
	public void streamsData() throws SQLException, IOException {
        try (JdbcStream.StreamableQuery query = jdbcStream.streamableQuery("SELECT * FROM test_data")) {
            Set<String> results = query.stream()
                    .map(row -> row.getString("entry"))
                    .filter(s -> Character.isAlphabetic(s.charAt(0)))
                    .collect(Collectors.toSet());

            assertThat("3 results start with an alphabetic character", results.size(), is(equalTo(3)));
        }
    }

    @Test
    public void streamsEmptyData() {
        Set<String> results = queryStream.streamQuery("SELECT * FROM test_data WHERE entry IS NULL",
                this::streamData);
        assertThat("A query with no results produces an empty set", results, is(empty()));
    }

    @Test
    public void callbackStreaming() {
        Set<String> results = jdbcStream.streamQuery("SELECT * FROM test_data", stream -> stream
            .map(row -> row.getString("entry"))
            .filter(s -> Character.isAlphabetic(s.charAt(0)))
            .collect(Collectors.toSet()));

        assertThat("3 results start with an alphabetic character", results.size(), is(equalTo(3)));
    }

}
