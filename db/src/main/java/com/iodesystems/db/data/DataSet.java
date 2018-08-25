package com.iodesystems.db.data;

import com.iodesystems.db.errors.DbException;
import com.iodesystems.db.logic.Handler;
import com.iodesystems.db.query.Column;
import com.iodesystems.db.query.Ordering;
import com.iodesystems.db.query.ParamaterizedSqlExecutor;
import com.iodesystems.db.query.Params;
import com.iodesystems.db.query.QueryContext;
import com.iodesystems.db.query.Sort;
import com.iodesystems.db.search.SearchContext;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;

public class DataSet {

  private final DataSource dataSource;
  private final QueryContext queryContext;
  private final List<String> searches;
  private final List<Ordering> ordering;
  private final int page;
  private final int pageSize;

  public DataSet(
      DataSource dataSource,
      QueryContext queryContext,
      int page,
      int pageSize,
      List<String> searches,
      List<Ordering> ordering) {
    this.dataSource = dataSource;
    this.queryContext = queryContext;
    this.page = page;
    this.pageSize = pageSize;
    this.searches = searches;
    this.ordering = ordering;
  }

  public DataSet(DataSet toClone) {
    this(toClone.dataSource,
        toClone.queryContext,
        toClone.page,
        toClone.pageSize,
        new ArrayList<>(toClone.searches),
        new ArrayList<>(toClone.ordering));
  }

  public DataSet(DataSource dataSource, QueryContext queryContext) {
    this(dataSource, queryContext, 0, -1, new ArrayList<>(), new ArrayList<>());
  }

  public List<Ordering> getOrdering() {
    return ordering;
  }

  public long count() {
    try {
      SearchContext searchContext = getSearchContext();
      String sql = "SELECT COUNT(0) FROM (" + searchContext.getSql() + ")";
      try (ResultSet resultSet =
          resultSet(
              sql, searchContext.getParams())) {
        RecordCursor recordCursor = new RecordCursor(sql, resultSet);
        recordCursor.next();
        return recordCursor.getLong(0);
      }
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public ResultSet resultSet() {
    SearchContext searchContext = getSearchContext();
    return resultSet(searchContext.getSql(), searchContext.getParams());
  }

  private ResultSet resultSet(String sql, Params params) {
    return ParamaterizedSqlExecutor.resultSet(dataSource, sql, params);
  }

  public SearchContext getSearchContext() {
    return new SearchContext(queryContext, searches, ordering, page, pageSize);
  }

  public void stream(Handler<RecordCursor> handler) {
    SearchContext searchContext = getSearchContext();
    try (ResultSet resultSet = resultSet(searchContext.getSql(), searchContext.getParams())) {
      RecordCursor recordCursor = new RecordCursor(searchContext.getSql(), resultSet);
      while (recordCursor.next()) {
        handler.handle(recordCursor);
      }
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }


  public DataSet order(String column, Sort direction) {
    return order(new Ordering(column, direction));
  }

  public DataSet order(Ordering ordering, Ordering... more) {
    List<Ordering> orderings = new ArrayList<>(this.ordering);
    orderings.add(ordering);
    orderings.addAll(Arrays.asList(more));
    return new DataSet(
        dataSource, queryContext, page, pageSize, searches, orderings);
  }

  public DataSet page(int page, int pageSize) {
    return new DataSet(
        dataSource, queryContext, page, pageSize, searches, ordering);
  }

  public DataSet nextPage() {
    if (pageSize == -1) {
      return this;
    } else {
      return page(page + 1, pageSize);
    }
  }


  public DataSet order(List<Ordering> orderings) {
    List<Ordering> newOrderings = new ArrayList<>(this.ordering);
    newOrderings.addAll(orderings);
    return new DataSet(
        dataSource, queryContext, page, pageSize, searches, newOrderings);
  }

  public List<Column<?>> getColumns() {
    SearchContext searchContext = page(0, 0).getSearchContext();
    List<Column<?>> columns = new ArrayList<>();
    try (ResultSet resultSet = resultSet(searchContext.getSql(), searchContext.getParams())) {
      ResultSetMetaData metaData = resultSet.getMetaData();
      int columnCount = metaData.getColumnCount();
      for (int i = 0; i < columnCount; i++) {
        String columnName = metaData.getColumnName(i + 1);
        if (columnName.toLowerCase().equals("__rn")) {
          continue;
        }
        Class<?> columnClass = null;
        try {
          columnClass = Class.forName(metaData.getColumnClassName(1));
        } catch (ClassNotFoundException e) {
          // Class not found
        }
        Column<?> column = new Column<>(
            false,
            null,
            columnName,
            columnClass);
        columns.add(column);
      }
      return columns;
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public int getPage() {
    return page;
  }

  public int getPageSize() {
    return pageSize;
  }

  public <U> U first(RecordMapper<U> mapper) {
    SearchContext searchContext = getSearchContext();
    try (ResultSet resultSet = resultSet(searchContext.getSql(), searchContext.getParams())) {
      RecordCursor recordCursor = new RecordCursor(searchContext.getSql(), resultSet);
      if (recordCursor.next()) {
        return mapper.map(recordCursor);
      } else {
        return null;

      }
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public <U> U first(Class<U> fieldType, U ifNull) {
    U value = first(fieldType);
    if (value == null) {
      return ifNull;
    }
    return value;
  }

  public <U> U first(Class<U> fieldType) {
    return first(r -> r.getRecord().get(0, fieldType));
  }

  public DataSet search(String s) {
    List<String> searches = new ArrayList<>(this.searches);
    searches.add(s);
    return new DataSet(dataSource, queryContext, page, pageSize, searches, ordering);
  }

  public List<Record> getItems() {
    return map(RecordCursor::getRecord).getItems();
  }

  public <T> TypedDataSet<T> map(RecordMapper<T> mapper) {
    return new TypedDataSet<>(this, mapper);
  }


  public Page<Record> query(Query query) {
    DataSet dataSet = this;
    if (query.getSearch() != null && !query.getSearch().isEmpty()) {
      dataSet.search(query.getSearch());
    }
    final DataSet countDataSet = dataSet;

    if (query.getPageSize() > 0) {
      dataSet = dataSet.page(query.getPage(), query.getPageSize());
    }
    if (query.getOrderings() != null && !query.getOrderings().isEmpty()) {
      dataSet.order(query.getOrderings());
    }
    return new Page<>(dataSet.map(r -> r.getRecord()), countDataSet::count);
  }
}
