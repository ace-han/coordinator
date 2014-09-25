package org.jenkinsci.plugins.coordinator.model;

import hudson.cli.CLICommand;
import hudson.model.ParameterValue;
import hudson.model.ParameterDefinition;

import java.io.IOException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

/**
 * It will presented in every project's configure page without any simple mechanism as 
 * BuildStepDescriptor#isApplicable(Class<> jobType) does for the time being,  
 * I will make the configuration of TreeNode back in Builder
 * 
 * Only methods that are necessary for this ParameterDefinition remain, which means this
 * class is not a standard one for reference for what it was supposed to do
 * @author Ace Han
 *
 */
public class CoordinatorParameterDefinition extends ParameterDefinition {
	
	private static final long serialVersionUID = -6884384863181141230L;
	
	private TreeNode defaultValue;
	
	public CoordinatorParameterDefinition(TreeNode defaultValue) {
        super(CoordinatorParameterValue.PARAM_KEY, "");
        this.defaultValue = defaultValue;
    }
    
    public ParameterValue createValue(StaplerRequest req, JSONObject jo){
    	String jsonStr = jo.getString("value");
    	if(jsonStr == null){
    		return createValue(req);
    	} else {
    		return createValue(jsonStr);
    	}
    }

    public ParameterValue createValue(StaplerRequest req){
    	String[] values = req.getParameterValues(getName());
        if (values == null) {
            return getDefaultParameterValue();
        } else if (values.length != 1) {
            throw new IllegalArgumentException("Illegal number of parameter values for " + getName() + ": " + values.length);
        } else {
            return createValue(values[0]);
        }
    }

    public ParameterValue createValue(CLICommand command, String value) throws IOException, InterruptedException {
    	return createValue(value);
    }
    
    public CoordinatorParameterValue createValue(String jsonStr){
    	return new CoordinatorParameterValue(getName(), getDescription(), TreeNode.fromString(jsonStr));
    }

	@Override
	public ParameterValue getDefaultParameterValue() {
		return new CoordinatorParameterValue(getName(), getDescription(), defaultValue);
	}
    
    
	
}
