package org.apache.http.impl.client;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

public class CloseableHttpClient {

    public CloseableHttpResponse execute(HttpGet httpGet) throws IOException {
        return new CloseableHttpResponse();
    }

}
