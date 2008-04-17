package org.drools.repository.remoteapi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;

import org.drools.repository.AssetItem;
import org.drools.repository.PackageItem;
import org.drools.repository.RulesRepository;
import org.drools.repository.RulesRepositoryException;
import org.drools.repository.remoteapi.Response.Binary;
import org.drools.repository.remoteapi.Response.Text;

/**
 * This provides a simple REST style remote friendly API.
 *
 * @author Michael Neale
 *
 */
public class RestAPI {

	private final RulesRepository repo;

	public RestAPI(RulesRepository repo) {
		this.repo = repo;
	}

	public Response get(String path) throws UnsupportedEncodingException {
		String[] bits = split(path);
		if (bits[0].equals("packages")) {
			String pkgName = bits[1];
			if (bits.length == 2) {
				return listPackage(pkgName);
			} else {
				String resourceFile = bits[2];
				return loadContent(pkgName, resourceFile);
			}
		} else {
			throw new IllegalArgumentException("Unable to deal with " + path);
		}

	}

	String[] split(String path) throws UnsupportedEncodingException {
		if (path.startsWith("/")) path = path.substring(1);
		String[] bits = path.split("/");
		for (int i = 0; i < bits.length; i++) {
			bits[i] = URLDecoder.decode(bits[i], "UTF-8");
		}
		return bits;
	}

	private Response loadContent(String pkgName, String resourceFile) throws UnsupportedEncodingException {
		PackageItem pkg = repo.loadPackage(pkgName);
		if (resourceFile.equals(".package")) {
			Text r = new Response.Text();
			r.lastModified = pkg.getLastModified();
			r.data = pkg.getHeader();
			return r;
		} else {
			String assetName = resourceFile.split("\\.")[0];

			AssetItem asset = pkg.loadAsset(assetName);
			if (asset.isBinary()) {
				Binary r = new Response.Binary();
				r.lastModified = asset.getLastModified();
				r.stream = asset.getBinaryContentAttachment();
				return r;
			} else {
				Text r = new Response.Text();
				r.lastModified = pkg.getLastModified();
				r.data = asset.getContent();
				return r;
			}

		}

	}

	private Response listPackage(String pkgName) throws UnsupportedEncodingException {
		PackageItem pkg = repo.loadPackage(URLDecoder.decode(pkgName, "UTF-8"));
		StringBuilder sb = new StringBuilder();
		Iterator<AssetItem> it = pkg.getAssets();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

		while (it.hasNext()) {
			AssetItem a = it.next();
			sb.append(a.getName() + "." + a.getFormat() + "=" + sdf.format(a.getLastModified().getTime()));
			sb.append('\n');
		}

		Text r = new Response.Text();
		r.lastModified = pkg.getLastModified();
		r.data = sb.toString();
		return r;
	}

	/** post is for new content.
	 * @throws IOException
	 * @throws RulesRepositoryException */
	public void post(String path, InputStream in, boolean binary, String comment) throws RulesRepositoryException, IOException {
		String[] bits = split(path);
		if (bits[0].equals("packages")) {
			String fileName = bits[2];
			String[] a = fileName.split("\\.");
			if (a[1].equals("package")) {
				//new package
				PackageItem pkg = repo.createPackage(bits[1], "<added remotely>");
				pkg.updateCheckinComment(comment);
				pkg.updateHeader(readContent(in));
				repo.save();
			} else {
				//new asset
				PackageItem pkg = repo.loadPackage(bits[1]);
				AssetItem asset = pkg.addAsset(a[0], "<added remotely>");
				asset.updateFormat(a[1]);
				if (binary) {
					asset.updateBinaryContentAttachment(in);
				} else {
					asset.updateContent(readContent(in));
				}
				asset.checkin(comment);
			}
		} else {
			throw new IllegalArgumentException("Unknown rest path for post.");
		}
	}

	private String readContent(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		final byte[] buf = new byte[1024];
        int len = 0;
        while ( (len = in.read( buf )) >= 0 ) {
            out.write( buf,
                       0,
                       len );
        }
        return new String(out.toByteArray());
    }

	/**
	 * Put is for updating content. It will cause a new revision to be created.
	 * need to also cope with the .package thing
	 * @throws IOException
	 */
	public void put(String path, Calendar lastModified, InputStream in, String comment) throws IOException {
		String[] bits = split(path);
		if (bits[0].equals("packages")) {
			String fileName = bits[2];
			String[] a = fileName.split("\\.");
			PackageItem pkg = repo.loadPackage(bits[1]);
			if (a[1].equals("package")) {
				//updating package header
				if (pkg.getLastModified().after(lastModified)) {
					throw new RulesRepositoryException("The package was modified by: " + pkg.getLastContributor() + ", unable to write changes.");
				}
				pkg.updateHeader(readContent(in));
				pkg.checkin(comment);
				repo.save();
			} else {
				AssetItem as = pkg.loadAsset(a[0]);
				if (as.getLastModified().after(lastModified)) {
					throw new RulesRepositoryException("The asset was modified by: " + as.getLastContributor() + ", unable to write changes.");
				}
				if (as.isBinary()) {
					as.updateBinaryContentAttachment(in);
				} else {
					as.updateContent(readContent(in));
				}
				as.checkin(comment);
			}

		} else {
			throw new IllegalArgumentException("Unknown rest path for put");
		}

	}

	/**
	 * Should be pretty obvious what this is for.
	 * @throws UnsupportedEncodingException
	 */
	public void delete(String path) throws UnsupportedEncodingException {
		String[] bits = split(path);
		if (bits[0].equals("packages")) {
			String fileName = bits[2].split("\\.")[0];
			AssetItem asset = repo.loadPackage(bits[1]).loadAsset(fileName);
			asset.archiveItem(true);
			asset.checkin("<removed remotely>");
		}
		else {
			throw new IllegalArgumentException("Unknown rest path for put");
		}

	}



}