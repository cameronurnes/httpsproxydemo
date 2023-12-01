import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.security.KeyStore;

public class HttpClientWithCertProxyDemo {

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.out.println("Usage: java HttpClientWithCertProxyDemo <certPath> <certPassword> <proxyHost> <proxyPort> <targetUrl>");
            return;
        }

        String certPath = args[0];
        String certPassword = args[1];
        String proxyHost = args[2];
        int proxyPort = Integer.parseInt(args[3]);
        String targetUrl = args[4];

        System.out.println("Loading client certificate from: " + certPath);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream keyStoreInput = new FileInputStream(certPath)) {
            keyStore.load(keyStoreInput, certPassword.toCharArray());
        }

        System.out.println("Setting up SSL context with the client certificate");
        SSLContext sslContext = SSLContextBuilder.create()
                .loadKeyMaterial(keyStore, certPassword.toCharArray())
                .build();

        System.out.println("Configuring proxy: " + proxyHost + ":" + proxyPort);
        var sslsf = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(sslContext)
                .build();

        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslsf)
                .build();

        HttpHost proxy = new HttpHost("https", proxyHost, proxyPort);

        System.out.println("Creating and configuring HttpClient");
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build()) {

            HttpGet request = new HttpGet(targetUrl);
            RequestConfig config = RequestConfig.custom()
                    .setProxy(proxy)
                    .build();
            request.setConfig(config);

            System.out.println("Executing the request");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                System.out.println("Received response with status: " + response.getCode());
                // Printing the response body
                String responseBody = EntityUtils.toString(response.getEntity());
                System.out.println("Response Body: " + responseBody);

                // Ensure the response body is fully consumed
                EntityUtils.consume(response.getEntity());
            }
        }
    }
}

