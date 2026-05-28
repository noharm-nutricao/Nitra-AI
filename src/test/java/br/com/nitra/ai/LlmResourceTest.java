package br.com.nitra.ai;

import br.com.nitra.ai.service.BedrockLlmService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;

@QuarkusTest
class LlmResourceTest {

    @InjectMock
    BedrockLlmService llmService;

    @Test
    @TestSecurity(user = "service-client", roles = {"nitra-ai/read"})
    void shouldReturnChatResponse() {
        when(llmService.invokeLlmModel("Explique Bedrock", "GPT"))
                .thenReturn("Bedrock é um serviço gerenciado.");

        given()
                .contentType("application/json")
                .queryParam("model", "GPT")
                .body("""
                        {
                          "message": "Explique Bedrock"
                        }
                        """)
                .when()
                .post("/llm/chat")
                .then()
                .statusCode(200)
                .body("response", is("Bedrock é um serviço gerenciado."));
    }

    @Test
    @TestSecurity(user = "service-client", roles = {"nitra-ai/write"})
    void shouldReturnImageChatResponse() {
        when(llmService.invokeLlmModelWithImage("Descreva a imagem", "aGVsbG8=", "PNG", "AMAZON_LITE"))
                .thenReturn("A imagem contém um exemplo sintético.");

        given()
                .contentType("application/json")
                .queryParam("model", "AMAZON_LITE")
                .body("""
                        {
                          "message": "Descreva a imagem",
                          "imageBase64": "aGVsbG8=",
                          "imageFormat": "PNG"
                        }
                        """)
                .when()
                .post("/llm/chat/image")
                .then()
                .statusCode(200)
                .body("response", is("A imagem contém um exemplo sintético."));
    }

    @Test
    @TestSecurity(user = "service-client", roles = {"nitra-ai/read"})
    void shouldReturnBadRequestWhenMessageIsBlank() {
        given()
                .contentType("application/json")
                .queryParam("model", "GPT")
                .body("""
                        {
                          "message": " "
                        }
                        """)
                .when()
                .post("/llm/chat")
                .then()
                .statusCode(400)
                .body("code", is("INVALID_REQUEST"))
                .body("message", containsString("A mensagem é obrigatória."))
                .body("path", is("/llm/chat"));
    }

    @Test
    @TestSecurity(user = "service-client", roles = {"nitra-ai/read"})
    void shouldMapIllegalArgumentException() {
        when(llmService.invokeLlmModel("oi", "INVALIDO"))
                .thenThrow(new IllegalArgumentException("Modelo inválido: INVALIDO."));

        given()
                .contentType("application/json")
                .queryParam("model", "INVALIDO")
                .body("""
                        {
                          "message": "oi"
                        }
                        """)
                .when()
                .post("/llm/chat")
                .then()
                .statusCode(400)
                .body("code", is("INVALID_ARGUMENT"))
                .body("message", is("Modelo inválido: INVALIDO."))
                .body("path", is("/llm/chat"));
    }

    @Test
    @TestSecurity(user = "service-client", roles = {"nitra-ai/write"})
    void shouldReturnBadRequestWhenImageBase64IsBlank() {
        given()
                .contentType("application/json")
                .queryParam("model", "ANTHROPIC")
                .body("""
                        {
                          "message": "Analise",
                          "imageBase64": " ",
                          "imageFormat": "PNG"
                        }
                        """)
                .when()
                .post("/llm/chat/image")
                .then()
                .statusCode(400)
                .body("code", is("INVALID_REQUEST"))
                .body("message", containsString("A imagem em base64 é obrigatória."))
                .body("path", is("/llm/chat/image"));
    }

    @Test
    @TestSecurity(user = "service-client", roles = {"nitra-ai/read"})
    void shouldReturnForbiddenWhenScopeIsMissing() {
        given()
                .contentType("application/json")
                .queryParam("model", "AMAZON_LITE")
                .body("""
                        {
                          "message": "Descreva a imagem",
                          "imageBase64": "aGVsbG8=",
                          "imageFormat": "PNG"
                        }
                        """)
                .when()
                .post("/llm/chat/image")
                .then()
                .statusCode(403);
    }

    @Test
    void shouldExposeOpenApiAndSwaggerUi() {
        given()
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .body(containsString("/llm/chat"))
                .body(containsString("/llm/chat/image"));

        given()
                .when()
                .get("/q/swagger-ui/")
                .then()
                .statusCode(200)
                .body(containsString("OpenAPI UI"));
    }
}
