package nexuscomponentdeployer;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class JarCreator {

    private final String MAIN_CLASS = "AppForDeployer";
    private Manifest manifest = new Manifest();
    private ZipOutputStream zos;

    JarCreator(String jarname) throws IOException {
        this.zos = new ZipOutputStream(new FileOutputStream(jarname));
    }

    public void setManifest() {
        this.manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        this.manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "nexuscomponentdeployer." + this.MAIN_CLASS);
        this.manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, "nexuscomponentdeployer.HttpPostMultipart");
    }

    // https://github.com/openjdk-mirror/jdk7u-jdk/blob/master/src/share/classes/sun/tools/jar/Main.java
    public void create() throws IOException {
        String MANIFEST_DIR = "META-INF/";
        String MANIFEST_NAME = "MANIFEST.MF";
        if (this.manifest == null)
            throw new IOException("Manifest is null");

        // 01. put META-INF directory
        ZipEntry e = new ZipEntry(MANIFEST_DIR);
        e.setTime(System.currentTimeMillis());
        e.setSize(0);
        e.setCrc(0);
        this.zos.putNextEntry(e);

        // 02. put MANIFEST.MF file into directory
        e = new ZipEntry(MANIFEST_DIR + MANIFEST_NAME);
        e.setTime(System.currentTimeMillis());
        this.zos.putNextEntry(e);

        // 03. write manifest
        this.manifest.write(this.zos);

    }

    public void addJars() throws IOException {
        String DEPENDENCY_DIR = "target/dependency/";
        File dir = new File("./" + DEPENDENCY_DIR);
        if (!dir.exists() || !dir.isDirectory())
            throw new IOException("\"./" + DEPENDENCY_DIR + "\" does not exist or is not a directory");

        ZipEntry e = new ZipEntry(DEPENDENCY_DIR);
        this.zos.putNextEntry(e);

        File[] entries = dir.listFiles();
        for (File jar: entries) {
            e = new ZipEntry(DEPENDENCY_DIR + jar.getName());
            e.setTime(System.currentTimeMillis());
            this.zos.putNextEntry(e);
            this.zos.write(Files.readAllBytes(jar.toPath()));
            this.zos.closeEntry();
        }
    }

    public void addCompiledClasses(List<String> classNames) throws Exception {
        String compilationPath = "./";
        String CLASS_DIR = "nexuscomponentdeployer/";
    
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
    
        if (jc == null) {
            throw new Exception("Compiler unavailable");
        }
    
        List<JavaFileObject> fileObjects = new ArrayList<>();
        for (String className : classNames) {
            // get text file from resources
            InputStream is = App.class.getClassLoader().getResourceAsStream(className + ".txt");
            String sourceCode = IOUtils.toString(is, StandardCharsets.UTF_8);
            JavaSourceFromString jsfs = new JavaSourceFromString(className, sourceCode);
            fileObjects.add(jsfs);
        }
        
        List<String> options = new ArrayList<>();
        options.add("-d");
        options.add(compilationPath);
        options.add("-classpath");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<URL> resources = classLoader.getResources("");
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                sb.append(new File(resource.getFile())).append(File.pathSeparator);
            }
            sb.append(compilationPath);
            options.add(sb.toString());
        } catch (IOException e) {
            // handle exception
        }
        
        StringWriter output = new StringWriter();
        boolean success = jc.getTask(output, null, null, options, null, fileObjects).call();
        if (success) {
            for (String className : classNames) {
                System.out.println("Class has been successfully compiled: " + className + ".class");
                
                // Load compiled class file
                File classFile = new File(CLASS_DIR + className + ".class");
                byte[] bytes = Files.readAllBytes(classFile.toPath());
                
                // Write to zip file
                ZipEntry e = new ZipEntry(CLASS_DIR + className + ".class");
                e.setTime(System.currentTimeMillis());
                this.zos.putNextEntry(e);
                this.zos.write(bytes);
                this.zos.closeEntry();
            }
            // Delete parent directory
            File parentDir = new File(CLASS_DIR);
            FileUtils.deleteDirectory(parentDir);
        } else {
            throw new Exception("Compilation failed: " + output);
        }
    }
    
    public void finish() throws IOException {
        this.zos.close();
    }

    public class JavaSourceFromString extends SimpleJavaFileObject {
        final String code;
    
        public JavaSourceFromString( String name, String code) {
            super( URI.create("string:///" + name.replace('.','/') 
                   + Kind.SOURCE.extension),Kind.SOURCE);
            this.code = code;
        }
    
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
    

}