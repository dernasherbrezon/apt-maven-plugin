package org.apache.maven.plugin.gpg;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

public abstract class GpgMojo extends AbstractGpgMojo {
	
	@Override
	public AbstractGpgSigner newSigner(MavenProject project) throws MojoExecutionException, MojoFailureException {
		return super.newSigner(project);
	}

}
