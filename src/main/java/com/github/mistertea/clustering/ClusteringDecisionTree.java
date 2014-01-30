package com.github.mistertea.clustering;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ClusteringDecisionTree {
  private static final int MAX_FEATURE = 136;
  private static final int IMBALANCE_FACTOR = 20;
  private static final int MAX_DEPTH = 4;

  public static class ClusteringDecisionTreeNode implements Runnable {

    private List<Set<Integer>> designMatrix;
    private Set<Integer> inputSet;
    int split;
    List<Integer> auxSplits = new ArrayList<Integer>();
    ClusteringDecisionTreeNode left;
    ClusteringDecisionTreeNode right;
    private int depth;
    Set<Integer> bannedSplits = new HashSet<Integer>();

    public ClusteringDecisionTreeNode(List<Set<Integer>> designMatrix, int depth) {
      this.designMatrix = designMatrix;
      inputSet = new HashSet<Integer>();
      this.depth = depth;
      for (int a = 0; a < designMatrix.size(); a++) {
        inputSet.add(a);
      }
    }

    public ClusteringDecisionTreeNode(List<Set<Integer>> designMatrix,
        Set<Integer> inputSet, int depth) {
      this.designMatrix = designMatrix;
      this.inputSet = inputSet;
      this.depth = depth;
    }
    
    public ClusteringDecisionTreeNode(DecisionTree tree, int index) {
      DecisionTreeNode node = tree.nodes.get(index);
      split = node.splitIndex;
      if (node.left>0) {
        left = new ClusteringDecisionTreeNode(tree, node.left);
      }
      if (node.right>0) {
        right = new ClusteringDecisionTreeNode(tree, node.right);
      }
    }

    @Override
    public void run() {
      int bestSplit = split = -1;
      auxSplits.clear();

      for (int a = 0; a < MAX_FEATURE; a++) {
        if (bannedSplits.contains(a)) {
          continue;
        }
        System.out.print(".");
        // Try to split on a
        int countPositive = 0;
        for (int i : inputSet) {
          Set<Integer> l = designMatrix.get(i);
          if (l.contains(a)) {
            countPositive++;
          }
        }
        int countNegative = inputSet.size() - countPositive;
        if (countNegative * IMBALANCE_FACTOR < countPositive
            || countPositive * IMBALANCE_FACTOR < countNegative) {
          continue;
        }
        int currentSplit = Math.abs(inputSet.size() / 2 - countPositive);
        if (bestSplit == -1 || currentSplit < bestSplit) {
          bestSplit = currentSplit;
          split = a;
        }
      }

      if (false && bestSplit == -1) {
        // Monte-carlo last ditch attempt at splitting

        Random rng = new Random();
        boolean done = false;
        for (int numSplits = 2; numSplits <= 8 && !done; numSplits++) {
          System.out.println("SPLITTING: " + numSplits);
          for (int trials = 0; trials < 100 && !done; trials++) {
            if (trials % 10 == 0) {
              System.out.print(".");
            }
            auxSplits.clear();
            for (int a = 0; a < numSplits; a++) {
              auxSplits.add(rng.nextInt(MAX_FEATURE));
            }

            int countPositive = 0;
            for (int i : inputSet) {
              Set<Integer> l = designMatrix.get(i);
              for (Integer item : auxSplits) {
                if (l.contains(item)) {
                  countPositive++;
                  break;
                }
              }
            }
            int countNegative = inputSet.size() - countPositive;
            if (countNegative * IMBALANCE_FACTOR < countPositive
                || countPositive * IMBALANCE_FACTOR < countNegative) {
              auxSplits.clear();
              continue;
            }
            System.out.println("GOT MONTE CARLO SPLIT " + countNegative + " "
                + countPositive);
            for (int i : auxSplits) {
              System.out.println(i);
            }
            done = true;
          }
        }
      }

      System.out.println("");

      if (depth > 0) {
        if (bestSplit == -1 && auxSplits.isEmpty()) {
          return;
        }
        Set<Integer> leftIndices = new HashSet<Integer>();
        Set<Integer> rightIndices = new HashSet<Integer>();
        if (auxSplits.isEmpty()) {
          for (int i : inputSet) {
            Set<Integer> l = designMatrix.get(i);
            if (l.contains(split)) {
              rightIndices.add(i);
            } else {
              leftIndices.add(i);
            }
          }
        } else {
          for (int i : inputSet) {
            Set<Integer> l = designMatrix.get(i);
            if (!Collections.disjoint(auxSplits, l)) {
              rightIndices.add(i);
            } else {
              leftIndices.add(i);
            }
          }
        }
        left = new ClusteringDecisionTreeNode(designMatrix, leftIndices,
            depth - 1);
        Thread leftThread = new Thread(left);
        leftThread.start();

        right = new ClusteringDecisionTreeNode(designMatrix, rightIndices,
            depth - 1);
        Thread rightThread = new Thread(right);
        rightThread.start();

        try {
          leftThread.join();
          rightThread.join();
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        if (depth > 1 && split != -1) {
          if (left.left == null || right.left == null) {
            System.out.println(depth + " BANNING " + split);
            bannedSplits.add(split);
            System.out.println("***BAN LIST***");
            for (int i : bannedSplits) {
              System.out.println(i);
            }
            left = null;
            right = null;
            run();
          }
        }
      } else {
        double[] mean = new double[MAX_FEATURE];
        for (int i : inputSet) {
          Set<Integer> l = designMatrix.get(i);
          for (int a = 0; a < MAX_FEATURE; a++) {
            mean[a] += l.contains(a) ? 1 : 0;
            // System.out.print((l.contains(a) ? 1 : 0) + " ");
          }
          // System.out.println();
        }
        for (int a = 0; a < mean.length; a++) {
          mean[a] /= inputSet.size();
        }
      }
    }

    public void print(int depth) {
      if (left != null) {
        left.print(depth + 2);
      }
      for (int a = 0; a < depth; a++) {
        System.out.print(" ");
      }
      System.out.println(split + " " + inputSet.size());
      double[] mean = new double[MAX_FEATURE];
      for (int i : inputSet) {
        Set<Integer> l = designMatrix.get(i);
        for (int j : l) {
          mean[j] += 1.0;
        }
      }
      for (int a = 0; a < mean.length; a++) {
        mean[a] /= inputSet.size();
      }
      double variance = 0;
      for (int i : inputSet) {
        Set<Integer> l = designMatrix.get(i);
        for (int a = 0; a < MAX_FEATURE; a++) {
          variance += (mean[a] - (l.contains(a) ? 1 : 0))
              * (mean[a] - (l.contains(a) ? 1 : 0));
        }
      }
      variance /= inputSet.size();
      variance /= MAX_FEATURE;
      // System.out.println("VARIANCE: " + variance);
      if (right != null) {
        right.print(depth + 2);
      }
    }

    public DecisionTree dump() {
      DecisionTree tree = new DecisionTree();
      return dumpNode(tree);
    }

    private DecisionTree dumpNode(DecisionTree tree) {
      DecisionTreeNode node = new DecisionTreeNode();
      node.splitIndex = split;
      tree.nodes.add(node);
      if (left != null) {
        node.left = tree.nodes.size();
        left.dumpNode(tree);
      }
      if (right != null) {
        node.right = tree.nodes.size();
        right.dumpNode(tree);
      }
      return tree;
    }
    
  }

  public static void main(String[] args) throws IOException {
    // 1317513291 id-560620 0 |user 1 9 11 13 23 16 18 17 19 15 43 14 39 30 66
    // 50 27 104 20 |id-552077 |id-555224 |id-555528 |id-559744 |id-559855
    // |id-560290 |id-560518 |id-560620 |id-563115 |id-563582 |id-563643
    // |id-563787 |id-563846 |id-563938 |id-564335 |id-564418 |id-564604
    // |id-565364 |id-565479 |id-565515 |id-565533 |id-565561 |id-565589
    // |id-565648 |id-565747 |id-565822
    List<Set<Integer>> designMatrix = new ArrayList<>();
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    String input=null;

    Random rng = new Random(1L);
    System.out.println("Loading data...");
    for (int record = 0; record < 1000000; record++) {
      int totalSkip = 10+rng.nextInt(10);
      for (int skip=0;skip<totalSkip;skip++) {
        input = br.readLine();
        if (input == null) {
          break;
        }
      }
      String[] tokens = input.split("\\s+");
      long timestamp = Long.parseLong(tokens[0]);
      String newsShown = tokens[1];
      int click = Integer.parseInt(tokens[2]);
      Set<Integer> features = new HashSet<Integer>();
      for (int i = 4; i < tokens.length; i++) {
        String token = tokens[i];
        if (token.charAt(0) == '|') {
        } else {
          features.add(Integer.parseInt(token) - 1);
        }
      }
      // System.out.println(input);
      designMatrix.add(features);
      if (record % 1000 == 0) {
        System.out.print(".");
      }
    }
    System.out.println("Done");

    // for (int a = 0; a < 50; a++) {
    // Set<Integer> features = new HashSet<Integer>();
    // for (int b = 0; b < MAX_FEATURE; b++) {
    // if (rng.nextBoolean()) {
    // features.add(b);
    // System.out.print("1 ");
    // } else {
    // System.out.print("0 ");
    // }
    // }
    // System.out.println();
    // designMatrix.add(features);
    // }
    // System.out.println("***");

    ClusteringDecisionTreeNode node = new ClusteringDecisionTreeNode(
        designMatrix, MAX_DEPTH);
    node.run();
    node.print(0);
    System.out.println(node.dump());
    System.out.println(new ClusteringDecisionTreeNode(node.dump(),0).dump());
  }
}
