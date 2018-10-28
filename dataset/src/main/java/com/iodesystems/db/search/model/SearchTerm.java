package com.iodesystems.db.search.model;

public class SearchTerm {

  private final String rawTerm;
  private final String target;
  private final String value;

  public SearchTerm(String rawTerm, String target, String value) {
    this.rawTerm = rawTerm;
    this.target = target;
    this.value = value;
  }

  public String getTarget() {
    return target;
  }

  public String getValue() {
    return value;
  }

  public String getRawTerm() {
    return rawTerm;
  }
}
