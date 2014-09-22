package com.hmm;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class QueryExecutor {

    public String executeGet(String uri) {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(uri);

        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            @Override
            public String handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
                return EntityUtils.toString(httpResponse.getEntity());
            }
        };
        try {
            String response = httpClient.execute(httpGet, responseHandler);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }


//        JsonReader reader = Json.createReader(new StringReader(response));



//        System.out.println(reader.readObject().getJsonObject("item").getJsonObject("videos").getJsonObject("main").getJsonArray("video_content").getJsonObject(1));
        return null;
    }
}
