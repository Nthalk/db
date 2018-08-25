package com.iodesystems.db.data;

import com.iodesystems.db.logic.Converter;
import com.iodesystems.db.query.Column;
import com.iodesystems.db.query.Ordering;
import java.util.ArrayList;
import java.util.List;

public class TypedDataSet<T> {

  private final DataSet dataSet;
  private final RecordMapper<T> mapper;

  public TypedDataSet(DataSet dataSet, RecordMapper<T> mapper) {
    this.dataSet = dataSet;
    this.mapper = mapper;
  }

  public <U> TypedDataSet<U> map(Converter<T, U> converter) {
    return new TypedDataSet<>(dataSet, r -> converter.map(mapper.map(r)));
  }

  public int getPage() {
    return dataSet.getPage();
  }

  public int getPageSize() {
    return dataSet.getPageSize();
  }

  public List<T> getItems() {
    List<T> items = new ArrayList<>();
    dataSet.stream(r -> items.add(mapper.map(r)));
    return items;
  }

  public TypedDataSet<T> search(String search) {
    return new TypedDataSet<>(dataSet.search(search), mapper);
  }

  public TypedDataSet<T> order(Ordering order) {
    return new TypedDataSet<>(dataSet.order(order), mapper);
  }

  public TypedDataSet<T> page(int page, int pageSize) {
    return new TypedDataSet<>(dataSet.page(page, pageSize), mapper);

  }

  public TypedDataSet<T> nextPage() {
    return new TypedDataSet<>(dataSet.nextPage(), mapper);
  }

  public List<Column<?>> getUnderlyingColumns() {
    return dataSet.getColumns();
  }

  public Page<T> query(Query query) {
    DataSet dataSet = this.dataSet;
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
    return new Page<T>(new TypedDataSet<>(dataSet, mapper), countDataSet::count);
  }
}
