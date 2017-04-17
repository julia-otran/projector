/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.other;

import br.com.projector.models.Music;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author guilherme
 */
public class Http {

    public interface Callback {

        public void onLoad(Music music);
    }

    public interface CancelSource {

        public void cancel();
    }

    public static CancelSource doRequest(final String url, final Callback content) {
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String result = executePost(url);
            }
        });

        t.start();

        return new CancelSource() {
            @Override
            public void cancel() {
                try {
                    t.interrupt();
                } catch (SecurityException ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    public static String executePost(String targetURL) {
        HttpURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            connection.setRequestProperty("Content-Length",
                    Integer.toString(0));
            connection.setRequestProperty("Content-Language", "pt-BR");

            connection.setUseCaches(false);
            connection.setDoOutput(false);

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line.replace("\r\n", "").replace("\n", ""));
                response.append("\r\n");
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
