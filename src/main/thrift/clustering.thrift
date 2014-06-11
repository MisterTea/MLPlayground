namespace java com.github.mistertea.clustering
namespace scala com.github.mistertea.clustering.scala

struct DecisionTreeNode {
  1:i32 left,
  2:i32 right,
  3:i32 splitIndex,
}

struct DecisionTree {
  1:list<DecisionTreeNode> nodes = [],
}
