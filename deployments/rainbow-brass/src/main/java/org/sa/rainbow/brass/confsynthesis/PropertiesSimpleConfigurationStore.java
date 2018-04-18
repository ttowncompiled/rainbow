package org.sa.rainbow.brass.confsynthesis;

import java.util.Properties;


public class PropertiesSimpleConfigurationStore {

	public static final String CONFIGURATIONS_SOURCE_PROPKEY = "configsource";
	public static final String CONFIGURATIONS_SOURCE_FILENAME = "config_list.json";
    public static final String CONFIGURATIONS_SOURCE_PATH = "/Users/jcamara/Dropbox/Documents/Work/projects/rainbow/deployments/rainbow-brass/prismtmp/p2cp1/";
    
    public static final Properties DEFAULT = new Properties ();
    static {
        DEFAULT.setProperty (PropertiesSimpleConfigurationStore.CONFIGURATIONS_SOURCE_PROPKEY, CONFIGURATIONS_SOURCE_PATH+CONFIGURATIONS_SOURCE_FILENAME);
    }
}
