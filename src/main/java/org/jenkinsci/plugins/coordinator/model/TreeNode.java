package org.jenkinsci.plugins.coordinator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class TreeNode {
	
	private static final String[] REFLECTION_EXCLUDE_FIELDS = new String[] {"chidlren"};
	static final JsonConfig JSON_CONFIG;
	static final TreeNode EMPTY_ROOT;	// for every tree initialization
	static {
		JSON_CONFIG = new JsonConfig();
		JSON_CONFIG.setRootClass(TreeNode.class);
		HashMap<String, Class<?>> classMap = new HashMap<String, Class<?>>(3);
		classMap.put("children", TreeNode.class);
		classMap.put("state", State.class);
		JSON_CONFIG.setClassMap(classMap);
		// save the output log from unnecessary large size
		JSON_CONFIG.setExcludes(new String[]{"parent", "jsonString", "leaf"});
		
		EMPTY_ROOT = new TreeNode();
		EMPTY_ROOT.setText("Root");
		
		// ROOT default is serial node
		EMPTY_ROOT.setType("serial");
		// EMPTY_ROOT.state.type = "serial";
	}
	
	
	private String id;
	private String text;
	private String type;
	
	private transient TreeNode parent;

	private List<TreeNode> children = new ArrayList<TreeNode>();
	
	private State state = new State();
	
	private int buildNumber;

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
		this.text = text.trim();
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public TreeNode getParent() {
		return parent;
	}

	public void setParent(TreeNode parent) {
		this.parent = parent;
	}
	
	public List<TreeNode> getChildren() {
		return this.children;
	}

	public void setChildren(List<TreeNode> children) {
		this.children = children;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}
	
	public boolean isLeaf() {
		return getChildren().size() == 0;
	}
	
	public boolean shouldChildrenSerialRun(){
		return "serial".equals(this.type);
	}
	
	public boolean shouldChildrenParallelRun(){
		return "parallel".equals(this.type);
	}
	
	public int getBuildNumber() {
		return buildNumber;
	}

	public void setBuildNumber(int buildNumber) {
		this.buildNumber = buildNumber;
	}
	
/*	
	public boolean shouldChildrenSerialRun(){
		return this.state.type == "serial";
	}
	
	public boolean shouldChildrenParallelRun(){
		return this.state.type == "parallel";
	}
*/	
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

	public String getJsonString(){
		return JSONObject.fromObject(this, JSON_CONFIG).toString();
	}
	
	public static class State {
		public boolean opened = true;
		public boolean disabled = false;
	    public boolean selected = true;
	    public boolean checked = false;
	    
	    //public String type; // it's weird that type in state doesnot change as ui changes
	    
	    public State(){}
	}
	
	public static TreeNode fromString(String jsonStr){
		JSONObject jsonObject = JSONObject.fromObject(jsonStr, TreeNode.JSON_CONFIG);
		return (TreeNode)JSONObject.toBean(jsonObject, TreeNode.JSON_CONFIG);
	}
}
