package com.iodesystems.db.search;

import java.util.ArrayList;
import java.util.List;

public class SearchTermGroup {

  private final List<SearchTerm> terms = new ArrayList<>();

  public List<SearchTerm> getTerms() {
    return terms;
  }

  public void addTerm(String raw, String target, String term) {
    terms.add(new SearchTerm(raw, target, term));
  }
}
