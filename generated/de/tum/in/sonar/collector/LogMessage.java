/**
 * Autogenerated by Thrift Compiler (0.9.0-dev)
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

public class LogMessage implements org.apache.thrift.TBase<LogMessage, LogMessage._Fields>, java.io.Serializable, Cloneable {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("LogMessage");

  private static final org.apache.thrift.protocol.TField LOG_LEVEL_FIELD_DESC = new org.apache.thrift.protocol.TField("logLevel", org.apache.thrift.protocol.TType.I32, (short)1);
  private static final org.apache.thrift.protocol.TField LOG_MESSAGE_FIELD_DESC = new org.apache.thrift.protocol.TField("logMessage", org.apache.thrift.protocol.TType.STRING, (short)2);
  private static final org.apache.thrift.protocol.TField PROGRAM_NAME_FIELD_DESC = new org.apache.thrift.protocol.TField("programName", org.apache.thrift.protocol.TType.STRING, (short)3);
  private static final org.apache.thrift.protocol.TField TIMESTAMP_FIELD_DESC = new org.apache.thrift.protocol.TField("timestamp", org.apache.thrift.protocol.TType.I64, (short)4);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new LogMessageStandardSchemeFactory());
    schemes.put(TupleScheme.class, new LogMessageTupleSchemeFactory());
  }

  public int logLevel; // required
  public String logMessage; // required
  public String programName; // required
  public long timestamp; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    LOG_LEVEL((short)1, "logLevel"),
    LOG_MESSAGE((short)2, "logMessage"),
    PROGRAM_NAME((short)3, "programName"),
    TIMESTAMP((short)4, "timestamp");

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
        case 1: // LOG_LEVEL
          return LOG_LEVEL;
        case 2: // LOG_MESSAGE
          return LOG_MESSAGE;
        case 3: // PROGRAM_NAME
          return PROGRAM_NAME;
        case 4: // TIMESTAMP
          return TIMESTAMP;
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
  private static final int __LOGLEVEL_ISSET_ID = 0;
  private static final int __TIMESTAMP_ISSET_ID = 1;
  private byte __isset_bitfield = 0;
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.LOG_LEVEL, new org.apache.thrift.meta_data.FieldMetaData("logLevel", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32        , "int")));
    tmpMap.put(_Fields.LOG_MESSAGE, new org.apache.thrift.meta_data.FieldMetaData("logMessage", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.PROGRAM_NAME, new org.apache.thrift.meta_data.FieldMetaData("programName", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.TIMESTAMP, new org.apache.thrift.meta_data.FieldMetaData("timestamp", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(LogMessage.class, metaDataMap);
  }

  public LogMessage() {
  }

  public LogMessage(
    int logLevel,
    String logMessage,
    String programName,
    long timestamp)
  {
    this();
    this.logLevel = logLevel;
    setLogLevelIsSet(true);
    this.logMessage = logMessage;
    this.programName = programName;
    this.timestamp = timestamp;
    setTimestampIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public LogMessage(LogMessage other) {
    __isset_bitfield = other.__isset_bitfield;
    this.logLevel = other.logLevel;
    if (other.isSetLogMessage()) {
      this.logMessage = other.logMessage;
    }
    if (other.isSetProgramName()) {
      this.programName = other.programName;
    }
    this.timestamp = other.timestamp;
  }

  public LogMessage deepCopy() {
    return new LogMessage(this);
  }

  @Override
  public void clear() {
    setLogLevelIsSet(false);
    this.logLevel = 0;
    this.logMessage = null;
    this.programName = null;
    setTimestampIsSet(false);
    this.timestamp = 0;
  }

  public int getLogLevel() {
    return this.logLevel;
  }

  public LogMessage setLogLevel(int logLevel) {
    this.logLevel = logLevel;
    setLogLevelIsSet(true);
    return this;
  }

  public void unsetLogLevel() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __LOGLEVEL_ISSET_ID);
  }

  /** Returns true if field logLevel is set (has been assigned a value) and false otherwise */
  public boolean isSetLogLevel() {
    return EncodingUtils.testBit(__isset_bitfield, __LOGLEVEL_ISSET_ID);
  }

  public void setLogLevelIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __LOGLEVEL_ISSET_ID, value);
  }

  public String getLogMessage() {
    return this.logMessage;
  }

  public LogMessage setLogMessage(String logMessage) {
    this.logMessage = logMessage;
    return this;
  }

  public void unsetLogMessage() {
    this.logMessage = null;
  }

  /** Returns true if field logMessage is set (has been assigned a value) and false otherwise */
  public boolean isSetLogMessage() {
    return this.logMessage != null;
  }

  public void setLogMessageIsSet(boolean value) {
    if (!value) {
      this.logMessage = null;
    }
  }

  public String getProgramName() {
    return this.programName;
  }

  public LogMessage setProgramName(String programName) {
    this.programName = programName;
    return this;
  }

  public void unsetProgramName() {
    this.programName = null;
  }

  /** Returns true if field programName is set (has been assigned a value) and false otherwise */
  public boolean isSetProgramName() {
    return this.programName != null;
  }

  public void setProgramNameIsSet(boolean value) {
    if (!value) {
      this.programName = null;
    }
  }

  public long getTimestamp() {
    return this.timestamp;
  }

  public LogMessage setTimestamp(long timestamp) {
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

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case LOG_LEVEL:
      if (value == null) {
        unsetLogLevel();
      } else {
        setLogLevel((Integer)value);
      }
      break;

    case LOG_MESSAGE:
      if (value == null) {
        unsetLogMessage();
      } else {
        setLogMessage((String)value);
      }
      break;

    case PROGRAM_NAME:
      if (value == null) {
        unsetProgramName();
      } else {
        setProgramName((String)value);
      }
      break;

    case TIMESTAMP:
      if (value == null) {
        unsetTimestamp();
      } else {
        setTimestamp((Long)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case LOG_LEVEL:
      return Integer.valueOf(getLogLevel());

    case LOG_MESSAGE:
      return getLogMessage();

    case PROGRAM_NAME:
      return getProgramName();

    case TIMESTAMP:
      return Long.valueOf(getTimestamp());

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case LOG_LEVEL:
      return isSetLogLevel();
    case LOG_MESSAGE:
      return isSetLogMessage();
    case PROGRAM_NAME:
      return isSetProgramName();
    case TIMESTAMP:
      return isSetTimestamp();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof LogMessage)
      return this.equals((LogMessage)that);
    return false;
  }

  public boolean equals(LogMessage that) {
    if (that == null)
      return false;

    boolean this_present_logLevel = true;
    boolean that_present_logLevel = true;
    if (this_present_logLevel || that_present_logLevel) {
      if (!(this_present_logLevel && that_present_logLevel))
        return false;
      if (this.logLevel != that.logLevel)
        return false;
    }

    boolean this_present_logMessage = true && this.isSetLogMessage();
    boolean that_present_logMessage = true && that.isSetLogMessage();
    if (this_present_logMessage || that_present_logMessage) {
      if (!(this_present_logMessage && that_present_logMessage))
        return false;
      if (!this.logMessage.equals(that.logMessage))
        return false;
    }

    boolean this_present_programName = true && this.isSetProgramName();
    boolean that_present_programName = true && that.isSetProgramName();
    if (this_present_programName || that_present_programName) {
      if (!(this_present_programName && that_present_programName))
        return false;
      if (!this.programName.equals(that.programName))
        return false;
    }

    boolean this_present_timestamp = true;
    boolean that_present_timestamp = true;
    if (this_present_timestamp || that_present_timestamp) {
      if (!(this_present_timestamp && that_present_timestamp))
        return false;
      if (this.timestamp != that.timestamp)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  public int compareTo(LogMessage other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    LogMessage typedOther = (LogMessage)other;

    lastComparison = Boolean.valueOf(isSetLogLevel()).compareTo(typedOther.isSetLogLevel());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetLogLevel()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.logLevel, typedOther.logLevel);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetLogMessage()).compareTo(typedOther.isSetLogMessage());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetLogMessage()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.logMessage, typedOther.logMessage);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetProgramName()).compareTo(typedOther.isSetProgramName());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetProgramName()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.programName, typedOther.programName);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
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
    StringBuilder sb = new StringBuilder("LogMessage(");
    boolean first = true;

    sb.append("logLevel:");
    sb.append(this.logLevel);
    first = false;
    if (!first) sb.append(", ");
    sb.append("logMessage:");
    if (this.logMessage == null) {
      sb.append("null");
    } else {
      sb.append(this.logMessage);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("programName:");
    if (this.programName == null) {
      sb.append("null");
    } else {
      sb.append(this.programName);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("timestamp:");
    sb.append(this.timestamp);
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

  private static class LogMessageStandardSchemeFactory implements SchemeFactory {
    public LogMessageStandardScheme getScheme() {
      return new LogMessageStandardScheme();
    }
  }

  private static class LogMessageStandardScheme extends StandardScheme<LogMessage> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, LogMessage struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // LOG_LEVEL
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.logLevel = iprot.readI32();
              struct.setLogLevelIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // LOG_MESSAGE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.logMessage = iprot.readString();
              struct.setLogMessageIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // PROGRAM_NAME
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.programName = iprot.readString();
              struct.setProgramNameIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // TIMESTAMP
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.timestamp = iprot.readI64();
              struct.setTimestampIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, LogMessage struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      oprot.writeFieldBegin(LOG_LEVEL_FIELD_DESC);
      oprot.writeI32(struct.logLevel);
      oprot.writeFieldEnd();
      if (struct.logMessage != null) {
        oprot.writeFieldBegin(LOG_MESSAGE_FIELD_DESC);
        oprot.writeString(struct.logMessage);
        oprot.writeFieldEnd();
      }
      if (struct.programName != null) {
        oprot.writeFieldBegin(PROGRAM_NAME_FIELD_DESC);
        oprot.writeString(struct.programName);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldBegin(TIMESTAMP_FIELD_DESC);
      oprot.writeI64(struct.timestamp);
      oprot.writeFieldEnd();
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class LogMessageTupleSchemeFactory implements SchemeFactory {
    public LogMessageTupleScheme getScheme() {
      return new LogMessageTupleScheme();
    }
  }

  private static class LogMessageTupleScheme extends TupleScheme<LogMessage> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, LogMessage struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetLogLevel()) {
        optionals.set(0);
      }
      if (struct.isSetLogMessage()) {
        optionals.set(1);
      }
      if (struct.isSetProgramName()) {
        optionals.set(2);
      }
      if (struct.isSetTimestamp()) {
        optionals.set(3);
      }
      oprot.writeBitSet(optionals, 4);
      if (struct.isSetLogLevel()) {
        oprot.writeI32(struct.logLevel);
      }
      if (struct.isSetLogMessage()) {
        oprot.writeString(struct.logMessage);
      }
      if (struct.isSetProgramName()) {
        oprot.writeString(struct.programName);
      }
      if (struct.isSetTimestamp()) {
        oprot.writeI64(struct.timestamp);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, LogMessage struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(4);
      if (incoming.get(0)) {
        struct.logLevel = iprot.readI32();
        struct.setLogLevelIsSet(true);
      }
      if (incoming.get(1)) {
        struct.logMessage = iprot.readString();
        struct.setLogMessageIsSet(true);
      }
      if (incoming.get(2)) {
        struct.programName = iprot.readString();
        struct.setProgramNameIsSet(true);
      }
      if (incoming.get(3)) {
        struct.timestamp = iprot.readI64();
        struct.setTimestampIsSet(true);
      }
    }
  }

}

