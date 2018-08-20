package com.iodesystems.db.search;

import com.iodesystems.db.query.Params;

public class ConditionContext {

  private final Params params;
  private String target;
  private String argument;

  public ConditionContext(Params params) {
    this.params = params;
  }

  public Params getParams() {
    return params;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getArgument() {
    return argument;
  }

  public void setArgument(String argument) {
    this.argument = argument;
  }
}
