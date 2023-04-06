package nexuscomponentdeployer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class AppForDeployer {

    private static final String API_URL = "http://localhost:8081/nexus/service/rest/v1/components?repository=maven-releases";
    private static final String RESOURCE_PACKAGE = "target/dependency/";
    private List<File> childJars = new ArrayList<>();

    public static void main(String[] args) throws IOException, URISyntaxException {
        AppForDeployer ad = new AppForDeployer();
        ad.findJars();
        ad.uploadJar();
    }

    public void findJars() throws IOException, URISyntaxException {
        System.out.println("======================================== Starting jar copy ========================================");

        new AppForDeployer().getThisJar();

        Predicate<JarEntry> pred = (j) -> !j.isDirectory() && j.getName().startsWith(RESOURCE_PACKAGE);
        try (JarFile jar = new AppForDeployer().getThisJar()) {
            List<JarEntry> resources = getEntriesUnderPath(jar, pred);
            for (JarEntry entry : resources) {
                File tempFile = File.createTempFile(entry.getName(), null);
                // File tempFile = new File(entry.getName());
                try (InputStream is = jar.getInputStream(entry);
                    OutputStream os = new FileOutputStream(tempFile))
                {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) != -1) {
                        os.write(buffer, 0, length);
                    }
                }
                childJars.add(tempFile);
                System.out.println("entry added: " + entry.getName());
            }
        }
        childJars.forEach(f -> System.out.println("verifying childjars... filename: " + f.getName() + ", filesize: " + f.length()));
        System.out.println("======================================== Exiting jar copy ========================================");

        // // Indexing IDE resource
        // URL url = AppForDeployer.class.getResource("/" + RESOURCE_PACKAGE);
        // if (url == null)
        //     throw new IOException("File not existing");
        
        // File apps = new File(url.toURI());
        // for (File app : apps.listFiles()) {
        //     if (!app.getName().endsWith(".jar"))
        //         continue;
        //     childJars.add(app);
        //     System.out.println("successfully added to childJars: " + app.getName());
        // }

        // System.out.println("verifying childjars...");
        // childJars.forEach(f -> System.out.println("filename: " + f.getName() + ", filesize: " + f.length()));
    }

    public void uploadJar() throws IOException {

        ListIterator<File> iterator = childJars.listIterator();
        while (iterator.hasNext()) {
            File jar = iterator.next();
            String filename = jar.getName().split("\\.jar")[0];
            String groupId = getGroupId(filename);
            String version = getVersion(filename);
            String artifactId = getArtifactId(filename, groupId, version);
            
            System.out.println("uploading... : " + filename);
            int count = 0;
            while (count < 3) {
                Map<String, String> headers = new HashMap<>();
                HttpPostMultipart multipart = new HttpPostMultipart(API_URL, "utf-8", headers);
                multipart.addFormField("maven2.groupId", groupId);
                multipart.addFormField("maven2.artifactId", artifactId);
                multipart.addFormField("maven2.version", version);
                multipart.addFormField("maven2.asset1.extension", "jar");
                multipart.addFormField("maven2.generate-pom", "true");
                multipart.addFormField("maven2.packaging", "jar");
                multipart.addFilePart("maven2.asset1", jar);
                int status = multipart.finish();
                
                if (status == 204) {
                    System.out.println("uploading success: " + filename);
                    break;
                } else if (status == 400) {
                    System.err.println("seems to be duplicated: " + filename);
                    break;
                } else {
                    System.out.println("uploading tried (" + Integer.toString(count + 1) + "), got status code: " + status + ", trying again...");
                    count++;
                }
                    
            }
        }

    }
    
    private String getGroupId(String filename) {
        return Arrays.stream(filename.split("\\."))
            .takeWhile(s -> !s.contains("-"))
            .collect(Collectors.joining("."));
    }

    private String getArtifactId(String filename, String groupId, String version) {
        return filename.replace(groupId + ".", "")
            .replace("-" + version, "")
            .replace(".jar", "");
    }

    private String getVersion(String filename) {
        return Arrays.stream(filename.split("-"))
            .dropWhile(s -> !Character.isDigit(s.charAt(0)))
            .map(s -> s.replace(".jar", ""))
            .collect(Collectors.joining("-"));
    }

    public static List<JarEntry> getEntriesUnderPath(JarFile jar, Predicate<JarEntry> pred)
    {
        List<JarEntry> list = new LinkedList<>();
        Enumeration<JarEntry> entries = jar.entries();

        // has to iterate through all the Jar entries
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (pred.test(entry))
                list.add(entry);
        }
        return list;
    }

    public JarFile getThisJar() throws IOException, URISyntaxException {
        URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
        System.out.println("url: " + url);
        System.out.println("url.toURI(): " + url.toURI());
        return new JarFile(new File(url.toURI()));
    }

}