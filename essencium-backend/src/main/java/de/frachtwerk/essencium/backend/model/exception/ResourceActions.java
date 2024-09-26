package de.frachtwerk.essencium.backend.model.exception;

public enum ResourceActions {
  CREATE("CREATE"),
  READ("READ"),
  UPDATE("UPDATE"),
  DELETE("DELETE");

  private final String action;

  ResourceActions(String action) {
    this.action = action;
  }

  @Override
  public String toString() {
    return action;
  }
}
