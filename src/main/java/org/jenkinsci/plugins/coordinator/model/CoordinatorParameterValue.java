package org.jenkinsci.plugins.coordinator.model;

import hudson.EnvVars;
import hudson.model.ParameterValue;
import hudson.model.AbstractBuild;
import hudson.util.VariableResolver;

import java.util.Locale;

public class CoordinatorParameterValue extends ParameterValue {

	private static final long serialVersionUID = -1278826177124697893L;
	
	public static final String PARAM_KEY = "executionPlan";

	private TreeNode value;
	
	public TreeNode getValue() {
		return value;
	}

	public void setValue(TreeNode value) {
		this.value = value;
	}

	public CoordinatorParameterValue(String name, String description, TreeNode value) {
		super(name, description);
		this.value = value;
	}
	
    /**
     * Exposes the name/value as an environment variable.
     */
    @Override
    public void buildEnvVars(AbstractBuild<?,?> build, EnvVars env) {
    	String jsonStr = value.getJsonString();
        env.put(name, jsonStr);
        env.put(name.toUpperCase(Locale.ENGLISH), jsonStr); // backward compatibility pre 1.345
    }

    @Override
    public VariableResolver<String> createVariableResolver(AbstractBuild<?, ?> build) {
        return new VariableResolver<String>() {
            public String resolve(String name) {
                return CoordinatorParameterValue.this.name.equals(name) ? value.getJsonString(): null;
            }
        };
    }

}
