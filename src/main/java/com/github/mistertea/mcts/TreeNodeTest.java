package com.github.mistertea.mcts;


/**
 * Created by Simon M. Lucas
 * sml@essex.ac.uk
 * Date: 03-Dec-2010
 * Time: 21:51:31
 */
public class TreeNodeTest {
    public static void main(String[] args) {
		AbstractTreeNode tn = new TicTacToeTreeNode();
        ElapsedTimer t = new ElapsedTimer();
		int n = 1000000;
        for (int i=0; i<n; i++) {
            tn.selectAction();
        }
        System.out.println(t);
        TreeView tv = new TreeView(tn);
        tv.showTree("After " + n + " play outs");
        System.out.println("Done");
    }
}