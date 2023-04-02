package nexuscomponentdeployer;

import java.io.File;
import java.util.List;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

public class DependencyDownloader {
    
    public int getDependenciesFromPomFile(File pom, File mavenHome, File mvnw) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pom);
        request.setGoals(List.of("dependency:copy-dependencies", "-Dmdep.prependGroupId=true"));

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);
        invoker.setMavenExecutable(mvnw);

        return invoker.execute(request).getExitCode();
    }

}
