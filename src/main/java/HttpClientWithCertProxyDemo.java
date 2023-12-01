import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
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
        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        sslContextBuilder.loadKeyMaterial(keyStore, certPassword.toCharArray());
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslContextBuilder.build());

        System.out.println("Configuring proxy: " + proxyHost + ":" + proxyPort);
        HttpHost proxy = new HttpHost(proxyHost, proxyPort, "https");

        System.out.println("Creating and configuring HttpClient");
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .build();

        System.out.println("Creating HTTP GET request to: " + targetUrl);
        HttpGet request = new HttpGet(targetUrl);
        RequestConfig config = RequestConfig.custom()
                .setProxy(proxy)
                .build();
        request.setConfig(config);

        System.out.println("Executing the request");
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            System.out.println("Received response with status: " + response.getStatusLine());
        }
    }
}
