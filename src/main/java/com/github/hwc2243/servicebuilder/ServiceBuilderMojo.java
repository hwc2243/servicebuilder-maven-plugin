
package com.github.hwc2243.servicebuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.xml.sax.SAXException;

import com.github.hwc2243.servicebuilder.service.BuilderArgs;
import com.github.hwc2243.servicebuilder.service.BuilderService;
import com.github.hwc2243.servicebuilder.service.BuilderServiceImpl;

@Mojo(name = "buildservice", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ServiceBuilderMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;
	
	@Parameter( defaultValue = "${project.resources}", required = true, readonly = true )
	private List<Resource> resources;
	
	@Override
	public void execute()
		throws MojoExecutionException, MojoFailureException {

		getLog().info("basedir: " + project.getBasedir());
		
		// HWC right now we are passed the same directory twice so make it unique 
		Set<String> serviceFiles = resources
						.stream()
						.filter(resource -> new File(resource.getDirectory(), "service.xml").exists())
						.map(resource -> resource.getDirectory() + File.separator + "service.xml")
						.collect(Collectors.toSet());
		
		if (serviceFiles.isEmpty()) {
			throw new MojoExecutionException("No service.xml file found");
		}
		if (serviceFiles.size() > 1) {
			serviceFiles.stream().forEach(directory -> { getLog().error("resource: = " + directory); });
			throw new MojoExecutionException("Multiple service.xml files found");
		}
		
		BuilderArgs args = new BuilderArgs();
		args.setServiceFile(serviceFiles.iterator().next());
		args.setOutputDir("src/main/java");
		
		getLog().info("Build-service");
		
		try
		{
			BuilderService builderService = new BuilderServiceImpl();
			builderService.build(args);
		}
		catch (IOException ex)
		{
			throw new MojoExecutionException("Failed to build service", ex);
		}
		catch (SAXException ex)
		{
			throw new MojoExecutionException("Failed to parse service", ex);
		}
	}
}
