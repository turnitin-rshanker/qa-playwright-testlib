package com.turnitin.playwright.actions;

public final class BulkInputException extends RuntimeException {
  private final int fieldIndex;
  private final String fieldDescription;

  public BulkInputException(int fieldIndex, String fieldDescription, RuntimeException cause) {
    super("Could not fill field " + fieldIndex + " (" + fieldDescription + ")", cause);
    this.fieldIndex = fieldIndex;
    this.fieldDescription = fieldDescription;
  }

  public int fieldIndex() {
    return fieldIndex;
  }

  public String fieldDescription() {
    return fieldDescription;
  }
}
