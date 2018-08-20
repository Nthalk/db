package com.iodesystems.db.data;

import com.iodesystems.db.query.Ordering;
import java.util.ArrayList;
import java.util.List;

public class Query {

  private String search = "";
  private List<Ordering> orderings = new ArrayList<>();
  private int page = 0;
  private int pageSize = -1;

  public String getSearch() {
    return search;
  }

  public void setSearch(String search) {
    this.search = search;
  }

  public List<Ordering> getOrderings() {
    return orderings;
  }

  public void setOrderings(List<Ordering> orderings) {
    this.orderings = orderings;
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }
}
