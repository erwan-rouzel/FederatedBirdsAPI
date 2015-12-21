package fr.ecp.sio.appenginedemo.utils;

import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

/**
 * Some utils related to file operations
 */
public class FileUtils {
    public static String getFileExtFromContentType(String contentType) throws MimeTypeException {
        // To easily get the file extension from mime type we use here the Apache Tika library
        MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
        MimeType mimeType = allTypes.forName(contentType);
        return mimeType.getExtension();
    }
}
