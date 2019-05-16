package com.example.nfcatester;

import com.loopj.android.http.*;

import cz.msebera.android.httpclient.Header;

public class TagRestClient {
    private static final String BASE_URL = "https://tagserveriot.herokuapp.com/tag/";

    public static AsyncHttpClient client = new AsyncHttpClient();

    public static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
