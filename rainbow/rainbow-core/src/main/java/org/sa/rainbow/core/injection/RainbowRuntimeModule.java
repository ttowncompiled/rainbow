package org.sa.rainbow.core.injection;

import org.sa.rainbow.core.IRainbowEnvironment;
import org.sa.rainbow.core.Rainbow;
import org.sa.rainbow.core.RainbowEnvironmentDelegate;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Singleton;

public class RainbowRuntimeModule implements Module {

	@Override
	public void configure(Binder binder) {
		binder.requestStaticInjection(RainbowEnvironmentDelegate.class);
		binder.bind(IRainbowEnvironment.class).to(Rainbow.class).in(Singleton.class);
	}

}
