package com.github.mistertea.mcts;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class TreeNode {
	private static final int MAX_VALUE = 1;
	static Random r = new Random();
	static int nActions = 2;
    static double epsilon = 1e-6;

    TreeNode[] children;
	int nVisits = 0;
	int totValue = 0;
	boolean selfMove;

	public TreeNode(boolean selfMove) {
		this.selfMove = selfMove;
	}

    public void selectAction() {
        List<TreeNode> visited = new LinkedList<TreeNode>();
        TreeNode cur = this;
        visited.add(this);
        while (!cur.isLeaf()) {
            cur = cur.select();
            // System.out.println("Adding: " + cur);
            visited.add(cur);
        }
        cur.expand();
        TreeNode newNode = cur.select();
        visited.add(newNode);
		int value = rollOut(newNode);
        for (TreeNode node : visited) {
            // would need extra logic for n-player game
            // System.out.println(node);
            node.updateStats(value);
        }
    }

    public void expand() {
        children = new TreeNode[nActions];
        for (int i=0; i<nActions; i++) {
			children[i] = new TreeNode(!selfMove);
        }
    }

    private TreeNode select() {
        TreeNode selected = null;
        double bestValue = Double.MIN_VALUE;
        for (TreeNode c : children) {
            double uctValue =
                    c.totValue / (c.nVisits + epsilon) +
 Math.sqrt(Math.log(nVisits + 1.0) / (c.nVisits + epsilon))
					+
                            r.nextDouble() * epsilon;
            // small random number to break ties randomly in unexpanded nodes
            // System.out.println("UCT value = " + uctValue);
            if (uctValue > bestValue) {
                selected = c;
                bestValue = uctValue;
            }
        }
        // System.out.println("Returning: " + selected);
        return selected;
    }

    public boolean isLeaf() {
        return children == null;
    }

	public int rollOut(TreeNode tn) {
        // ultimately a roll out will end in some value
        // assume for now that it ends in a win or a loss
        // and just return this at random
        return r.nextInt(2);
    }

	public void updateStats(int value) {
        nVisits++;
		if (selfMove) {
			totValue += value;
		} else {
			totValue += MAX_VALUE - value;
		}
    }

    public int arity() {
        return children == null ? 0 : children.length;
    }
}