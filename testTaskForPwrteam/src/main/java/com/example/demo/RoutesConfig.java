package com.example.demo;

import com.example.demo.handler.GitHubHandler;
import com.example.demo.model.api.RepoDetails;
import com.example.demo.problem.NotFoundException;
import com.example.demo.problem.RetryExhaustedException;
import com.example.demo.problem.ServiceException;
import com.example.demo.problem.UnauthorizedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.RouterOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Slf4j
@Configuration
public class RoutesConfig {

    @Bean
    @RouterOperation(operation = @Operation(operationId = "getRepoDetails", summary = "Get repository details", tags = {"Repository"},
            parameters = {
                    @Parameter(in = ParameterIn.PATH, name = "username",
                            description = "The GitHub username whose repositories are to be fetched",
                            required = true, example = "apache"),
                    @Parameter(in = ParameterIn.QUERY, name = "filter",
                            description = "Filter repositories by type: forked, nonforked, or all (default is all).",
                            required = false, example = "forked",
                            schema = @Schema(
                                    type = "string",
                                    allowableValues = {"forked", "nonforked", "all"},
                                    defaultValue = "all"
                            ))
            },
            responses = {@ApiResponse(responseCode = "200", description = "Successfully fetched repository details",
                    content = @Content(schema = @Schema(implementation = RepoDetails.class))),
                    @ApiResponse(responseCode = "504", description = "The server took too long to respond. "),
                    @ApiResponse(responseCode = "408", description = "Failed to establish a connection to the server. "),
                    @ApiResponse(responseCode = "503", description = "A network error occurred while trying to reach the server. "),
                    @ApiResponse(responseCode = "404", description = "No such resource on the server, or there is no such user. "),
                    @ApiResponse(responseCode = "401", description = "User not authorized. "),
                    @ApiResponse(responseCode = "500", description = "A internal server error occurred. "),
                    @ApiResponse(responseCode = "406", description = "Media type not supported. ")
            }))
    public RouterFunction<ServerResponse> githubRoutes(GitHubHandler gitHubHandler) {
        return route()
                .GET("/users/{username}/repositories", gitHubHandler::getRepoDetails)
                .build();
    }

    @Bean
    @Order(-2)
    public WebExceptionHandler exceptionHandler() {
        return (ServerWebExchange exchange, Throwable ex) -> {
            Map<String, Object> errorResponse = new HashMap<>();

            if (ex instanceof RetryExhaustedException re) {
                Throwable cause = re.getCause();

                if (cause instanceof ReadTimeoutException rte) {
                    log.info("RouterConfig:: handling exception :: ", rte);
                    exchange.getResponse().setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
                    errorResponse.put("status", HttpStatus.GATEWAY_TIMEOUT.value());
                    errorResponse.put("message", "The server took too long to respond. " + re.getUri());
                    return writeErrorResponse(exchange, errorResponse);
                } else if (cause instanceof ConnectTimeoutException cte) {
                    log.info("RouterConfig:: handling exception :: ", cte);
                    exchange.getResponse().setStatusCode(HttpStatus.REQUEST_TIMEOUT);
                    errorResponse.put("status", HttpStatus.REQUEST_TIMEOUT.value());
                    errorResponse.put("message", "Failed to establish a connection to the server. " + re.getUri());
                    return writeErrorResponse(exchange, errorResponse);
                } else if (cause instanceof WebClientRequestException wre) {
                    log.info("RouterConfig:: handling exception :: ", wre);
                    exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                    errorResponse.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
                    errorResponse.put("message", "A network error occurred while trying to reach the server. " + re.getUri());
                    return writeErrorResponse(exchange, errorResponse);
                } else {
                    log.info("RouterConfig:: handling exception :: ", ex);
                    exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                    errorResponse.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
                    errorResponse.put("message", ex.getCause().getMessage());
                    return writeErrorResponse(exchange, errorResponse);
                }
            } else if (ex instanceof NoResourceFoundException) {
                log.info("RouterConfig:: handling exception :: ", ex);
                exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
                errorResponse.put("status", HttpStatus.NOT_FOUND.value());
                errorResponse.put("message", "please check your path - should be in such format /users/{username}/repositories");
                return writeErrorResponse(exchange, errorResponse);
            } else if (ex instanceof UnauthorizedException) {
                log.info("RouterConfig:: handling exception :: ", ex);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());
                errorResponse.put("message", ex.getMessage());
                return writeErrorResponse(exchange, errorResponse);
            } else if (ex instanceof NotFoundException) {
                log.info("RouterConfig:: handling exception :: ", ex);
                exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
                errorResponse.put("status", HttpStatus.NOT_FOUND.value());
                errorResponse.put("message", ex.getMessage());
                return writeErrorResponse(exchange, errorResponse);
            } else if (ex instanceof ServiceException) {
                Throwable cause = ex.getCause();
                log.info("RouterConfig:: handling exception :: ", ex);
                exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                errorResponse.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
                errorResponse.put("message", "A network error occurred while trying to reach the server. " + cause.getMessage());
                return writeErrorResponse(exchange, errorResponse);
            }
            log.info("RouterConfig:: handling exception :: ", ex);
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            errorResponse.put("message", ex.getMessage());
            return writeErrorResponse(exchange, errorResponse);
        };
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, Map<String, Object> errorResponse) {
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = serializeToJson(errorResponse).getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private String serializeToJson(Map<String, Object> errorResponse) {
        try {
            return new ObjectMapper().writeValueAsString(errorResponse);
        } catch (JsonProcessingException e) {
            log.warn("Error serializing response", e);
            // Fallback to a simple message if serialization fails
            return "{\"error\": \"Internal Server Error\"}";
        }
    }
}
