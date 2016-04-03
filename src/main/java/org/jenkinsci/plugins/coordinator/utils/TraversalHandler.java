package org.jenkinsci.plugins.coordinator.utils;

import org.jenkinsci.plugins.coordinator.model.TreeNode;

public interface TraversalHandler {
	void doTraversal(TreeNode node);
}