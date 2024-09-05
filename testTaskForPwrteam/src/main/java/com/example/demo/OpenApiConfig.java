package com.example.demo;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "${api.common.title}",
                version = "${api.common.version}",
                description = "${api.common.description}",
                termsOfService = "${api.common.termsOfService}",
                contact = @Contact(
                        name = "${api.common.contact.name}",
                        url = "${api.common.contact.url}",
                        email = "${api.common.contact.email}"
                ),
                license = @License(
                        name = "${api.common.license}",
                        url = "${api.common.licenseUrl}"
                )
        )
)
public class OpenApiConfig {

}
