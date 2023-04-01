package io.github.learnjakartaee.spring;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import io.github.learnjakartaee.spring.config.ComponentsConfiguration;
import io.github.learnjakartaee.spring.config.WebMvcConfiguration;

public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

	@Override
	protected WebApplicationContext createRootApplicationContext() {
		WebApplicationContext context = super.createRootApplicationContext();
		String profiles = System.getenv("SPRING_PROFILES_ACTIVE");
		if (profiles == null) {
			profiles = System.getProperty("spring.profiles.active", "default");
		}
		((AnnotationConfigWebApplicationContext) context).getEnvironment().setActiveProfiles(profiles.split(","));
		return context;
	}

	@Override
	protected Class<?>[] getRootConfigClasses() {
		return new Class<?>[] {

				WebMvcConfiguration.class, ComponentsConfiguration.class

		};
	}

	@Override
	protected Class<?>[] getServletConfigClasses() {
		return null;
	}

	@Override
	protected String[] getServletMappings() {
		return new String[] { "/spring/*" };
	}
}