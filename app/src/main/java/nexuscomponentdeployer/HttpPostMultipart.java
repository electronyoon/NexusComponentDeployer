package nexuscomponentdeployer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class HttpPostMultipart {

    private final String boundary;
    private static final String LINE = "\r\n";
    private HttpURLConnection httpConn;
    private String charset;
    private OutputStream outputStream;
    private PrintWriter writer;
    private static final String AUTH = "admin:admin123";
    private static final String encodedAuthString = Base64.getEncoder().encodeToString(AUTH.getBytes());

    // https://stackoverflow.com/questions/11766878/sending-files-using-post-with-httpurlconnection
    /**
     * This constructor initializes a new HTTP POST request with content type
     * is set to multipart/form-data
     *
     * @param requestURL
     * @param charset
     * @param headers
     * @throws IOException
     */
    public HttpPostMultipart(String requestURL, String charset, Map<String, String> headers) throws IOException {
        this.charset = charset;
        boundary = UUID.randomUUID().toString();
        URL url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true);    // indicates POST method
        httpConn.setDoInput(true);
        httpConn.setRequestProperty("Authorization", "Basic " + encodedAuthString);
        httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (headers != null && headers.size() > 0) {
            Iterator<String> it = headers.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                String value = headers.get(key);
                httpConn.setRequestProperty(key, value);
            }
        }
        outputStream = httpConn.getOutputStream();
        outputStream.flush();
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);
        writer.flush();
    }

    public static int checkEndpoint(String endpointUrl) {
        try {
            URL url = new URL(endpointUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            // connection.setRequestProperty("User-Agent", "myJavaClient");
            connection.setRequestProperty("Authorization", "Basic " + encodedAuthString);
            connection.setRequestProperty("Content-Type", "multipart/form-data");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);

            OutputStream outputStream = connection.getOutputStream();
            outputStream.write("".getBytes());
            outputStream.flush();
            outputStream.close();
    
            return connection.getResponseCode();

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Adds a form field to the request
     *
     * @param name  field name
     * @param value field value
     */
    public void addFormField(String name, String value) {
        writer.append("--" + boundary).append(LINE);
        writer.append("Content-Disposition: form-data; name=\"" + name + "\"").append(LINE);
        writer.append("Content-Type: text/plain; charset=" + charset).append(LINE);
        writer.append(LINE);
        writer.append(value).append(LINE);
        writer.flush();
    }

    /**
     * Adds a upload file section to the request
     *
     * @param fieldName
     * @param uploadFile
     * @throws IOException
     */
    public void addFilePart(String fieldName, File uploadFile)
            throws IOException {
        String fileName = uploadFile.getName();
        writer.append("--" + boundary).append(LINE);
        writer.flush();
        writer.append("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"").append(LINE);
        writer.flush();
        writer.append("Content-Type: application/binary").append(LINE).append(LINE);
        writer.flush();

        FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
            outputStream.flush();
        }
        inputStream.close();
        writer.append(LINE);
        writer.flush();
        writer.append("--" + boundary + "--").append(LINE);
        writer.flush();
    }

    /**
     * Completes the request and receives response from the server.
     *
     * @return String as response in case the server returned
     * status OK, otherwise an exception is thrown.
     * @throws IOException
     */
    public int finish() throws IOException {
        
        // ByteArrayOutputStream rb = (ByteArrayOutputStream) outputStream;
        // System.out.println("Requesting body: " + rb.toString());
        writer.close();

        String response = "";
        // checks server's status code first
        int status = httpConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = httpConn.getInputStream().read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            response = result.toString(this.charset);
            System.out.println("Successfully uploaded: " + response);
            httpConn.disconnect();
        } else {
            System.err.println("Server returned non-OK status: " + status);
        }
        return status;
    }
}
