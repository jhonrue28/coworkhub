package com.academy.coworkhub.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Swagger documenta los códigos que cada endpoint puede devolver, y solo esos. */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired MockMvc mvc;

    @Test
    void codigosDeRespuestaDocumentados() throws Exception {
        String login = "$.paths['/auth/login'].post.responses";
        String reserva = "$.paths['/api/reservas'].post.responses";
        String suspender = "$.paths['/api/miembros/{id}/suspender'].patch.responses";
        mvc.perform(get("/v3/api-docs/todo"))
                .andExpect(status().isOk())
                // El login es público y no escribe: 200, 400 y 401, sin 403 ni 409
                .andExpect(jsonPath(login + "['200']").exists())
                .andExpect(jsonPath(login + "['400']").exists())
                .andExpect(jsonPath(login + "['401']").exists())
                .andExpect(jsonPath(login + "['403']").doesNotExist())
                .andExpect(jsonPath(login + "['409']").doesNotExist())
                .andExpect(jsonPath("$.paths['/auth/login'].post.security").isEmpty())
                // Crear reserva sí puede romper reglas y exige token
                .andExpect(jsonPath(reserva + "['400']").exists())
                .andExpect(jsonPath(reserva + "['401']").exists())
                .andExpect(jsonPath(reserva + "['403']").exists())
                .andExpect(jsonPath(reserva + "['409']").exists())
                // Suspender no rompe ninguna regla
                .andExpect(jsonPath(suspender + "['404']").exists())
                .andExpect(jsonPath(suspender + "['409']").doesNotExist());
    }
}
