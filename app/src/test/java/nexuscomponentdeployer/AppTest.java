/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package nexuscomponentdeployer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


class AppTest {

    private static final String API_URL = "http://localhost:8081/nexus/service/rest/v1/components?repository=maven-releases";
    @Test
    void healthCheck() {
        int responseCode = HttpPostMultipart.checkEndpoint(API_URL);
        assertEquals(200, responseCode);

    }

}
