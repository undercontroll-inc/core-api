package com.undercontroll.infrastructure.config;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class ApiResponseDocumentation {


    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "200",
            description = "Requisição processada com sucesso"
    )
    public @interface SuccessResponse {
    }


    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "201",
            description = "Recurso criado com sucesso"
    )
    public @interface CreatedResponse {
    }


    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "204",
            description = "Requisição processada com sucesso, mas sem conteúdo para retornar"
    )
    public @interface NoContentResponse {
    }


    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "400",
            description = "Requisição inválida - Dados fornecidos estão incorretos ou incompletos",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            name = "Exemplo de erro de validação",
                            value = """
                                    {
                                        "timestamp": "2025-11-23T14:30:00",
                                        "status": 400,
                                        "error": "Bad Request",
                                        "message": "Dados de requisição inválidos",
                                        "path": "/v1/api/components"
                                    }
                                    """
                    )
            )
    )
    public @interface BadRequestResponse {
    }


    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "401",
            description = "Não autorizado - Token JWT inválido, expirado ou não fornecido",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            name = "Exemplo de erro de autenticação",
                            value = """
                                    {
                                        "timestamp": "2025-11-23T14:30:00",
                                        "status": 401,
                                        "error": "Unauthorized",
                                        "message": "Token JWT inválido ou expirado",
                                        "path": "/v1/api/components"
                                    }
                                    """
                    )
            )
    )
    public @interface UnauthorizedResponse {
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "403",
            description = "Acesso negado - Usuário não possui permissão para acessar este recurso",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            name = "Exemplo de erro de autorização",
                            value = """
                                    {
                                        "timestamp": "2025-11-23T14:30:00",
                                        "status": 403,
                                        "error": "Forbidden",
                                        "message": "Usuário não possui permissão para acessar este recurso",
                                        "path": "/v1/api/components"
                                    }
                                    """
                    )
            )
    )
    public @interface ForbiddenResponse {
    }


    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "404",
            description = "Recurso não encontrado - O recurso solicitado não existe",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            name = "Exemplo de recurso não encontrado",
                            value = """
                                    {
                                        "timestamp": "2025-11-23T14:30:00",
                                        "status": 404,
                                        "error": "Not Found",
                                        "message": "Componente com ID 123 não encontrado",
                                        "path": "/v1/api/components/123"
                                    }
                                    """
                    )
            )
    )
    public @interface NotFoundResponse {
    }


    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "409",
            description = "Conflito - Recurso já existe ou há conflito de dados",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            name = "Exemplo de conflito",
                            value = """
                                    {
                                        "timestamp": "2025-11-23T14:30:00",
                                        "status": 409,
                                        "error": "Conflict",
                                        "message": "Componente com este nome já existe",
                                        "path": "/v1/api/components"
                                    }
                                    """
                    )
            )
    )
    public @interface ConflictResponse {
    }


    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor - Ocorreu um erro inesperado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            name = "Exemplo de erro interno",
                            value = """
                                    {
                                        "timestamp": "2025-11-23T14:30:00",
                                        "status": 500,
                                        "error": "Internal Server Error",
                                        "message": "Ocorreu um erro inesperado ao processar a requisição",
                                        "path": "/v1/api/components"
                                    }
                                    """
                    )
            )
    )
    public @interface InternalServerErrorResponse {
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Requisição processada com sucesso"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requisição inválida",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Não autorizado - Token inválido ou expirado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado - Permissões insuficientes",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno do servidor",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public @interface StandardApiResponses {
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Requisição processada com sucesso"),
            @ApiResponse(responseCode = "204", description = "Sem conteúdo para retornar"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Não autorizado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Recurso não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public @interface GetApiResponses {
    }


    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recurso criado com sucesso"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requisição inválida",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Não autorizado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflito - Recurso já existe",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public @interface PostApiResponses {
    }


    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recurso atualizado com sucesso"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requisição inválida",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Não autorizado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Recurso não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public @interface PutApiResponses {
    }


    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recurso deletado com sucesso"),
            @ApiResponse(responseCode = "204", description = "Recurso deletado com sucesso (sem conteúdo)"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Não autorizado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Recurso não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public @interface DeleteApiResponses {
    }


    @Schema(description = "Estrutura padrão de resposta de erro")
    public static class ErrorResponse {
        @Schema(description = "Timestamp do erro", example = "2025-11-23T14:30:00")
        private String timestamp;

        @Schema(description = "Código de status HTTP", example = "400")
        private Integer status;

        @Schema(description = "Tipo de erro", example = "Bad Request")
        private String error;

        @Schema(description = "Mensagem descritiva do erro", example = "Dados de requisição inválidos")
        private String message;

        @Schema(description = "Caminho da requisição que gerou o erro", example = "/v1/api/components")
        private String path;
    }
}

