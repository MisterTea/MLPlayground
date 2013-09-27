package com.github.mistertea.mcts;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public abstract class AbstractTreeNode {
	private static final double MAX_VALUE = 1;
	static double epsilon = 1e-6;
	private Random rng = new Random(1L);

	List<AbstractTreeNode> children;
	int nVisits = 0;
	double totValue = 0.0;
	boolean selfMove;

	public AbstractTreeNode(boolean selfMove) {
		this.selfMove = selfMove;
	}

	public void selectAction() {
		List<AbstractTreeNode> visited = new LinkedList<AbstractTreeNode>();
		AbstractTreeNode cur = this;
		visited.add(this);
		while (!cur.isLeaf()) {
			cur = cur.select();
			// System.out.println("Adding: " + cur);
			visited.add(cur);
		}
		Double value = cur.getTerminalValue();
		if (value == null) {
			cur.expand();
			AbstractTreeNode newNode = cur.select();
			visited.add(newNode);
			value = newNode.rollOut();
		}
		for (AbstractTreeNode node : visited) {
			// would need extra logic for n-player game
			// System.out.println(node);
			node.updateStats(value);
		}
	}

	public abstract void expand();

	public abstract Double getTerminalValue();

	protected AbstractTreeNode select() {
		AbstractTreeNode selected = null;
		double bestValue = Double.MIN_VALUE;
		for (AbstractTreeNode c : children) {
			double uctValue;
			if (c.nVisits == 0) {
				uctValue = rng.nextDouble() * 100000;
			} else {
				uctValue = c.totValue
						/ (c.nVisits + epsilon)
						+ Math.sqrt(Math.log(nVisits + 1.0)
								/ (c.nVisits + epsilon)) + rng.nextDouble()
						* epsilon;
				// small random number to break ties randomly in unexpanded
				// nodes
				// System.out.println("UCT value = " + uctValue);
			}
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

	// ultimately a roll out will end in some value
	// assume for now that it ends in a win or a loss
	// and just return this at random
	public abstract double rollOut();

	public void updateStats(double value) {
		nVisits++;
		if (selfMove) {
			totValue += value;
		} else {
			totValue += MAX_VALUE - value;
		}
	}

	public int arity() {
		return children == null ? 0 : children.size();
	}
}