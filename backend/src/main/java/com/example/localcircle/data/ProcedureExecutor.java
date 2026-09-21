package com.example.localcircle.data;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Component;

@Component
public class ProcedureExecutor {
  private final JdbcTemplate jdbc;

  public ProcedureExecutor(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public <T> List<T> call(String sql, RowMapper<T> reader, Object... args) {
    return jdbc.execute(
        (ConnectionCallback<List<T>>)
            connection -> {
              try (CallableStatement statement = connection.prepareCall(sql)) {
                for (int i = 0; i < args.length; i++) {
                  if (args[i] instanceof Double d)
                    statement.setBigDecimal(i + 1, BigDecimal.valueOf(d));
                  else statement.setObject(i + 1, args[i]);
                }
                statement.setQueryTimeout(10);
                boolean result = statement.execute();
                while (!result && statement.getUpdateCount() != -1)
                  result = statement.getMoreResults();
                List<T> rows = new ArrayList<>();
                if (result)
                  try (ResultSet rs = statement.getResultSet()) {
                    while (rs.next()) rows.add(reader.mapRow(rs, rows.size()));
                  }
                return List.copyOf(rows);
              }
            });
  }
}
