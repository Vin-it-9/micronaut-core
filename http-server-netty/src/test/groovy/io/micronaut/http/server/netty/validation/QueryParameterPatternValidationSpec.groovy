package io.micronaut.http.server.netty.validation

import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.server.netty.AbstractMicronautSpec
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern

/**
 * Test for issue #12203: @Pattern bean validation should not be applied to a null query parameter annotated with @Nullable
 */
class QueryParameterPatternValidationSpec extends AbstractMicronautSpec {

    void "test nullable query parameter with pattern should allow null"() {
        given:
        HttpClient client = HttpClient.create(embeddedServer.URL)

        when: "Call with valid pattern value"
        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/query-validation/test?code=ABC123"), 
            String
        )

        then: "Should succeed"
        response.status == HttpStatus.OK
        response.body() == "Code: ABC123"

        when: "Call without query parameter (null value)"
        response = client.toBlocking().exchange(
            HttpRequest.GET("/query-validation/test"), 
            String
        )

        then: "Should succeed because parameter is @Nullable"
        response.status == HttpStatus.OK
        response.body() == "Code: null"

        when: "Call with invalid pattern value"
        client.toBlocking().exchange(
            HttpRequest.GET("/query-validation/test?code=invalid-value"), 
            String
        )

        then: "Should fail validation"
        HttpClientResponseException e = thrown()
        e.status == HttpStatus.BAD_REQUEST

        cleanup:
        client.close()
    }

    void "test non-nullable query parameter with pattern should require value"() {
        given:
        HttpClient client = HttpClient.create(embeddedServer.URL)

        when: "Call without query parameter (null value)"
        client.toBlocking().exchange(
            HttpRequest.GET("/query-validation/test-required"), 
            String
        )

        then: "Should fail because parameter is required"
        HttpClientResponseException e = thrown()
        e.status == HttpStatus.BAD_REQUEST

        cleanup:
        client.close()
    }

    @Requires(property = "spec.name", value = "QueryParameterPatternValidationSpec")
    @Controller("/query-validation")
    @Valid
    static class TestController {
        
        @Get("/test")
        String testNullablePattern(
            @Nullable 
            @Pattern(regexp = "[A-Z0-9]+")
            @QueryValue("code") String code
        ) {
            return "Code: ${code}"
        }

        @Get("/test-required")
        String testRequiredPattern(
            @Pattern(regexp = "[A-Z0-9]+")
            @QueryValue("code") String code
        ) {
            return "Code: ${code}"
        }
    }

    @Override
    Map<String, Object> getConfiguration() {
        super.getConfiguration() + [
            'spec.name': 'QueryParameterPatternValidationSpec'
        ]
    }
}
