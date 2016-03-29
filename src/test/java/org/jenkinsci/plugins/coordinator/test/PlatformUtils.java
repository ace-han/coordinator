package org.jenkinsci.plugins.coordinator.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import hudson.tasks.BatchFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Shell;

public class PlatformUtils {
	
	public static String getOsFamily(){
		return System.getProperty("os.name").toLowerCase();
	}

	public static boolean isWindows(){
		String osFamily = getOsFamily();
		return osFamily.indexOf("win")>=0;
	}
	
	public static Class<? extends CommandInterpreter> getCommandShellClass(){
		return isWindows()? BatchFile.class: Shell.class;
	}
	
	public static CommandInterpreter getCommandShell(String command) throws NoSuchMethodException, 
																	SecurityException, InstantiationException, 
																	IllegalAccessException, IllegalArgumentException, 
																	InvocationTargetException {
		Class<? extends CommandInterpreter> commandShellClass = getCommandShellClass();
		Constructor<? extends CommandInterpreter> constructor = commandShellClass.getConstructor(String.class);
		CommandInterpreter newInstance = constructor.newInstance(command);;
		return newInstance;
	}
	
	public static CommandInterpreter getCommandShellQuietly(String command) {
		try {
			return getCommandShell(command);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}