package com.iodesystems.db.search;

public class Search {

  public static final ConditionFactory Y_N = c -> {
    String value = c.term().toLowerCase();
    Boolean argument = null;
    if ("y".equals(value)
        || "yes".equals(value)
        || "1".equals(value)
        || "t".equals(value)
        || "true".equals(value)) {
      argument = true;
    } else if ("n".equals(value)
        || "no".equals(value)
        || "0".equals(value)
        || "f".equals(value)
        || "false".equals(value)) {
      argument = false;
    }
    if (argument == null) {
      return null;
    }
    return c
        .field() + " = " + c.
        param(argument ? "N" : "Y");
  };

  public static final ConditionFactory IS = c -> c
      .field() + " = " + c
      .param();

  public static final ConditionFactory STARTS_WITH = c -> c
      .field() +
      " like " + c
      .param(c.term() + "%");

}
