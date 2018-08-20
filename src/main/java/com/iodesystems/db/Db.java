package com.iodesystems.db;

import com.iodesystems.db.data.DataSet;
import com.iodesystems.db.data.ParamaterizedStatement;
import com.iodesystems.db.data.Record;
import com.iodesystems.db.data.RecordMapper;
import com.iodesystems.db.errors.DbException;
import com.iodesystems.db.errors.RollbackException;
import com.iodesystems.db.jdbc.SingleUnclosableConnectionDataSource;
import com.iodesystems.db.jdbc.TransactionalDataSource;
import com.iodesystems.db.logic.Transactional;
import com.iodesystems.db.logic.TransactionalResult;
import com.iodesystems.db.query.ParamaterizedSqlExecutor;
import com.iodesystems.db.query.Params;
import com.iodesystems.db.query.ParamsFactory;
import com.iodesystems.db.query.QueryContext;
import com.iodesystems.db.query.QueryContextFactory;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import javax.sql.DataSource;

public class Db {

  private final DataSource dataSource;

  public DataSource getDataSource() {
    return dataSource;
  }

  public Db(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public static void transaction(DataSource dataSource, Transactional transactional)
      throws RollbackException {
    transactionResult(dataSource, (TransactionalResult<Void>) c -> {
      transactional.run(c);
      return null;
    });
  }

  public Db transactionStart() {
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      boolean autoCommit = connection.getAutoCommit();
      connection.setAutoCommit(false);
      Savepoint savepoint = connection.setSavepoint();
      TransactionalDataSource transactionalDataSource = new TransactionalDataSource(dataSource,
          connection, autoCommit, savepoint);
      return new Db(transactionalDataSource);
    } catch (SQLException e) {
      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException ignored) {

        }
      }
      throw new DbException(e);
    }
  }

  public Db transactionRollback() {
    if (dataSource instanceof TransactionalDataSource) {
      TransactionalDataSource dataSource = (TransactionalDataSource) this.dataSource;
      dataSource.transactionRollback();
      return new Db(dataSource.getParentDataSource());
    } else {
      throw new DbException("Not in a transaction");
    }
  }

  public Db transactionEnd() {
    if (dataSource instanceof TransactionalDataSource) {
      TransactionalDataSource dataSource = (TransactionalDataSource) this.dataSource;
      dataSource.transactionEnd();
      return new Db(dataSource.getParentDataSource());
    } else {
      throw new DbException("Not in a transaction");
    }
  }

  public static <T> T transactionResult(
      DataSource dataSource, TransactionalResult<T> transactionalResult) throws RollbackException {
    Db db = new Db(dataSource).transactionStart();
    try {
      return transactionalResult.run(db);
    } catch (RollbackException e) {
      db = db.transactionRollback();
      throw e;
    } finally {
      if (db.dataSource instanceof TransactionalDataSource) {
        db.transactionEnd();
      }
    }
  }

  public void transaction(Transactional transactional) throws RollbackException {
    transaction(dataSource, transactional);
  }

  public DataSet<Record> query(String sql, QueryContextFactory queryContextFactory) {
    return new DataSet<>(dataSource, queryContextFactory.queryContext(new QueryContext(sql)));
  }

  public DataSet<Record> query(Connection connection, String sql,
      QueryContextFactory queryContextFactory) {
    return new DataSet<>(new SingleUnclosableConnectionDataSource(dataSource, connection),
        queryContextFactory.queryContext(new QueryContext(sql)));
  }

  public DataSet<Record> query(String sql) {
    return new DataSet<>(dataSource, new QueryContext(sql));
  }

  public ParamaterizedStatement prepare(String sql) {
    return new ParamaterizedStatement(sql, dataSource);
  }

  public ParamaterizedStatement prepare(Connection connection, String sql) {
    return new ParamaterizedStatement(sql,
        new SingleUnclosableConnectionDataSource(dataSource, connection));
  }

  public <T> T transactionResult(TransactionalResult<T> transactionalResult) throws Exception {
    return transactionResult(dataSource, transactionalResult);
  }

  public void execute(String sql, ParamsFactory paramsFactory) {
    try (Connection connection = dataSource.getConnection()) {
      try (PreparedStatement preparedStatement =
          ParamaterizedSqlExecutor.prepareStatement(
              connection,
              sql,
              paramsFactory.create(new Params(0)))) {
        preparedStatement.execute();
      }
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public void execute(String sql) {
    try (Connection connection = dataSource.getConnection()) {
      try (CallableStatement preparedStatement = connection.prepareCall(sql)) {
        preparedStatement.execute();
      }
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }
}