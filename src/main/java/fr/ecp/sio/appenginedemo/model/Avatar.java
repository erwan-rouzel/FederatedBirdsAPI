package fr.ecp.sio.appenginedemo.model;

/**
 * A model class for storing avatar representation. The only purpose of this class
 * as of now is to return a nicely json-looking reply through the API (instead of plain String).
 */
public class Avatar {
    public String servingUrl;

    public Avatar(String servingUrl) {
        this.servingUrl = servingUrl;
    }
}
