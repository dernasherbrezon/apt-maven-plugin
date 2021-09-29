package com.st.maven.apt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import ru.r2cloud.apt.AptRepository;
import ru.r2cloud.apt.AptRepositoryImpl;
import ru.r2cloud.apt.GpgSigner;
import ru.r2cloud.apt.GpgSignerImpl;
import ru.r2cloud.apt.model.DebFile;
import ru.r2cloud.apt.model.SignConfiguration;

@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = false)
public class AptDeployMojo extends AbstractMojo {

	@Parameter(defaultValue = "${maven.deploy.skip}", readonly = true)
	private boolean skip;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Component
	private PlexusContainer container;

	@Parameter(defaultValue = "${maven.apt.file}", readonly = true)
	private String file;

	@Parameter(defaultValue = "${maven.apt.files}", readonly = true)
	private String files;

	@Parameter(readonly = true, required = true)
	private String codename;

	@Parameter(readonly = true, required = true)
	private String component;

	@Parameter(property = "gpg.executable")
	private String executable;

	/**
	 * The "name" of the key to sign with. Passed to gpg as
	 * <code>--local-user</code>.
	 */
	@Parameter(property = "gpg.keyname")
	private String keyname;

	/**
	 * The passphrase to use when signing. If not given, look up the value under
	 * Maven settings using server id at 'passphraseServerKey' configuration.
	 **/
	@Parameter(property = "gpg.passphrase")
	private String passphrase;

	/**
	 * Server id to lookup the passphrase under Maven settings.
	 * 
	 * @since 1.6
	 */
	@Parameter(property = "gpg.passphraseServerId", defaultValue = "gpg.passphrase")
	private String passphraseServerId;

	/**
	 * Current user system settings for use in Maven.
	 *
	 * @since 1.6
	 */
	@Parameter(defaultValue = "${settings}", readonly = true)
	private Settings settings;

	/**
	 * Maven Security Dispatcher
	 *
	 * @since 1.6
	 */
	@Component(hint = "mng-4384")
	private SecDispatcher securityDispatcher;

	@Parameter(property = "gpg.sign", readonly = true)
	private boolean sign;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			getLog().info("Skipping artifact deployment");
			return;
		}
		List<DebFile> debs;
		try {
			debs = getDebFiles();
		} catch (Exception e1) {
			throw new MojoExecutionException("unable to read .deb files", e1);
		}
		if (debs.isEmpty()) {
			getLog().info("\"deb\" artifacts not found. skipping");
			return;
		}

		GpgSigner signer = null;
		if (sign) {
			SignConfiguration signConfig = new SignConfiguration();
			if (executable != null) {
				signConfig.setGpgCommand(executable);
			} else {
				String command = "gpg";
				if (System.getProperty("os.name").toLowerCase(Locale.US).contains("windows")) {
					command += ".exe";
				}
				signConfig.setGpgCommand(command);
			}
			signConfig.setKeyname(keyname);
			signConfig.setPassphrase(codename);
			loadGpgPassphrase();
			signConfig.setPassphrase(passphrase);

			signer = new GpgSignerImpl(signConfig);
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
			AptRepository aptRepo = new AptRepositoryImpl(codename, component, signer, new WagonTransport(w));
			aptRepo.saveFiles(debs);
		} catch (Exception e) {
			throw new MojoExecutionException("unable to save", e);
		}

	}

	private List<DebFile> getDebFiles() throws IOException, ArchiveException {
		List<Artifact> attachedArtefacts = project.getAttachedArtifacts();
		List<DebFile> deb = new ArrayList<>();
		for (Artifact cur : attachedArtefacts) {
			if (cur.getType().equals("deb")) {
				deb.add(new DebFile(cur.getFile()));
			}
		}
		if (file != null && file.trim().length() != 0) {
			File f = new File(file);
			if (!f.exists()) {
				throw new IOException("specified file not found: " + f.getAbsolutePath());
			}
			deb.add(new DebFile(f));
		}
		if (files != null) {
			DirectoryScanner scanner = new DirectoryScanner();
			File basedir = new File(".");
			scanner.setBasedir(basedir);
			scanner.setIncludes(new String[] { files });
			scanner.setCaseSensitive(true);
			scanner.scan();
			for (String curFile : scanner.getIncludedFiles()) {
				File f = new File(basedir, curFile);
				if (!f.isFile()) {
					continue;
				}
				deb.add(new DebFile(f));
			}
		}
		return deb;
	}

	private void loadGpgPassphrase() throws MojoFailureException {
		if (StringUtils.isEmpty(this.passphrase)) {
			Server server = this.settings.getServer(passphraseServerId);

			if (server != null) {
				if (server.getPassphrase() != null) {
					try {
						this.passphrase = securityDispatcher.decrypt(server.getPassphrase());
					} catch (SecDispatcherException e) {
						throw new MojoFailureException("Unable to decrypt gpg passphrase", e);
					}
				}
			}
		}
	}
}