package com.iodesystems.db.search;

import java.util.ArrayList;
import java.util.List;

public class SearchParser {

  final char groupSeparator = ',';
  final char termSeparator = ' ';
  final char targetSeparator = ':';
  boolean escaping = false;
  char inQuote = 0;

  public List<SearchTermGroup> parse(String search) {
    List<SearchTermGroup> groups = new ArrayList<>();
    SearchTermGroup group = new SearchTermGroup();
    StringBuilder current = new StringBuilder();
    String target = null;
    for (char c : search.toCharArray()) {
      if (inQuote != 0 && !escaping) {
        if (c == inQuote) {
          inQuote = 0;
        } else {
          current.append(c);
        }
      } else {
        if (c == '\'' || c == '"') {
          inQuote = c;
        } else if (c == '\\') {
          escaping = !escaping;
          current.append(c);
        } else {
          escaping = false;
          if (c == groupSeparator) {
            if (current.length() > 0) {
              group.addTerm(
                  target == null
                      ? current.toString()
                      : target + targetSeparator + current.toString(),
                  target,
                  current.toString());
              current.setLength(0);
            }
            if (!group.getTerms().isEmpty()) {
              groups.add(group);
            }
            group = new SearchTermGroup();
            target = null;
          } else if (c == termSeparator) {
            if (current.length() > 0) {
              group.addTerm(
                  target == null
                      ? current.toString()
                      : target + targetSeparator + current.toString(),
                  target,
                  current.toString());
              current.setLength(0);
              target = null;
            }
          } else if (c == targetSeparator) {
            target = current.toString();
            current.setLength(0);
          } else {
            current.append(c);
          }
        }
      }
    }

    if (current.length() > 0) {
      group.addTerm(
          target == null ? current.toString() : target + targetSeparator + current.toString(),
          target,
          current.toString());
    }

    if (!group.getTerms().isEmpty()) {
      groups.add(group);
    }
    return groups;
  }
}
