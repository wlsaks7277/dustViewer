package com.example.dustviewer2;

/**
 * Created by SUEHYUN on 2019-05-29.
 */

import android.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class makeListThread extends Thread {
    private boolean stopped = false;
    private Document doc = null;
    private static String urlStr = "http://www.kma.go.kr/wid/queryDFSRSS.jsp?zone=1159068000";

    @Override
    public void run() {
        super.run();
        while(!stopped) {
            try {
                doc = getWeather();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopThread()
    {
        stopped = true;
    }

    public Document getDoc()
    {
        return doc;
    }


    public Document getWeather() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        URL urlForHttp = new URL(urlStr);

        InputStream instream = getInputStreamUsingHTTP(urlForHttp);
        Document document = builder.parse(instream);

        if (document != null) {
            NodeList list = document.getElementsByTagName("data");
            System.out.println("스레드실행");
            for (int i = 0; i < list.getLength(); i++) {
                System.out.println("=========================");
                for (int k = 0; k < list.item(i).getChildNodes().getLength(); k++) {
                    if (list.item(i).getChildNodes().item(k).getNodeType() == Node.ELEMENT_NODE) {
                        System.out.print(k + ":" + list.item(i).getChildNodes().item(k).getNodeName() + "====>");
                        System.out.println(list.item(i).getChildNodes().item(k).getTextContent());
                    }
                }
            }
            return document;
        }
        return null;
    }

    private InputStream getInputStreamUsingHTTP(URL urlForHttp) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) urlForHttp.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);;
        conn.setAllowUserInteraction(false);
        int resCode = conn.getResponseCode();
        Log.d("TAG ::","Response code : " + resCode);
        InputStream instream = conn.getInputStream();
        return  instream;
    }
}

