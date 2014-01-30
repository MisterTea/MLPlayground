namespace java com.github.mistertea.clustering

struct DecisionTreeNode {
  1:i32 left,
  2:i32 right,
  3:i32 splitIndex,
}

struct DecisionTree {
  1:list<DecisionTreeNode> nodes = [],
}
