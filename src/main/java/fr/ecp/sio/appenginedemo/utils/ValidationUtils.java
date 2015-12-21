package fr.ecp.sio.appenginedemo.utils;

import com.google.api.client.http.HttpRequest;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import org.apache.commons.validator.routines.EmailValidator;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URLConnection;
import java.net.URL;

/**
 * Some utils to validate user inputs.
 * Never relies on the client applications, always re-validate.
 */
public class ValidationUtils {

    // Regex patterns for the login and the password
    private static final String LOGIN_PATTERN = "^[A-Za-z0-9_-]{4,12}$";
    private static final String PASSWORD_PATTERN = "^\\w{4,12}$";

    public static boolean validateLogin(String login) {
        return login != null && login.matches(LOGIN_PATTERN);
    }

    public static boolean validatePassword(String password) {
        return password != null && password.matches(PASSWORD_PATTERN);
    }

    public static boolean validateEmail(String email) {
        // Here we use a library from Apache Commons to do the validation
        return EmailValidator.getInstance(false).isValid(email);
    }

    // Just doing regex validation would not be enough here.
    // We have to check that URL actually exists and represents an image.
    public static boolean validateImageUrl(String imageUrl){
        if(imageUrl == null) return false;

        try {
            URLFetchService fetcher = URLFetchServiceFactory.getURLFetchService();
            URL url = new URL(imageUrl);
            HTTPResponse response = fetcher.fetch(url);

            String result = "";

            for(HTTPHeader header : response.getHeaders()) {
                String headerName = header.getName();
                String headerValue = header.getValue();

                if(headerName.equals("Content-Type")) {
                    return headerValue.startsWith("image/");
                }
            }

            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
