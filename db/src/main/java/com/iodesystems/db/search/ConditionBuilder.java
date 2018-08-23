package com.iodesystems.db.search;

import com.iodesystems.db.query.Params;

public class ConditionBuilder {

  private final ConditionContext conditionContext;
  private final Params params;

  public ConditionBuilder(ConditionContext conditionContext) {
    this.conditionContext = conditionContext;
    params = conditionContext.getParams();
  }

  public String param(String key, Object value) {
    params.set(key, value);
    return null;
  }

  public String param(Object value) {
    return params.setPrefixed(conditionContext.getTarget(), value);
  }

  public String param() {
    return params.setPrefixed(conditionContext.getTarget(), conditionContext.getArgument());
  }

  public String field() {
    return conditionContext.getTarget();
  }

  public String term() {
    return conditionContext.getArgument();
  }
}
