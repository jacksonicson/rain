/**
 * Autogenerated by Thrift Compiler (0.8.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package de.tum.in.storm.iaas;


import java.util.Map;
import java.util.HashMap;
import org.apache.thrift.TEnum;

public enum DomainSize implements org.apache.thrift.TEnum {
  SMALL(0),
  MEDIUM(1),
  LARGE(2);

  private final int value;

  private DomainSize(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   * @return null if the value is not found.
   */
  public static DomainSize findByValue(int value) { 
    switch (value) {
      case 0:
        return SMALL;
      case 1:
        return MEDIUM;
      case 2:
        return LARGE;
      default:
        return null;
    }
  }
}
