package com.github.mistertea.mcts;

import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.Assert;

public class TicTacToeTreeNode extends AbstractTreeNode {
	private static final int EMPTY_SQUARE = 0;
	private static final int X_SQUARE = 1;
	private static final int O_SQUARE = -1;
	int board[][];
	int numMoves;

	public TicTacToeTreeNode() {
		super(true);
		board = new int[3][3];
		board[0][0] = X_SQUARE;
		board[0][1] = X_SQUARE;
		board[2][0] = O_SQUARE;
		board[2][1] = O_SQUARE;
		numMoves = 4;
	}

	public TicTacToeTreeNode(int board[][], int numMoves, boolean selfMove) {
		super(selfMove);
		this.board = board;
		this.numMoves = numMoves;
	}

	private int[][] deepCopy(int[][] original) {
		if (original == null) {
			return null;
		}

		final int[][] result = new int[original.length][];
		for (int i = 0; i < original.length; i++) {
			result[i] = Arrays.copyOf(original[i], original[i].length);
		}
		return result;
	}

	@Override
	public void expand() {
		children = new ArrayList<AbstractTreeNode>();
		for (int a = 0; a < 3; a++) {
			for (int b = 0; b < 3; b++) {
				if (board[a][b] == EMPTY_SQUARE) {
					board[a][b] = selfMove ? X_SQUARE : O_SQUARE;
					children.add(new TicTacToeTreeNode(deepCopy(board),
							numMoves + 1, !selfMove));
					board[a][b] = EMPTY_SQUARE;
				}
			}
		}
		System.out.println(numMoves + " " + children.size());
		Assert.assertTrue(!children.isEmpty());
	}

	@Override
	public double rollOut() {
		System.out.println("Rolling out");
		int tmpboard[][] = deepCopy(board);
		int tmpNumMoves = numMoves;
		boolean tmpselfMove = selfMove;
		AbstractTreeNode node = new TicTacToeTreeNode(tmpboard, tmpNumMoves,
				tmpselfMove);
		while (true) {
			Double value = node.getTerminalValue();
			if (value != null) {
				return value;
			}
			node.expand();
			node = node.select();
		}
	}

	@Override
	public Double getTerminalValue() {
		for (int a = 0; a < 2; a++) {
			int curPlayer = (a == 0) ? X_SQUARE : O_SQUARE;
			double valueIfWin = (a == 0) ? 1.0 : 0.0;

			if (board[0][0] == curPlayer && board[0][1] == curPlayer
					&& board[0][2] == curPlayer) {
				return valueIfWin;
			}
			if (board[1][0] == curPlayer && board[1][1] == curPlayer
					&& board[1][2] == curPlayer) {
				return valueIfWin;
			}
			if (board[2][0] == curPlayer && board[2][1] == curPlayer
					&& board[2][2] == curPlayer) {
				return valueIfWin;
			}

			if (board[0][0] == curPlayer && board[1][0] == curPlayer
					&& board[2][0] == curPlayer) {
				return valueIfWin;
			}
			if (board[0][1] == curPlayer && board[1][1] == curPlayer
					&& board[2][1] == curPlayer) {
				return valueIfWin;
			}
			if (board[0][2] == curPlayer && board[1][2] == curPlayer
					&& board[2][2] == curPlayer) {
				return valueIfWin;
			}

			if (board[0][0] == curPlayer && board[1][1] == curPlayer
					&& board[2][2] == curPlayer) {
				return valueIfWin;
			}
			if (board[0][2] == curPlayer && board[1][1] == curPlayer
					&& board[2][0] == curPlayer) {
				return valueIfWin;
			}
		}

		if (numMoves >= 9) {
			return 0.5;
		}

		return null;
	}
}
