//
// Autogenerated by Thrift Compiler (1.0.0-dev)
//
// DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
//


DecisionTreeNode = function(args) {
  this.left = null;
  this.right = null;
  this.splitIndex = null;
  if (args) {
    if (args.left !== undefined) {
      this.left = args.left;
    }
    if (args.right !== undefined) {
      this.right = args.right;
    }
    if (args.splitIndex !== undefined) {
      this.splitIndex = args.splitIndex;
    }
  }
};
DecisionTreeNode.prototype = {};
DecisionTreeNode.prototype.read = function(input) {
  input.readStructBegin();
  while (true)
  {
    var ret = input.readFieldBegin();
    var fname = ret.fname;
    var ftype = ret.ftype;
    var fid = ret.fid;
    if (ftype == Thrift.Type.STOP) {
      break;
    }
    switch (fid)
    {
      case 1:
      if (ftype == Thrift.Type.I32) {
        this.left = input.readI32().value;
      } else {
        input.skip(ftype);
      }
      break;
      case 2:
      if (ftype == Thrift.Type.I32) {
        this.right = input.readI32().value;
      } else {
        input.skip(ftype);
      }
      break;
      case 3:
      if (ftype == Thrift.Type.I32) {
        this.splitIndex = input.readI32().value;
      } else {
        input.skip(ftype);
      }
      break;
      default:
        input.skip(ftype);
    }
    input.readFieldEnd();
  }
  input.readStructEnd();
  return;
};

DecisionTreeNode.prototype.write = function(output) {
  output.writeStructBegin('DecisionTreeNode');
  if (this.left !== null && this.left !== undefined) {
    output.writeFieldBegin('left', Thrift.Type.I32, 1);
    output.writeI32(this.left);
    output.writeFieldEnd();
  }
  if (this.right !== null && this.right !== undefined) {
    output.writeFieldBegin('right', Thrift.Type.I32, 2);
    output.writeI32(this.right);
    output.writeFieldEnd();
  }
  if (this.splitIndex !== null && this.splitIndex !== undefined) {
    output.writeFieldBegin('splitIndex', Thrift.Type.I32, 3);
    output.writeI32(this.splitIndex);
    output.writeFieldEnd();
  }
  output.writeFieldStop();
  output.writeStructEnd();
  return;
};

DecisionTree = function(args) {
  this.nodes = [];
  if (args) {
    if (args.nodes !== undefined) {
      this.nodes = args.nodes;
    }
  }
};
DecisionTree.prototype = {};
DecisionTree.prototype.read = function(input) {
  input.readStructBegin();
  while (true)
  {
    var ret = input.readFieldBegin();
    var fname = ret.fname;
    var ftype = ret.ftype;
    var fid = ret.fid;
    if (ftype == Thrift.Type.STOP) {
      break;
    }
    switch (fid)
    {
      case 1:
      if (ftype == Thrift.Type.LIST) {
        var _size0 = 0;
        var _rtmp34;
        this.nodes = [];
        var _etype3 = 0;
        _rtmp34 = input.readListBegin();
        _etype3 = _rtmp34.etype;
        _size0 = _rtmp34.size;
        for (var _i5 = 0; _i5 < _size0; ++_i5)
        {
          var elem6 = null;
          elem6 = new DecisionTreeNode();
          elem6.read(input);
          this.nodes.push(elem6);
        }
        input.readListEnd();
      } else {
        input.skip(ftype);
      }
      break;
      case 0:
        input.skip(ftype);
        break;
      default:
        input.skip(ftype);
    }
    input.readFieldEnd();
  }
  input.readStructEnd();
  return;
};

DecisionTree.prototype.write = function(output) {
  output.writeStructBegin('DecisionTree');
  if (this.nodes !== null && this.nodes !== undefined) {
    output.writeFieldBegin('nodes', Thrift.Type.LIST, 1);
    output.writeListBegin(Thrift.Type.STRUCT, this.nodes.length);
    for (var iter7 in this.nodes)
    {
      if (this.nodes.hasOwnProperty(iter7))
      {
        iter7 = this.nodes[iter7];
        iter7.write(output);
      }
    }
    output.writeListEnd();
    output.writeFieldEnd();
  }
  output.writeFieldStop();
  output.writeStructEnd();
  return;
};

