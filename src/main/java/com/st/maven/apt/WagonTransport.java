package com.st.maven.apt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authorization.AuthorizationException;

import ru.r2cloud.apt.IOCallback;
import ru.r2cloud.apt.ResourceDoesNotExistException;
import ru.r2cloud.apt.Transport;
import ru.r2cloud.apt.model.RemoteFile;

public class WagonTransport implements Transport {

	private final Wagon wagon;
	private final Log log;

	public WagonTransport(Wagon wagon, Log log) {
		this.wagon = wagon;
		this.log = log;
	}

	@Override
	public void save(String path, File file) throws IOException {
		try {
			wagon.put(file, path);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void save(String path, IOCallback callback) throws IOException {
		File tempFile = File.createTempFile("apt", String.valueOf(path.hashCode()));
		try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(tempFile))) {
			callback.save(fos);
		}
		save(path, tempFile);
		if (!tempFile.delete()) {
			log.warn("unable to delete file: " + tempFile.getAbsolutePath());
		}
	}

	@Override
	public void load(String path, IOCallback callback) throws IOException, ResourceDoesNotExistException {
		File tempFile = File.createTempFile("apt", String.valueOf(path.hashCode()));
		try {
			wagon.get(path, tempFile);
		} catch (TransferFailedException e) {
			throw new IOException(e);
		} catch (org.apache.maven.wagon.ResourceDoesNotExistException e) {
			throw new ResourceDoesNotExistException(e);
		} catch (AuthorizationException e) {
			throw new IOException(e);
		}
		try (InputStream is = new BufferedInputStream(new FileInputStream(tempFile))) {
			callback.load(is);
		}
		if (!tempFile.delete()) {
			log.warn("unable to delete file: " + tempFile.getAbsolutePath());
		}
	}

	@Override
	public void saveGzipped(String path, IOCallback callback) throws IOException {
		File tempFile = File.createTempFile("apt", String.valueOf(path.hashCode()));
		try (OutputStream fos = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(tempFile)))) {
			callback.save(fos);
		}
		save(path, tempFile);
		if (!tempFile.delete()) {
			log.warn("unable to delete file: " + tempFile.getAbsolutePath());
		}
	}

	@Override
	public void loadGzipped(String path, IOCallback callback) throws IOException, ResourceDoesNotExistException {
		File tempFile = File.createTempFile("apt", String.valueOf(path.hashCode()));
		try {
			wagon.get(path, tempFile);
		} catch (TransferFailedException e) {
			throw new IOException(e);
		} catch (org.apache.maven.wagon.ResourceDoesNotExistException e) {
			throw new ResourceDoesNotExistException(e);
		} catch (AuthorizationException e) {
			throw new IOException(e);
		}
		try (InputStream is = new BufferedInputStream(new GZIPInputStream(new FileInputStream(tempFile)))) {
			callback.load(is);
		}
		if (!tempFile.delete()) {
			log.warn("unable to delete file: " + tempFile.getAbsolutePath());
		}
	}

	@Override
	public void delete(String path) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<RemoteFile> listFiles(String path) {
		throw new UnsupportedOperationException();
	}

}
