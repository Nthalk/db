package com.iodesystems.db.query;

import com.iodesystems.db.errors.DbException;
import com.iodesystems.db.jdbc.CleaningResultSetWrapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;

public class ParamaterizedSqlExecutor {

  private static final Pattern paramPattern = Pattern.compile(":([A-Za-z0-9_]+)");
  private final String sql;
  private final List<String> paramNames = new ArrayList<>();

  public ParamaterizedSqlExecutor(String sql) {
    Matcher matcher = paramPattern.matcher(sql);
    if (matcher.find()) {
      do {
        String group = matcher.group(1);
        paramNames.add(group);
        sql = matcher.replaceFirst("?");
        matcher.reset(sql);
      } while (matcher.find());
    }
    this.sql = sql;
  }

  public static PreparedStatement prepareStatement(
      Connection connection, String sql, Params params) {
    try {
      ParamaterizedSqlExecutor paramaterizedSqlExecutor = new ParamaterizedSqlExecutor(sql);
      PreparedStatement preparedStatement = connection
          .prepareStatement(paramaterizedSqlExecutor.sql);
      paramaterizedSqlExecutor.populateStatement(preparedStatement, params);
      return preparedStatement;
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public static ResultSet resultSet(DataSource dataSource, String sql, Params params) {
    try {
      Connection connection = dataSource.getConnection();
      PreparedStatement statement =
          ParamaterizedSqlExecutor.prepareStatement(connection, sql, params);
      statement.execute();
      return new CleaningResultSetWrapper(connection, statement, statement.getResultSet());
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public void populateStatement(PreparedStatement preparedStatement, Params params) {
    try {
      int index = 1;
      for (String paramName : paramNames) {
        preparedStatement.setObject(index++, params.get(paramName));
      }
    } catch (SQLException e) {
      throw new DbException(e);
    }

  }
}
