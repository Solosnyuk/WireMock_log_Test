import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExternalApiMockTest {

    private static WireMockServer wireMockServer;
    private static final Logger logger = LogManager.getLogger(ExternalApiMockTest.class);

    @BeforeAll
    public static void setup() {
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
    public void testErrorFromExternalApi() {
        logger.info("СТАРТ testErrorFromExternalApi");

        Response response = RestAssured.given()
                .baseUri("http://localhost:8089")
                .when()
                .get("/api/external/error");

        logger.info("Запрос отправлен. Код ответа: {}", response.getStatusCode());
        logger.info("Тело ответа: {}", response.body().asString());

        try {
            assertEquals(500, response.statusCode());
            assertEquals("Internal Server Error", response.body().asString());
            logger.info("Ошибка обработана корректно.");
        } catch (AssertionError e) {
            logger.error("Ошибка в тесте: {}", e.getMessage());
            throw e;
        }

        logger.info("ЗАВЕРШЕНИЕ testErrorFromExternalApi");
    }

    @Test
    public void testSuccessResponseFromExternalApi() {
        logger.info("СТАРТ testSuccessResponseFromExternalApi");

        Response response = RestAssured.given()
                .baseUri("http://localhost:8089")
                .when()
                .get("/api/external/success");

        logger.info("Запрос отправлен. Статус-код: {}", response.statusCode());
        logger.info("Тело ответа: {}", response.body().asPrettyString());

        try {
            assertEquals("success", response.jsonPath()
                    .getString("status"), "Поле 'status' должно быть 'success'");
            assertEquals("mock data", response.jsonPath()
                    .getString("data"), "Поле 'data' должно быть 'mock data'");
            logger.info("Данные валидны. Тест пройден успешно.");
        } catch (AssertionError e) {
            logger.error("Ошибка валидации данных: {}", e.getMessage());
            throw e;
        }

        logger.info("ЗАВЕРШЕНИЕ testSuccessResponseFromExternalApi");
    }

}