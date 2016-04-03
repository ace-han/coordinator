package org.jenkinsci.plugins.coordinator.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.coordinator.model.TreeNode;
import org.jenkinsci.plugins.coordinator.model.TreeNode.State;

public class TreeNodeUtils {
	

	/**
	 * Assumption is that, left(origin) and right(requestNode) is identical in structure
	 * This function merge from right to left recursively
	 * 
	 * please do note about the order of the parameters
	 * @param originNode
	 * @param requestNode
	 */
	public static void mergeState4Execution(TreeNode originNode, TreeNode requestNode) {
		State rState = requestNode.getState();
		State oState = originNode.getState();
		
		oState.opened = rState.opened;
		oState.disabled = rState.disabled;
		oState.selected = rState.selected;
		oState.checked = rState.checked;
		oState.undetermined = rState.undetermined;
		// don't merge below two properties, which will jerpodize project's root node afterwards
//		lstate.breaking = rstate.breaking;
//		lstate.execPattern = rstate.execPattern;
		
		// for legacy support 
		if(StringUtils.isEmpty(oState.execPattern)){
			oState.breaking = true;
			if(originNode.isLeaf()){
				oState.execPattern = "serial";
			}else{
				oState.execPattern = originNode.getType().substring("breaking-".length());
			}
		}
		// for build history display, we need to set this two fields
		rState.breaking = oState.breaking;
		rState.execPattern = oState.execPattern;
		for(int i=0; i<originNode.getChildren().size(); i++){
			mergeState4Execution(originNode.getChildren().get(i), 
					requestNode.getChildren().get(i));
		}
	}
	
	
	public static List<TreeNode> getFlatNodes(TreeNode node, boolean byDepth) {
		ArrayList<TreeNode> result = new ArrayList<TreeNode>();
		if(byDepth){
			flatNodesByDepth(node, result);
		} else {
			flatNodesByBreadth(node, result);
		}
		return result;
	}
	
	private static void flatNodesByBreadth(TreeNode node, ArrayList<TreeNode> list) {
		list.add(node);
		if(node.getChildren().isEmpty()){
			return;
		}
		list.addAll(node.getChildren());
		for(TreeNode child: node.getChildren()){
			flatNodesByBreadth(child, list);
		}
	}

	private static void flatNodesByDepth(TreeNode node, List<TreeNode> list){
		list.add(node);
		for(TreeNode child: node.getChildren()){
			flatNodesByDepth(child, list);
		}
	}
	
	public static void preOrderTraversal(TreeNode node, TraversalHandler handler){
		boolean canContinue = handler.doTraversal(node);
		if(!canContinue){return;}
		for(TreeNode c: node.getChildren()){
			preOrderTraversal(c, handler);
		}
	}
}
