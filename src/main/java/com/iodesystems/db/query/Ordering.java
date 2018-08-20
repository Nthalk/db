package com.iodesystems.db.query;

public class Ordering {

  private String column;
  private Sort sort;

  public Ordering() {
  }

  public Ordering(String column, Sort sort) {
    this.column = column;
    this.sort = sort;
  }

  public String getColumn() {
    return column;
  }

  public void setColumn(String column) {
    this.column = column;
  }

  public Sort getSort() {
    return sort;
  }

  public void setSort(Sort sort) {
    this.sort = sort;
  }
}
