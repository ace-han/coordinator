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
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSON;
import net.sf.json.JSONArray;

import org.apache.commons.lang.time.FastDateFormat;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

public class CoordinatorBuild extends Build<CoordinatorProject, CoordinatorBuild> {
	
	private List<List<? extends Action>> oldActions = new ArrayList<List<? extends Action>>();

	private transient PerformExecutor performExecutor;
	
	private TreeNode originalExecutionPlan;
	
	public static final FastDateFormat DATETIME_FORMATTER = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss Z");

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
		return prepareAtomicBuildInfo(new ArrayList<AtomicBuildInfo>(), this.originalExecutionPlan, false);
	}
	
	public JSON doQueryActiveAtomicBuildStatus() {
		Set<Entry<String, AbstractBuild<?, ?>>> entrySet = this.performExecutor.getActiveBuildMap().entrySet();
		ArrayList<AtomicBuildInfo> result = new ArrayList<AtomicBuildInfo>(entrySet.size()); 
		for(Map.Entry<String, AbstractBuild<?, ?>> entry: entrySet){
			AbstractBuild<?, ?> build = entry.getValue();
			TreeNode node = new TreeNode();
			node.setId(entry.getKey());
			node.setText(build.getParent().getName());
			node.setBuildNumber(build.getNumber());
			AtomicBuildInfo abi = prepareBuildInfo(node);
			result.add(abi);
		}
		return JSONArray.fromObject(result, TreeNode.JSON_CONFIG);
	}
	
	protected AtomicBuildInfo prepareAtomicBuildInfo(List<AtomicBuildInfo> list, TreeNode node, boolean simpleMode){
		if(simpleMode && !node.getState().checked){
			return null; // save the time
		}
		AtomicBuildInfo abi;
		List<TreeNode> children = node.getChildren();
		if(children.isEmpty()){
			abi = prepareBuildInfo(node);
		} else {
			abi = new AtomicBuildInfo();
		}
		abi.treeNode = node;
		list.add(abi);
		abi.tableRowIndex = list.size() - 1;
		abi.children = new ArrayList<AtomicBuildInfo>(children.size());
		for(TreeNode child: children){
			AtomicBuildInfo abiChild = prepareAtomicBuildInfo(list, child, simpleMode);
			if(abiChild != null){
				abi.children.add(abiChild);
			}
		}
		return abi;
	}

	protected AtomicBuildInfo prepareBuildInfo(TreeNode node) {
		AtomicBuildInfo abi = new AtomicBuildInfo();
		AbstractBuild<?, ?> targetBuild = retrieveTargetBuild(node.getText(), node.getBuildNumber());
		if(targetBuild != null){
			abi.launchTime = DATETIME_FORMATTER.format(targetBuild.getTime());
			abi.duration = targetBuild.getDurationString();
			abi.statusHtml = prepareStatusHtml(targetBuild);
		} else {
			abi.launchTime = abi.duration = "N/A";
			abi.statusHtml = "<span title=\"This build might be already discarded\" style=\"color:red;\">Unretrievable</span>";
		}
		return abi;
	}
	
	
	protected String prepareStatusHtml(AbstractBuild<?, ?> targetBuild) {
		return "&nbsp;";
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
		public String launchTime;
		public String duration;
		public String statusHtml;

		public TreeNode treeNode;
		public int tableRowIndex;
		
		public List<AtomicBuildInfo> children;
	}
}
