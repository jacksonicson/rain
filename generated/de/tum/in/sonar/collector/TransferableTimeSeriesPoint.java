/**
 * Autogenerated by Thrift Compiler (0.9.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package de.tum.in.sonar.collector;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferableTimeSeriesPoint implements org.apache.thrift.TBase<TransferableTimeSeriesPoint, TransferableTimeSeriesPoint._Fields>, java.io.Serializable, Cloneable {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TransferableTimeSeriesPoint");

  private static final org.apache.thrift.protocol.TField TIMESTAMP_FIELD_DESC = new org.apache.thrift.protocol.TField("timestamp", org.apache.thrift.protocol.TType.I64, (short)1);
  private static final org.apache.thrift.protocol.TField VALUE_FIELD_DESC = new org.apache.thrift.protocol.TField("value", org.apache.thrift.protocol.TType.DOUBLE, (short)2);
  private static final org.apache.thrift.protocol.TField LABELS_FIELD_DESC = new org.apache.thrift.protocol.TField("labels", org.apache.thrift.protocol.TType.SET, (short)3);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TransferableTimeSeriesPointStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TransferableTimeSeriesPointTupleSchemeFactory());
  }

  public long timestamp; // required
  public double value; // required
  public Set<String> labels; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    TIMESTAMP((short)1, "timestamp"),
    VALUE((short)2, "value"),
    LABELS((short)3, "labels");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // TIMESTAMP
          return TIMESTAMP;
        case 2: // VALUE
          return VALUE;
        case 3: // LABELS
          return LABELS;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __TIMESTAMP_ISSET_ID = 0;
  private static final int __VALUE_ISSET_ID = 1;
  private byte __isset_bitfield = 0;
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.TIMESTAMP, new org.apache.thrift.meta_data.FieldMetaData("timestamp", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64        , "long")));
    tmpMap.put(_Fields.VALUE, new org.apache.thrift.meta_data.FieldMetaData("value", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.DOUBLE)));
    tmpMap.put(_Fields.LABELS, new org.apache.thrift.meta_data.FieldMetaData("labels", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.SetMetaData(org.apache.thrift.protocol.TType.SET, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TransferableTimeSeriesPoint.class, metaDataMap);
  }

  public TransferableTimeSeriesPoint() {
  }

  public TransferableTimeSeriesPoint(
    long timestamp,
    double value,
    Set<String> labels)
  {
    this();
    this.timestamp = timestamp;
    setTimestampIsSet(true);
    this.value = value;
    setValueIsSet(true);
    this.labels = labels;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TransferableTimeSeriesPoint(TransferableTimeSeriesPoint other) {
    __isset_bitfield = other.__isset_bitfield;
    this.timestamp = other.timestamp;
    this.value = other.value;
    if (other.isSetLabels()) {
      Set<String> __this__labels = new HashSet<String>();
      for (String other_element : other.labels) {
        __this__labels.add(other_element);
      }
      this.labels = __this__labels;
    }
  }

  public TransferableTimeSeriesPoint deepCopy() {
    return new TransferableTimeSeriesPoint(this);
  }

  @Override
  public void clear() {
    setTimestampIsSet(false);
    this.timestamp = 0;
    setValueIsSet(false);
    this.value = 0.0;
    this.labels = null;
  }

  public long getTimestamp() {
    return this.timestamp;
  }

  public TransferableTimeSeriesPoint setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    setTimestampIsSet(true);
    return this;
  }

  public void unsetTimestamp() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __TIMESTAMP_ISSET_ID);
  }

  /** Returns true if field timestamp is set (has been assigned a value) and false otherwise */
  public boolean isSetTimestamp() {
    return EncodingUtils.testBit(__isset_bitfield, __TIMESTAMP_ISSET_ID);
  }

  public void setTimestampIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __TIMESTAMP_ISSET_ID, value);
  }

  public double getValue() {
    return this.value;
  }

  public TransferableTimeSeriesPoint setValue(double value) {
    this.value = value;
    setValueIsSet(true);
    return this;
  }

  public void unsetValue() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __VALUE_ISSET_ID);
  }

  /** Returns true if field value is set (has been assigned a value) and false otherwise */
  public boolean isSetValue() {
    return EncodingUtils.testBit(__isset_bitfield, __VALUE_ISSET_ID);
  }

  public void setValueIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __VALUE_ISSET_ID, value);
  }

  public int getLabelsSize() {
    return (this.labels == null) ? 0 : this.labels.size();
  }

  public java.util.Iterator<String> getLabelsIterator() {
    return (this.labels == null) ? null : this.labels.iterator();
  }

  public void addToLabels(String elem) {
    if (this.labels == null) {
      this.labels = new HashSet<String>();
    }
    this.labels.add(elem);
  }

  public Set<String> getLabels() {
    return this.labels;
  }

  public TransferableTimeSeriesPoint setLabels(Set<String> labels) {
    this.labels = labels;
    return this;
  }

  public void unsetLabels() {
    this.labels = null;
  }

  /** Returns true if field labels is set (has been assigned a value) and false otherwise */
  public boolean isSetLabels() {
    return this.labels != null;
  }

  public void setLabelsIsSet(boolean value) {
    if (!value) {
      this.labels = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case TIMESTAMP:
      if (value == null) {
        unsetTimestamp();
      } else {
        setTimestamp((Long)value);
      }
      break;

    case VALUE:
      if (value == null) {
        unsetValue();
      } else {
        setValue((Double)value);
      }
      break;

    case LABELS:
      if (value == null) {
        unsetLabels();
      } else {
        setLabels((Set<String>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case TIMESTAMP:
      return Long.valueOf(getTimestamp());

    case VALUE:
      return Double.valueOf(getValue());

    case LABELS:
      return getLabels();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case TIMESTAMP:
      return isSetTimestamp();
    case VALUE:
      return isSetValue();
    case LABELS:
      return isSetLabels();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TransferableTimeSeriesPoint)
      return this.equals((TransferableTimeSeriesPoint)that);
    return false;
  }

  public boolean equals(TransferableTimeSeriesPoint that) {
    if (that == null)
      return false;

    boolean this_present_timestamp = true;
    boolean that_present_timestamp = true;
    if (this_present_timestamp || that_present_timestamp) {
      if (!(this_present_timestamp && that_present_timestamp))
        return false;
      if (this.timestamp != that.timestamp)
        return false;
    }

    boolean this_present_value = true;
    boolean that_present_value = true;
    if (this_present_value || that_present_value) {
      if (!(this_present_value && that_present_value))
        return false;
      if (this.value != that.value)
        return false;
    }

    boolean this_present_labels = true && this.isSetLabels();
    boolean that_present_labels = true && that.isSetLabels();
    if (this_present_labels || that_present_labels) {
      if (!(this_present_labels && that_present_labels))
        return false;
      if (!this.labels.equals(that.labels))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  public int compareTo(TransferableTimeSeriesPoint other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    TransferableTimeSeriesPoint typedOther = (TransferableTimeSeriesPoint)other;

    lastComparison = Boolean.valueOf(isSetTimestamp()).compareTo(typedOther.isSetTimestamp());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetTimestamp()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.timestamp, typedOther.timestamp);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetValue()).compareTo(typedOther.isSetValue());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetValue()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.value, typedOther.value);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetLabels()).compareTo(typedOther.isSetLabels());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetLabels()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.labels, typedOther.labels);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TransferableTimeSeriesPoint(");
    boolean first = true;

    sb.append("timestamp:");
    sb.append(this.timestamp);
    first = false;
    if (!first) sb.append(", ");
    sb.append("value:");
    sb.append(this.value);
    first = false;
    if (!first) sb.append(", ");
    sb.append("labels:");
    if (this.labels == null) {
      sb.append("null");
    } else {
      sb.append(this.labels);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class TransferableTimeSeriesPointStandardSchemeFactory implements SchemeFactory {
    public TransferableTimeSeriesPointStandardScheme getScheme() {
      return new TransferableTimeSeriesPointStandardScheme();
    }
  }

  private static class TransferableTimeSeriesPointStandardScheme extends StandardScheme<TransferableTimeSeriesPoint> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TransferableTimeSeriesPoint struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // TIMESTAMP
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.timestamp = iprot.readI64();
              struct.setTimestampIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // VALUE
            if (schemeField.type == org.apache.thrift.protocol.TType.DOUBLE) {
              struct.value = iprot.readDouble();
              struct.setValueIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // LABELS
            if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
              {
                org.apache.thrift.protocol.TSet _set16 = iprot.readSetBegin();
                struct.labels = new HashSet<String>(2*_set16.size);
                for (int _i17 = 0; _i17 < _set16.size; ++_i17)
                {
                  String _elem18; // required
                  _elem18 = iprot.readString();
                  struct.labels.add(_elem18);
                }
                iprot.readSetEnd();
              }
              struct.setLabelsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, TransferableTimeSeriesPoint struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      oprot.writeFieldBegin(TIMESTAMP_FIELD_DESC);
      oprot.writeI64(struct.timestamp);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(VALUE_FIELD_DESC);
      oprot.writeDouble(struct.value);
      oprot.writeFieldEnd();
      if (struct.labels != null) {
        oprot.writeFieldBegin(LABELS_FIELD_DESC);
        {
          oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, struct.labels.size()));
          for (String _iter19 : struct.labels)
          {
            oprot.writeString(_iter19);
          }
          oprot.writeSetEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TransferableTimeSeriesPointTupleSchemeFactory implements SchemeFactory {
    public TransferableTimeSeriesPointTupleScheme getScheme() {
      return new TransferableTimeSeriesPointTupleScheme();
    }
  }

  private static class TransferableTimeSeriesPointTupleScheme extends TupleScheme<TransferableTimeSeriesPoint> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TransferableTimeSeriesPoint struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetTimestamp()) {
        optionals.set(0);
      }
      if (struct.isSetValue()) {
        optionals.set(1);
      }
      if (struct.isSetLabels()) {
        optionals.set(2);
      }
      oprot.writeBitSet(optionals, 3);
      if (struct.isSetTimestamp()) {
        oprot.writeI64(struct.timestamp);
      }
      if (struct.isSetValue()) {
        oprot.writeDouble(struct.value);
      }
      if (struct.isSetLabels()) {
        {
          oprot.writeI32(struct.labels.size());
          for (String _iter20 : struct.labels)
          {
            oprot.writeString(_iter20);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TransferableTimeSeriesPoint struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(3);
      if (incoming.get(0)) {
        struct.timestamp = iprot.readI64();
        struct.setTimestampIsSet(true);
      }
      if (incoming.get(1)) {
        struct.value = iprot.readDouble();
        struct.setValueIsSet(true);
      }
      if (incoming.get(2)) {
        {
          org.apache.thrift.protocol.TSet _set21 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.labels = new HashSet<String>(2*_set21.size);
          for (int _i22 = 0; _i22 < _set21.size; ++_i22)
          {
            String _elem23; // required
            _elem23 = iprot.readString();
            struct.labels.add(_elem23);
          }
        }
        struct.setLabelsIsSet(true);
      }
    }
  }

}

