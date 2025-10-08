
package com.github.hwc2243.servicebuilder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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
import com.github.hwc2243.servicebuilder.service.ServiceException;

@Mojo(name = "buildservice", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ServiceBuilderMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;

	@Parameter(defaultValue = "${project.resources}", required = true, readonly = true)
	private List<Resource> resources;

	@Parameter(property = "servicebuilder.buildType", required = false)
	private List<String> buildType = Arrays.asList("all");

	@Parameter(property = "servicebuilder.clean", required = false, defaultValue = "false")
	private boolean clean = false;

	@Parameter(property = "servicebuilder.jpaPackage", required = false)
	private String jpaPackage = "jakarta.persistence";
	
	@Parameter(property = "servicebuilder.outputDir", required = false)
	private String outputDir = null;
	
	@Parameter(property = "servicebuilder.replace", required = false, defaultValue = "false")
	private boolean replace = false;
	
	@Parameter(property = "servicebuilder.serviceFile", required = false)
	private String serviceFile;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		getLog().info("basedir: " + project.getBasedir());

		if (outputDir == null || outputDir.isEmpty()) {
			outputDir = project.getBasedir() + File.separator + "src" + File.separator + "main" + File.separator + "java";
		}
		if (serviceFile == null || serviceFile.isEmpty()) {
			// HWC right now we are passed the same directory twice so make it unique
			Set<String> serviceFiles = resources.stream()
					.filter(resource -> new File(resource.getDirectory(), "service.xml").exists())
					.map(resource -> resource.getDirectory() + File.separator + "service.xml")
					.collect(Collectors.toSet());

			if (serviceFiles.isEmpty()) {
				throw new MojoExecutionException("No service.xml file found");
			}
			if (serviceFiles.size() > 1) {
				serviceFiles.stream().forEach(directory -> {
					getLog().error("resource: = " + directory);
				});
				throw new MojoExecutionException("Multiple service.xml files found");
			}
			serviceFile = serviceFiles.iterator().next();
		}

		BuilderArgs args = BuilderArgs.builder()
				.buildType(buildType.stream()
						.map(type -> BuilderArgs.BuildType.valueOf(type.toUpperCase()))
						.collect(Collectors.toList()))
				.clean(clean)
				.jpaPackage(jpaPackage)
				.outputDir(outputDir)
				.replace(replace)
				.serviceFile(serviceFile).build();
		
		/*
		BuilderArgs args = new BuilderArgs();
		args.setBuildType(buildType.stream()
				.map(type -> BuilderArgs.BuildType.valueOf(type.toUpperCase()))
				.collect(Collectors.toList()));
		args.setOutputDir("src/main/java");
		args.setServiceFile(serviceFile);
		*/

		getLog().info("Build-service");

		try {
			BuilderService builderService = new BuilderServiceImpl();
			builderService.build(args);
		} catch (IOException ex) {
			throw new MojoExecutionException("Failed to build service", ex);
		} catch (SAXException ex) {
			throw new MojoExecutionException("Failed to parse service", ex);
		} catch (ServiceException ex) {
			throw new MojoExecutionException("Failed to build service", ex);
		}
	}
}
