package com.st.maven.apt;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
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

	/**
	 * 
	 * @parameter expression="${maven.apt.file}"
	 * @readonly
	 */
	private String file;

	/**
	 * 
	 * @parameter
	 * @readonly
	 * @required
	 */
	private String codename;

	/**
	 * 
	 * @parameter
	 * @readonly
	 * @required
	 */
	private String component;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			getLog().info("Skipping artifact deployment");
			return;
		}
		List<File> deb = getDebFiles();
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

			File packagesFile = File.createTempFile("apt", "packagesOld");
			String packagesBaseFilename = component + "/binary-amd64/Packages.gz";
			String packagesFilename = "dists/" + codename + "/" + packagesBaseFilename;

			Packages packages = new Packages();
			InputStream fis = null;
			try {
				w.get(packagesFilename, packagesFile);
				fis = new GZIPInputStream(new FileInputStream(packagesFile));
				packages.load(fis);
			} catch (ResourceDoesNotExistException e) {
				getLog().info("Packages.gz do not exist. creating...");
			} catch (Exception e) {
				throw new MojoExecutionException("unable to load Packages.gz from: " + packagesFile.getAbsolutePath(), e);
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
						getLog().error("unable to close cursor", e);
					}
				}
			}

			for (File f : deb) {
				String control = readControl(f);
				if (control == null) {
					throw new MojoExecutionException("invalid .deb format. Missing control file: " + f.getAbsolutePath());
				}
				ControlFile controlFile = new ControlFile();
				controlFile.load(control);
				String relativeFilename = "pool/" + component + "/" + controlFile.getPackageName().charAt(0) + "/" + controlFile.getPackageName() + "/" + controlFile.getPackageName() + "_" + controlFile.getVersion() + "_" + controlFile.getArch() + ".deb";
				try {
					FileInfo fileInfo = getFileInfo(f);
					controlFile.append("Filename: " + relativeFilename);
					controlFile.append("Size: " + fileInfo.getSize());
					controlFile.append("MD5sum: " + fileInfo.getMd5());
					controlFile.append("SHA1: " + fileInfo.getSha1());
					controlFile.append("SHA256: " + fileInfo.getSha256());
				} catch (Exception e) {
					throw new MojoExecutionException("unable to calculate checksum for: " + f.getAbsolutePath(), e);
				}
				packages.add(controlFile);
				getLog().info("uploading: " + f.getAbsolutePath());
				w.put(f, relativeFilename);
			}

			OutputStream fos = null;
			try {
				fos = new GZIPOutputStream(new FileOutputStream(packagesFile));
				packages.save(fos);
			} catch (Exception e) {
				throw new MojoExecutionException("unable to write packages", e);
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e) {
						getLog().error("unable to close cursor", e);
					}
				}
			}

			getLog().info("uploading: Packages.gz");
			w.put(packagesFile, packagesFilename);

			File releaseFile = File.createTempFile("apt", "releaseFile");
			String releaseFilename = getReleasePath();

			Release release = loadRelease(w, releaseFile);

			try {
				FileInfo packagesFileInfo = getFileInfo(packagesFile);
				packagesFileInfo.setFilename(packagesBaseFilename);
				release.getFiles().add(packagesFileInfo);
			} catch (Exception e) {
				throw new MojoExecutionException("unable to calculate checksum for: " + packagesFile.getAbsolutePath(), e);
			}

			try {
				fos = new FileOutputStream(releaseFile);
				release.save(fos);
			} catch (Exception e) {
				throw new MojoExecutionException("unable to write releases", e);
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e) {
						getLog().error("unable to close cursor", e);
					}
				}
			}

			getLog().info("uploading: Release");
			w.put(releaseFile, releaseFilename);

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

	private Release loadRelease(Wagon w, File releaseFile) throws MojoExecutionException {
		InputStream fis = null;
		Release release = new Release();
		try {
			w.get(getReleasePath(), releaseFile);
			fis = new FileInputStream(releaseFile);
			release.load(fis);
		} catch (ResourceDoesNotExistException e) {
			getLog().info("Release do not exist. creating...");
			release.setArchitectures("amd64");
			release.setCodename(codename);
			release.setComponents(component);
			release.setLabel(codename);
			release.setOrigin(codename);
		} catch (Exception e) {
			throw new MojoExecutionException("unable to read Release from: " + releaseFile.getAbsolutePath(), e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					getLog().error("unable to close cursor", e);
				}
			}
		}
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		release.setDate(sdf.format(new Date()));
		return release;
	}

	private String getReleasePath() {
		String releaseFilename = "dists/" + codename + "/Release";
		return releaseFilename;
	}

	private List<File> getDebFiles() throws MojoExecutionException {
		List<Artifact> attachedArtefacts = project.getAttachedArtifacts();
		List<File> deb = new ArrayList<File>();
		for (Artifact cur : attachedArtefacts) {
			if (cur.getType().equals("deb")) {
				deb.add(cur.getFile());
			}
		}
		if (file != null && file.trim().length() != 0) {
			File f = new File(file);
			if (!f.exists()) {
				throw new MojoExecutionException("specified file not found: " + f.getAbsolutePath());
			}
			deb.add(f);
		}
		return deb;
	}

	private static FileInfo getFileInfo(File f) throws Exception {
		FileInfo result = new FileInfo();
		result.setSize(String.valueOf(f.length()));
		BufferedInputStream bis = null;
		try {
			bis = new BufferedInputStream(new FileInputStream(f));
			MessageDigest md5Alg = MessageDigest.getInstance("MD5");
			md5Alg.reset();
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			sha1.reset();
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			sha256.reset();
			byte[] buf = new byte[2048];
			int curByte = -1;
			while ((curByte = bis.read(buf)) != -1) {
				md5Alg.update(buf, 0, curByte);
				sha1.update(buf, 0, curByte);
				sha256.update(buf, 0, curByte);
			}
			result.setMd5(new String(Hex.encodeHex(md5Alg.digest())));
			result.setSha1(new String(Hex.encodeHex(sha1.digest())));
			result.setSha256(new String(Hex.encodeHex(sha256.digest())));
		} finally {
			if (bis != null) {
				bis.close();
			}
		}
		return result;
	}

	private String readControl(File deb) throws MojoExecutionException {
		ArArchiveEntry entry;
		TarArchiveEntry control_entry;
		ArchiveInputStream debStream = null;
		try {
			debStream = new ArchiveStreamFactory().createArchiveInputStream("ar", new FileInputStream(deb));
			while ((entry = (ArArchiveEntry) debStream.getNextEntry()) != null) {
				if (entry.getName().equals("control.tar.gz")) {
					GZIPInputStream gzipInputStream = new GZIPInputStream(debStream);
					ArchiveInputStream control_tgz = new ArchiveStreamFactory().createArchiveInputStream("tar", gzipInputStream);
					while ((control_entry = (TarArchiveEntry) control_tgz.getNextEntry()) != null) {
						getLog().debug("control entry: " + control_entry.getName());
						if (control_entry.getName().equals("./control")) {
							ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
							IOUtils.copy(control_tgz, outputStream);
							String content_string = outputStream.toString("UTF-8");
							outputStream.close();
							return content_string;
						}
					}
				}
			}
			return null;
		} catch (Exception e) {
			throw new MojoExecutionException("invalid .deb. unable to find control at: " + deb.getAbsolutePath(), e);
		} finally {
			if (debStream != null) {
				try {
					debStream.close();
				} catch (IOException e) {
					getLog().error("unable to close .deb", e);
				}
			}
		}
	}

}