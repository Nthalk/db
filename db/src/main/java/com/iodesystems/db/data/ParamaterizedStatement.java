package com.iodesystems.db.data;

import com.iodesystems.db.errors.DbException;
import com.iodesystems.db.jdbc.CleaningPreparedStatementWrapper;
import com.iodesystems.db.query.ParamaterizedSqlExecutor;
import com.iodesystems.db.query.Params;
import com.iodesystems.db.query.ParamsConfigurer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

public class ParamaterizedStatement implements AutoCloseable {

  private final ParamaterizedSqlExecutor paramaterizedSqlExecutor;
  private final String sql;
  private final DataSource dataSource;
  private PreparedStatement preparedStatement = null;
  private final List<ParamsConfigurer> params = new ArrayList<>();

  public ParamaterizedStatement(String sql, DataSource dataSource) {
    paramaterizedSqlExecutor = new ParamaterizedSqlExecutor(sql);
    this.sql = sql;
    this.dataSource = dataSource;
  }

  public ParamaterizedStatement add(ParamsConfigurer paramsConfigurer) {
    params.add(paramsConfigurer);
    return this;
  }

  public int execute() {
    if (params.size() == 0) {
      return 0;
    }
    try (Connection connection = dataSource.getConnection()) {
      ParamaterizedSqlExecutor executor = new ParamaterizedSqlExecutor(sql);
      try (PreparedStatement statement = ParamaterizedSqlExecutor
          .prepareStatement(connection, sql, null)) {
        for (ParamsConfigurer paramsConfigurer : params) {
          executor.populateStatement(statement, paramsConfigurer.configure(new Params(0)));
          statement.addBatch();
        }
        return statement.executeUpdate();
      }
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public int execute(ParamsConfigurer paramsConfigurer) {
    try {
      Params params = paramsConfigurer.configure(new Params(0));
      if (preparedStatement == null) {
        Connection connection = dataSource.getConnection();
        preparedStatement = new CleaningPreparedStatementWrapper(ParamaterizedSqlExecutor
            .prepareStatement(connection, sql, params), connection);
      } else {
        paramaterizedSqlExecutor
            .populateStatement(preparedStatement, params);
      }
      return preparedStatement.executeUpdate();
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public void close() {
    if (preparedStatement != null) {
      try {
        preparedStatement.close();
      } catch (SQLException e) {
        throw new DbException(e);
      }
    }
  }

}
