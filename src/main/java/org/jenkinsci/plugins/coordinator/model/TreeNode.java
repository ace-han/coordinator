package org.jenkinsci.plugins.coordinator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.json.JsonConfig;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class TreeNode {
	private static final String[] REFLECTION_EXCLUDE_FIELDS = new String[] {"chidlren"};
	static final JsonConfig JSON_CONFIG;
	static {
		JSON_CONFIG = new JsonConfig();
		JSON_CONFIG.setRootClass(TreeNode.class);
		HashMap<String, Class<?>> classMap = new HashMap<String, Class<?>>(3);
		classMap.put("children", TreeNode.class);
		JSON_CONFIG.setClassMap(classMap);
		// save the output log from unnecessary large size
		JSON_CONFIG.setExcludes(new String[]{"parent"});
	}
	
	private String id;
	private String text;
	private String icon;
	private TreeNode parent;

	private List<TreeNode> children;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}
	
	public TreeNode getParent() {
		return parent;
	}

	public void setParent(TreeNode parent) {
		this.parent = parent;
	}
	
	public List<TreeNode> getChildren() {
		if(this.children == null){
			this.children = new ArrayList<TreeNode>();
		}
		return this.children;
	}

	public void setChildren(List<TreeNode> children) {
		this.children = children;
	}

	public boolean isLeaf() {
		return getChildren().size() == 0;
	}
	
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public boolean equals(TreeNode node) {
		// since we have id a more significant field
		return EqualsBuilder.reflectionEquals(this, node, REFLECTION_EXCLUDE_FIELDS);
	}

	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this, REFLECTION_EXCLUDE_FIELDS);
	}

	public String toJsonString(){
		return null;
	}
}
