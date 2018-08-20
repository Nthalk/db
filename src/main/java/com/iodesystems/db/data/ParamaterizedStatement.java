package com.iodesystems.db.data;

import com.iodesystems.db.errors.DbException;
import com.iodesystems.db.jdbc.CleaningPreparedStatementWrapper;
import com.iodesystems.db.query.ParamaterizedSqlExecutor;
import com.iodesystems.db.query.Params;
import com.iodesystems.db.query.ParamsFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

public class ParamaterizedStatement {

  private final ParamaterizedSqlExecutor paramaterizedSqlExecutor;
  private final String sql;
  private final DataSource dataSource;
  private PreparedStatement preparedStatement = null;


  public ParamaterizedStatement(String sql, DataSource dataSource) {
    paramaterizedSqlExecutor = new ParamaterizedSqlExecutor(sql);
    this.sql = sql;
    this.dataSource = dataSource;
  }

  public ParamaterizedStatement execute(ParamsFactory paramsFactory) {
    try {
      Params params = paramsFactory.create(new Params(0));
      if (preparedStatement == null) {
        Connection connection = dataSource.getConnection();
        preparedStatement = new CleaningPreparedStatementWrapper(ParamaterizedSqlExecutor
            .prepareStatement(connection, sql, params), connection);
      } else {
        paramaterizedSqlExecutor
            .populateStatement(preparedStatement, params);
      }
      preparedStatement.execute();
      return this;
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
