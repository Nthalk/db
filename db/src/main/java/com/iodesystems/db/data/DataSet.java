package com.iodesystems.db.data;

import com.iodesystems.db.errors.DbException;
import com.iodesystems.db.logic.Converter;
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

public class DataSet<T> {

  private final DataSource dataSource;
  private final QueryContext queryContext;
  private final RecordMapper<T> recordLoader;
  private final List<String> searches;
  private final List<Ordering> orderings;
  private final int page;
  private final int pageSize;

  public DataSet(
      DataSource dataSource,
      QueryContext queryContext,
      RecordMapper<T> recordLoader,
      int page,
      int pageSize,
      List<String> searches,
      List<Ordering> orderings) {
    this.dataSource = dataSource;
    this.queryContext = queryContext;
    this.recordLoader = recordLoader;
    this.page = page;
    this.pageSize = pageSize;
    this.searches = searches;
    this.orderings = orderings;
  }

  public DataSet(DataSource dataSource, QueryContext queryContext) {
    this(dataSource, queryContext, null, 0, -1, new ArrayList<>(), new ArrayList<>());
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
    return new SearchContext(queryContext, searches, orderings, page, pageSize);
  }

  public void stream(Handler<T> handler) {
    SearchContext searchContext = getSearchContext();
    try (ResultSet resultSet = resultSet(searchContext.getSql(), searchContext.getParams())) {
      RecordCursor recordCursor = new RecordCursor(searchContext.getSql(), resultSet);
      if (recordLoader == null) {
        while (recordCursor.next()) {
          //noinspection unchecked
          handler.handle((T) recordCursor.getRecord());
        }
      } else {
        while (recordCursor.next()) {
          handler.handle(recordCursor.map(recordLoader));
        }
      }
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public <TO> DataSet<TO> map(Converter<T, TO> converter) {
    //noinspection unchecked
    return new DataSet<>(
        dataSource,
        queryContext,
        record ->
            recordLoader == null
                ? converter.convert((T) record)
                : converter.convert(recordLoader.map(record)),
        page,
        pageSize,
        searches,
        orderings);
  }

  public <U> DataSet<U> load(RecordMapper<U> recordMapper) {
    return new DataSet<>(
        dataSource, queryContext, recordMapper, page, pageSize, searches, orderings);
  }

  public List<T> getItems() {
    List<T> items = new ArrayList<>();
    stream(items::add);
    return items;
  }

  public DataSet<T> search(String search) {
    List<String> searches = new ArrayList<>(this.searches);
    searches.add(search);
    return new DataSet<>(
        dataSource, queryContext, recordLoader, page, pageSize, searches, orderings);
  }

  public DataSet<T> order(String column, Sort direction) {
    return order(new Ordering(column, direction));
  }

  public DataSet<T> order(Ordering ordering, Ordering... more) {
    List<Ordering> orderings = new ArrayList<>(this.orderings);
    orderings.add(ordering);
    orderings.addAll(Arrays.asList(more));
    return new DataSet<>(
        dataSource, queryContext, recordLoader, page, pageSize, searches, orderings);
  }

  public DataSet<T> page(int page, int pageSize) {
    return new DataSet<>(
        dataSource, queryContext, recordLoader, page, pageSize, searches, orderings);
  }

  public DataSet<T> nextPage() {
    if (pageSize == -1) {
      return this;
    } else {
      return page(page + 1, pageSize);
    }
  }

  public Page<T> apply(Query query) {
    DataSet<T> dataSet = this;
    if (query.getSearch() != null && !query.getSearch().isEmpty()) {
      dataSet.search(query.getSearch());
    }
    final DataSet<T> countDataSet = dataSet;

    if (query.getPageSize() > 0) {
      dataSet = dataSet.page(query.getPage(), query.getPageSize());
    }
    if (query.getOrderings() != null && !query.getOrderings().isEmpty()) {
      dataSet.order(query.getOrderings());
    }
    return new Page<>(dataSet, countDataSet::count);
  }

  public Page<T> page() {
    return new Page<>(this, this::count);
  }

  public DataSet<T> order(List<Ordering> orderings) {
    List<Ordering> newOrderings = new ArrayList<>(this.orderings);
    newOrderings.addAll(orderings);
    return new DataSet<>(
        dataSource, queryContext, recordLoader, page, pageSize, searches, newOrderings);
  }

  public List<Column> getColumns() {
    SearchContext searchContext = page(0, 0).getSearchContext();
    List<Column> columns = new ArrayList<>();
    try (ResultSet resultSet = resultSet(searchContext.getSql(), searchContext.getParams())) {
      ResultSetMetaData metaData = resultSet.getMetaData();
      int columnCount = metaData.getColumnCount();
      for (int i = 0; i < columnCount; i++) {
        String columnName = metaData.getColumnName(i + 1);
        if (columnName.toLowerCase().equals("__rn")) {
          continue;
        }
        Column column = queryContext.getColumn(columnName);
        if (column == null) {
          column = new Column(false, null, columnName);
        }
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

  public T first() {
    List<T> items = page(this.page, 1).getItems();
    if (items.isEmpty()) {
      return null;
    } else {
      return items.get(0);
    }
  }

}
