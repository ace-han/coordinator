package org.jenkinsci.plugins.coordinator.utils;

import org.jenkinsci.plugins.coordinator.model.TreeNode;

public interface TraversalHandler {
	/**
	 * 
	 * @param node
	 * @return true if it's okay to traverse further, false otherwise
	 */
	boolean doTraversal(TreeNode node);
}