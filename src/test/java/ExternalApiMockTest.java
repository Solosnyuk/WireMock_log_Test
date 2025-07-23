import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExternalApiMockTest {

    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void setup() {
        RestAssured.reset();

        wireMockServer = new WireMockServer(wireMockConfig().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        WireMock.stubFor(get(urlEqualTo("/api/external/success"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"success\", \"data\": \"mock data\"}")));

        stubFor(get(urlEqualTo("/api/external/error"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));
    }

    @AfterAll
    public static void tearDown() {
        wireMockServer.stop();
    }

    @Test
    public void testErrorHandlingFromExternalApi() {
        Response response = RestAssured.given()
                .baseUri("http://localhost:8089")
                .when()
                .get("/api/external/error")
                .then()
                .extract().response();

        assertEquals(500, response.statusCode());
        assertEquals("Internal Server Error", response.body().asString());
    }

    @Test
    public void testSuccessResponseFromExternalApi() {
        Response response = RestAssured.given()
                .baseUri("http://localhost:8089")
                .when()
                .get("/api/external/success")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().response();

        assertEquals("success", response.jsonPath().getString("status"));
        assertEquals("mock data", response.jsonPath().getString("data"));
    }
}