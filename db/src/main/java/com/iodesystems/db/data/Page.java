package com.iodesystems.db.data;

import com.iodesystems.db.logic.Converter;
import java.util.List;
import java.util.concurrent.Callable;

public class Page<T> {

  private final TypedDataSet<T> dataSet;
  private final Callable<Long> total;

  public Page(TypedDataSet<T> dataSet, Callable<Long> total) {
    this.dataSet = dataSet;
    this.total = total;
  }

  public <U> Page<U> as(Converter<T, U> converter) {
    return new Page<>(dataSet.map(converter), total);
  }

  public long getTotalItems() {
    try {
      return total.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public int getPage() {
    return dataSet.getPage();
  }

  public int getPageSize() {
    return dataSet.getPageSize();
  }

  public List<T> getItems() {
    return dataSet.getItems();
  }
}
