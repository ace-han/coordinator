package org.jenkinsci.plugins.coordinator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

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
		
		// ROOT default is breaking serial node
		// EMPTY_ROOT.setType("serial");
		EMPTY_ROOT.state.breaking = true;
		EMPTY_ROOT.state.execPattern = "serial";
		
	}
	
	
	private String id;
	private String text;
	private String type;	// TODO target be deprecated in 1.5.0
	
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
	
	public int getBuildNumber() {
		return buildNumber;
	}

	public void setBuildNumber(int buildNumber) {
		this.buildNumber = buildNumber;
	}
	
	public TreeNode getParent() {
		return parent;
	}

	public void setParent(TreeNode parent) {
		this.parent = parent;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public boolean equals(Object obj) {
		// since we have id a more significant field
		if(! (obj instanceof TreeNode)){
			return false;
		}
		TreeNode node = (TreeNode) obj;
		return EqualsBuilder.reflectionEquals(this, node, REFLECTION_EXCLUDE_FIELDS);
	}

	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this, REFLECTION_EXCLUDE_FIELDS);
	}

	public String getJsonString(){
		return JSONObject.fromObject(this, JSON_CONFIG).toString();
	}

	
	public TreeNode clone(boolean deep){
		TreeNode clone = new TreeNode();
		clone.id = this.id;
		clone.text = this.text;
		clone.type = this.type;
		clone.state = this.state;
		clone.parent = this.parent;
		clone.buildNumber = this.buildNumber;
		if(deep){
			for(TreeNode c: this.children){
				clone.children.add(c.clone(deep));
			}
		} else {
			clone.children = this.children;
		}
		return clone;
	}

	/**
	 * Making this a calculated string based on {@code TreeNode.state.breaking, TreeNode.state.execPattern}
	 * @return A calculated String as {@code leaf, breaking-serial,breaking-parallel,non-breaking-serial,non-breaking-parallel} 
	 */
	public String getType() {
		if(this.isLeaf()){
			return "leaf";
		}
		String result;
		if(StringUtils.isEmpty(this.state.execPattern)){
			// default is breaking
			result = "breaking-"+type;
		} else {
			String breakingStype = this.state.breaking? "breaking-": "non-breaking-";
			result = breakingStype+this.state.execPattern;
		}
		return result;
	}

	/**
	 * For compatible sake
	 * Since it's a calculated field from now on, 
	 * please use {@link #State}{@code .breaking} and {@link #State}{@code .execPattern} instead
	 * @param type
	 */
	@Deprecated
	public void setType(String type) {
		this.type = type;
	}

	public boolean shouldChildrenSerialRun(){
		// TODO replace by {@code "serial".equals(TreeNode.state.execPattern) }
		if(StringUtils.isEmpty(this.state.execPattern)) {
			return this.getType().contains("serial");
		} else {
			return "serial".equals(this.state.execPattern);
		}
	}
	
	public boolean shouldChildrenParallelRun(){
		// TODO replace by {@code "parallel".equals(TreeNode.state.execPattern) }
		if(StringUtils.isEmpty(this.state.execPattern)) {
			return this.getType().contains("parallel");
		} else {
			return "parallel".equals(this.state.execPattern);
		}
	}
	
	public static class State {
		public boolean opened = true;
		public boolean disabled = false;
	    public boolean selected = true;
	    public boolean checked = false;
	    
	    // as I could not find out where to save this status
	    // it's mainly for actually doing atomic job to get the child job linked
	    // only used for server side, client side has its own mechanism to work well
	    public boolean undetermined = false;
	    
	    //public String type; // it's weird that type in state doesnot change as ui changes
	    // TreeNode.getType() is composed by below two options
	    public boolean breaking = true;
	    public String execPattern = "";
	    
	    public State(){}
	}
	
	public static TreeNode fromString(String jsonStr){
		JSONObject jsonObject = JSONObject.fromObject(jsonStr, TreeNode.JSON_CONFIG);
		return (TreeNode)JSONObject.toBean(jsonObject, TreeNode.JSON_CONFIG);
	}
}
