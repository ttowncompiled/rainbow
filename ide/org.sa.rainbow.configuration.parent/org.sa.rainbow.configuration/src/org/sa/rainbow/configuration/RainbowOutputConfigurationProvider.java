package org.sa.rainbow.configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.xtext.generator.IFileSystemAccess;
import org.eclipse.xtext.generator.IOutputConfigurationProvider;
import org.eclipse.xtext.generator.OutputConfiguration;

public class RainbowOutputConfigurationProvider implements IOutputConfigurationProvider {

	public static final String RAINBOW_TARGET_PROPERTIES_OUTPUT="rainbow-target-properties";
	
	
	@Override
	public Set<OutputConfiguration> getOutputConfigurations() {
		OutputConfiguration defaultOutput = new OutputConfiguration(IFileSystemAccess.DEFAULT_OUTPUT);
	    defaultOutput.setDescription("Output Folder");
	    defaultOutput.setOutputDirectory("./src-gen");
	    defaultOutput.setOverrideExistingResources(true);
	    defaultOutput.setCreateOutputDirectory(true);
	    defaultOutput.setCleanUpDerivedResources(true);
	    defaultOutput.setSetDerivedProperty(true);
	    
	    OutputConfiguration propertyOutput = new OutputConfiguration(RAINBOW_TARGET_PROPERTIES_OUTPUT);
	    propertyOutput.setDescription("Rainbow target property output folder");
	    propertyOutput.setOutputDirectory("./tgt-gen");
	    propertyOutput.setOverrideExistingResources(true);
	    propertyOutput.setCreateOutputDirectory(true);
	    propertyOutput.setCleanUpDerivedResources(false);
	    propertyOutput.setSetDerivedProperty(true);
	    
	    return new HashSet<>(Arrays.asList(new OutputConfiguration []{defaultOutput, propertyOutput}));
	    
	}

}