package fr.ecp.sio.appenginedemo.data;

import fr.ecp.sio.appenginedemo.utils.FileUtils;
import org.apache.tika.io.IOUtils;
import org.apache.tika.mime.MimeTypeException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.GeneralSecurityException;

/**
 * This is a repository class for the images.
 * It could be backed by any kind of persistent storage engine.
 * Here we use the Cloud Storage from Google Cloud Platform, and we access it using the XML API.
 */
public class ImagesRepository {
    // We store here the constants which depend on the bucked name
    private final static String BUCKET_URL = "http://federatedbirds-storage.storage.googleapis.com/";
    private final static String SERVING_URL = "https://storage.googleapis.com/federatedbirds-storage/";

    public static String saveImage(InputStream inputStream, String contentType, String fileId) throws IOException, DataException, MimeTypeException, GeneralSecurityException {
        // Saves an image given its content type and ID

        // First we convert the InputStream containing image to a byte array
        // To ease this operation we use the commons-io apache library
        byte[] allBytes = org.apache.commons.io.IOUtils.toByteArray(inputStream);

        HttpURLConnection connection;
        String fileName;
        String fileExtension;

        // We retrieve the file extension from the contentype using our custom FileUtils helper class
        fileExtension = FileUtils.getFileExtFromContentType(contentType);
        fileName = fileId + fileExtension;

        // Here we contact the Cloud Storage XML API to actually store the file
        URL url = new URL(BUCKET_URL + fileName);
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestProperty("Content-Length", Integer.toString(allBytes.length));
        connection.setUseCaches(false);
        connection.setDoOutput(true);

        // Send request
        DataOutputStream wr = new DataOutputStream (connection.getOutputStream());
        wr.write(allBytes, 0, allBytes.length);
        wr.close();

        // We check if the API is replying by 200 for success
        // Otherwise we retrieve the error message and wrap it in a our DataException
        if(connection.getResponseCode() != 200) {
            String errorMessage = IOUtils.toString(connection.getErrorStream(), "UTF-8");
            throw new DataException(connection.getResponseCode(), "cannotSaveImage", errorMessage);
        }

        // We finally return the full URL to the stored image
        return SERVING_URL + fileName;
    }

    public static void deleteImage(String fileName) throws IOException, DataException {
        HttpURLConnection connection;
        URL url = new URL(BUCKET_URL + fileName);
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Content-Length", "0");
        connection.setUseCaches(false);
        connection.setDoOutput(true);

        // We check if the API is replying by 200 or 204 for success
        // Otherwise we retrieve the error message and wrap it in a our DataException
        if(connection.getResponseCode() >= 400) {
            String errorMessage = IOUtils.toString(connection.getErrorStream(), "UTF-8");
            throw new DataException(connection.getResponseCode(), "cannotDeleteImage", errorMessage);
        }
    }
}
