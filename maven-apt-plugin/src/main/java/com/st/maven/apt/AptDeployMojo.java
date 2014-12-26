package com.st.maven.apt;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @goal deploy
 * @phase deploy
 */
public class AptDeployMojo extends AbstractMojo {

	/**
	 * 
	 * @parameter expression="${maven.deploy.skip}"
	 * @readonly
	 * @default false
	 */
	private boolean skip;

	/**
	 * The maven project.
	 * 
	 * @parameter expression="${project}"
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * @component
	 */
	private PlexusContainer container;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			getLog().info("Skipping artifact deployment");
			return;
		}
		List<Artifact> attachedArtefacts = project.getAttachedArtifacts();
		List<Artifact> deb = new ArrayList<Artifact>();
		for (Artifact cur : attachedArtefacts) {
			if (cur.getType().equals("deb")) {
				deb.add(cur);
			}
		}
		if (deb.isEmpty()) {
			getLog().info("\"deb\" artifacts not found. skipping");
			return;
		}
		ArtifactRepository repository = project.getDistributionManagementArtifactRepository();
		if (repository == null) {
			throw new MojoExecutionException("no repository found for distribution");
		}
		Wagon w = null;
		Repository repositoryForWagon = new Repository(repository.getId(), repository.getUrl());
		AuthenticationInfo info = null;
		if (repository.getAuthentication() != null) {
			info = new AuthenticationInfo();
			info.setUserName(repository.getAuthentication().getUsername());
			info.setPassword(repository.getAuthentication().getPassword());
		}
		try {
			w = container.lookup(Wagon.class, repository.getProtocol());
		} catch (ComponentLookupException e) {
			throw new MojoExecutionException("unable to find wagon", e);
		}

		if (w == null) {
			throw new MojoExecutionException("unable to find wagon for: " + repository.getProtocol());
		}

		try {
			w.connect(repositoryForWagon, info);

			getLog().info("all was good");

		} catch (Exception e) {
			throw new MojoExecutionException("unable to process", e);
		} finally {
			try {
				w.disconnect();
			} catch (ConnectionException e) {
				getLog().error("unable to disconnect", e);
			}
		}

	}

}