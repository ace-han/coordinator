package org.jenkinsci.plugins.coordinator.model;

import hudson.Util;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.CauseAction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.apache.commons.jelly.JellyContext;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.WebApp;
import org.kohsuke.stapler.interceptor.RequirePOST;

public class CoordinatorBuild extends Build<CoordinatorProject, CoordinatorBuild> {
	
	private List<List<? extends Action>> oldActions = new ArrayList<List<? extends Action>>();

	private transient PerformExecutor performExecutor;
	
	private TreeNode originalExecutionPlan;
	
	private transient Map<String, Integer> tableRowIndexMap;
	
	/*package*/ TreeNode getOriginalExecutionPlan() {
		return originalExecutionPlan;
	}

	public void setOriginalExecutionPlan(TreeNode originalExecutionPlan) {
		this.originalExecutionPlan = originalExecutionPlan;
	}

	public CoordinatorBuild(CoordinatorProject project) throws IOException {
		super(project);
	}

	public CoordinatorBuild(CoordinatorProject project, File buildDir) throws IOException {
        super(project, buildDir);
    }

	public void addOldActions(List<? extends Action> oldActions) {
		this.oldActions.add(oldActions);
	}
	
	public List<List<? extends Action>> getOldActions(){
		return this.oldActions;
	}

	@Override
	public void run() {
		if (!this.oldActions.isEmpty()){
			foldCauseIntoOne();
		}
		super.run();
	}

	private void foldCauseIntoOne() {
		CauseAction curCauseAction = getAction(CauseAction.class);
		if(curCauseAction != null ){
			CauseAction oldCauseAction = Util.filter(this.oldActions.get(this.oldActions.size()-1), 
														CauseAction.class).get(0);
			
			curCauseAction.getCauses().addAll(oldCauseAction.getCauses());
			super.replaceAction(curCauseAction);
		}
	}

	@Override
	@RequirePOST
	public synchronized HttpResponse doStop() throws IOException,
			ServletException {
		if(performExecutor != null){
			performExecutor.shutdown();
		}
		return super.doStop();
	}

	public void setPerformExecutor(PerformExecutor performExecutor) {
		// just for a clean stop
		this.performExecutor = performExecutor;
	}
	
	public AtomicBuildInfo getExecutionPlanInfo(){
		if(this.tableRowIndexMap == null || this.tableRowIndexMap.isEmpty()){
			this.tableRowIndexMap = new HashMap<String, Integer>();
			prepareTableRowIndexMap(this.originalExecutionPlan);
		}
		return prepareAtomicBuildInfo(this.originalExecutionPlan, false);
	}
	
	protected void prepareTableRowIndexMap(TreeNode node) {
		this.tableRowIndexMap.put(node.getId(), tableRowIndexMap.size());
		for(TreeNode child: node.getChildren()){
			prepareTableRowIndexMap(child);
		}
	}

	public JSON doQueryActiveAtomicBuildStatus(StaplerRequest req) {
		Set<Entry<String, AbstractBuild<?, ?>>> entrySet = this.performExecutor.getActiveBuildMap().entrySet();
		Map<String, String> result = new HashMap<String, String>(entrySet.size()*2 + 3); 
		for(Map.Entry<String, AbstractBuild<?, ?>> entry: entrySet){
			AbstractBuild<?, ?> build = entry.getValue();
			AtomicBuildInfo abi = new AtomicBuildInfo();
			abi.build = build;
			abi.tableRowIndex = this.tableRowIndexMap.get(entry.getKey());
			result.put(entry.getKey(), getBuildInfoScriptAsString(abi));
		}
		return JSONObject.fromObject(result);
	}
	
	protected String getBuildInfoScriptAsString(AtomicBuildInfo abi) {
//		JellyContext context = new JellyContext();
//        // let Jelly see the whole classes
//        context.setClassLoader(WebApp.getCurrent().getClassLoader());
//        context.setVariable("it", abi);
//        context.runScript(uri, output)
		return "";
	}

	protected AtomicBuildInfo prepareAtomicBuildInfo(TreeNode node, boolean simpleMode){
		if(simpleMode && !node.getState().checked){
			return null; // save the time
		}
		AtomicBuildInfo abi = new AtomicBuildInfo();
		abi.treeNode = node;
		abi.tableRowIndex = this.tableRowIndexMap.get(node.getId());
		List<TreeNode> children = node.getChildren();
		if(children.isEmpty()){
			abi.build = retrieveTargetBuild(node.getText(), node.getBuildNumber());
		} 
		abi.children = new ArrayList<AtomicBuildInfo>(children.size());
		for(TreeNode child: children){
			AtomicBuildInfo abiChild = prepareAtomicBuildInfo(child, simpleMode);
			if(abiChild != null){
				abi.children.add(abiChild);
			}
		}
		return abi;
	}

	private AbstractBuild<?, ?> retrieveTargetBuild(String projectName,
			int buildNumber){
		AbstractProject<?, ?> project = (AbstractProject<?, ?>)Jenkins.getInstance().getItem(projectName);
		if(project == null){
			return null;
		}
		return (AbstractBuild<?, ?>)project.getBuildByNumber(buildNumber);
	}
	
	/**
	 * For display purpose
	 * @author Ace Han
	 *
	 */
	public static class AtomicBuildInfo {
		public TreeNode treeNode;
		public int tableRowIndex;
		public AbstractBuild<?, ?> build;
		
		public List<AtomicBuildInfo> children;
	}
}
