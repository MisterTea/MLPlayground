/**
 * Autogenerated by Thrift Compiler (1.0.0-dev)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.github.mistertea.boardgame.landshark;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
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
import javax.annotation.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked"})
@Generated(value = "Autogenerated by Thrift Compiler (1.0.0-dev)", date = "2014-1-15")
public class AuctionState implements org.apache.thrift.TBase<AuctionState, AuctionState._Fields>, java.io.Serializable, Cloneable, Comparable<AuctionState> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("AuctionState");

  private static final org.apache.thrift.protocol.TField PROPERTY_FIELD_DESC = new org.apache.thrift.protocol.TField("property", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField AUCTION_OWNER_FIELD_DESC = new org.apache.thrift.protocol.TField("auctionOwner", org.apache.thrift.protocol.TType.STRING, (short)2);
  private static final org.apache.thrift.protocol.TField BIDS_FIELD_DESC = new org.apache.thrift.protocol.TField("bids", org.apache.thrift.protocol.TType.MAP, (short)3);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new AuctionStateStandardSchemeFactory());
    schemes.put(TupleScheme.class, new AuctionStateTupleSchemeFactory());
  }

  public String property; // required
  public String auctionOwner; // required
  public Map<String,Integer> bids; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    PROPERTY((short)1, "property"),
    AUCTION_OWNER((short)2, "auctionOwner"),
    BIDS((short)3, "bids");

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
        case 1: // PROPERTY
          return PROPERTY;
        case 2: // AUCTION_OWNER
          return AUCTION_OWNER;
        case 3: // BIDS
          return BIDS;
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
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.PROPERTY, new org.apache.thrift.meta_data.FieldMetaData("property", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.AUCTION_OWNER, new org.apache.thrift.meta_data.FieldMetaData("auctionOwner", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.BIDS, new org.apache.thrift.meta_data.FieldMetaData("bids", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.MapMetaData(org.apache.thrift.protocol.TType.MAP, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING), 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(AuctionState.class, metaDataMap);
  }

  public AuctionState() {
    this.bids = new HashMap<String,Integer>();

  }

  public AuctionState(
    String property,
    String auctionOwner,
    Map<String,Integer> bids)
  {
    this();
    this.property = property;
    this.auctionOwner = auctionOwner;
    this.bids = bids;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public AuctionState(AuctionState other) {
    if (other.isSetProperty()) {
      this.property = other.property;
    }
    if (other.isSetAuctionOwner()) {
      this.auctionOwner = other.auctionOwner;
    }
    if (other.isSetBids()) {
      Map<String,Integer> __this__bids = new HashMap<String,Integer>(other.bids);
      this.bids = __this__bids;
    }
  }

  public AuctionState deepCopy() {
    return new AuctionState(this);
  }

  @Override
  public void clear() {
    this.property = null;
    this.auctionOwner = null;
    this.bids = new HashMap<String,Integer>();

  }

  public String getProperty() {
    return this.property;
  }

  public AuctionState setProperty(String property) {
    this.property = property;
    return this;
  }

  public void unsetProperty() {
    this.property = null;
  }

  /** Returns true if field property is set (has been assigned a value) and false otherwise */
  public boolean isSetProperty() {
    return this.property != null;
  }

  public void setPropertyIsSet(boolean value) {
    if (!value) {
      this.property = null;
    }
  }

  public String getAuctionOwner() {
    return this.auctionOwner;
  }

  public AuctionState setAuctionOwner(String auctionOwner) {
    this.auctionOwner = auctionOwner;
    return this;
  }

  public void unsetAuctionOwner() {
    this.auctionOwner = null;
  }

  /** Returns true if field auctionOwner is set (has been assigned a value) and false otherwise */
  public boolean isSetAuctionOwner() {
    return this.auctionOwner != null;
  }

  public void setAuctionOwnerIsSet(boolean value) {
    if (!value) {
      this.auctionOwner = null;
    }
  }

  public int getBidsSize() {
    return (this.bids == null) ? 0 : this.bids.size();
  }

  public void putToBids(String key, int val) {
    if (this.bids == null) {
      this.bids = new HashMap<String,Integer>();
    }
    this.bids.put(key, val);
  }

  public Map<String,Integer> getBids() {
    return this.bids;
  }

  public AuctionState setBids(Map<String,Integer> bids) {
    this.bids = bids;
    return this;
  }

  public void unsetBids() {
    this.bids = null;
  }

  /** Returns true if field bids is set (has been assigned a value) and false otherwise */
  public boolean isSetBids() {
    return this.bids != null;
  }

  public void setBidsIsSet(boolean value) {
    if (!value) {
      this.bids = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case PROPERTY:
      if (value == null) {
        unsetProperty();
      } else {
        setProperty((String)value);
      }
      break;

    case AUCTION_OWNER:
      if (value == null) {
        unsetAuctionOwner();
      } else {
        setAuctionOwner((String)value);
      }
      break;

    case BIDS:
      if (value == null) {
        unsetBids();
      } else {
        setBids((Map<String,Integer>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case PROPERTY:
      return getProperty();

    case AUCTION_OWNER:
      return getAuctionOwner();

    case BIDS:
      return getBids();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case PROPERTY:
      return isSetProperty();
    case AUCTION_OWNER:
      return isSetAuctionOwner();
    case BIDS:
      return isSetBids();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof AuctionState)
      return this.equals((AuctionState)that);
    return false;
  }

  public boolean equals(AuctionState that) {
    if (that == null)
      return false;

    boolean this_present_property = true && this.isSetProperty();
    boolean that_present_property = true && that.isSetProperty();
    if (this_present_property || that_present_property) {
      if (!(this_present_property && that_present_property))
        return false;
      if (!this.property.equals(that.property))
        return false;
    }

    boolean this_present_auctionOwner = true && this.isSetAuctionOwner();
    boolean that_present_auctionOwner = true && that.isSetAuctionOwner();
    if (this_present_auctionOwner || that_present_auctionOwner) {
      if (!(this_present_auctionOwner && that_present_auctionOwner))
        return false;
      if (!this.auctionOwner.equals(that.auctionOwner))
        return false;
    }

    boolean this_present_bids = true && this.isSetBids();
    boolean that_present_bids = true && that.isSetBids();
    if (this_present_bids || that_present_bids) {
      if (!(this_present_bids && that_present_bids))
        return false;
      if (!this.bids.equals(that.bids))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public int compareTo(AuctionState other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetProperty()).compareTo(other.isSetProperty());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetProperty()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.property, other.property);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetAuctionOwner()).compareTo(other.isSetAuctionOwner());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetAuctionOwner()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.auctionOwner, other.auctionOwner);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetBids()).compareTo(other.isSetBids());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetBids()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.bids, other.bids);
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
    StringBuilder sb = new StringBuilder("AuctionState(");
    boolean first = true;

    sb.append("property:");
    if (this.property == null) {
      sb.append("null");
    } else {
      sb.append(this.property);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("auctionOwner:");
    if (this.auctionOwner == null) {
      sb.append("null");
    } else {
      sb.append(this.auctionOwner);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("bids:");
    if (this.bids == null) {
      sb.append("null");
    } else {
      sb.append(this.bids);
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
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class AuctionStateStandardSchemeFactory implements SchemeFactory {
    public AuctionStateStandardScheme getScheme() {
      return new AuctionStateStandardScheme();
    }
  }

  private static class AuctionStateStandardScheme extends StandardScheme<AuctionState> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, AuctionState struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // PROPERTY
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.property = iprot.readString();
              struct.setPropertyIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // AUCTION_OWNER
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.auctionOwner = iprot.readString();
              struct.setAuctionOwnerIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // BIDS
            if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
              {
                org.apache.thrift.protocol.TMap _map16 = iprot.readMapBegin();
                struct.bids = new HashMap<String,Integer>(2*_map16.size);
                for (int _i17 = 0; _i17 < _map16.size; ++_i17)
                {
                  String _key18;
                  int _val19;
                  _key18 = iprot.readString();
                  _val19 = iprot.readI32();
                  struct.bids.put(_key18, _val19);
                }
                iprot.readMapEnd();
              }
              struct.setBidsIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, AuctionState struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.property != null) {
        oprot.writeFieldBegin(PROPERTY_FIELD_DESC);
        oprot.writeString(struct.property);
        oprot.writeFieldEnd();
      }
      if (struct.auctionOwner != null) {
        oprot.writeFieldBegin(AUCTION_OWNER_FIELD_DESC);
        oprot.writeString(struct.auctionOwner);
        oprot.writeFieldEnd();
      }
      if (struct.bids != null) {
        oprot.writeFieldBegin(BIDS_FIELD_DESC);
        {
          oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.I32, struct.bids.size()));
          for (Map.Entry<String, Integer> _iter20 : struct.bids.entrySet())
          {
            oprot.writeString(_iter20.getKey());
            oprot.writeI32(_iter20.getValue());
          }
          oprot.writeMapEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class AuctionStateTupleSchemeFactory implements SchemeFactory {
    public AuctionStateTupleScheme getScheme() {
      return new AuctionStateTupleScheme();
    }
  }

  private static class AuctionStateTupleScheme extends TupleScheme<AuctionState> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, AuctionState struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetProperty()) {
        optionals.set(0);
      }
      if (struct.isSetAuctionOwner()) {
        optionals.set(1);
      }
      if (struct.isSetBids()) {
        optionals.set(2);
      }
      oprot.writeBitSet(optionals, 3);
      if (struct.isSetProperty()) {
        oprot.writeString(struct.property);
      }
      if (struct.isSetAuctionOwner()) {
        oprot.writeString(struct.auctionOwner);
      }
      if (struct.isSetBids()) {
        {
          oprot.writeI32(struct.bids.size());
          for (Map.Entry<String, Integer> _iter21 : struct.bids.entrySet())
          {
            oprot.writeString(_iter21.getKey());
            oprot.writeI32(_iter21.getValue());
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, AuctionState struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(3);
      if (incoming.get(0)) {
        struct.property = iprot.readString();
        struct.setPropertyIsSet(true);
      }
      if (incoming.get(1)) {
        struct.auctionOwner = iprot.readString();
        struct.setAuctionOwnerIsSet(true);
      }
      if (incoming.get(2)) {
        {
          org.apache.thrift.protocol.TMap _map22 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.I32, iprot.readI32());
          struct.bids = new HashMap<String,Integer>(2*_map22.size);
          for (int _i23 = 0; _i23 < _map22.size; ++_i23)
          {
            String _key24;
            int _val25;
            _key24 = iprot.readString();
            _val25 = iprot.readI32();
            struct.bids.put(_key24, _val25);
          }
        }
        struct.setBidsIsSet(true);
      }
    }
  }

}

