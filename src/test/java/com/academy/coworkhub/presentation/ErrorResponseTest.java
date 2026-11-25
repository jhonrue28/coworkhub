package com.academy.coworkhub.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Códigos HTTP y cuerpo de error único: {"error", "codigo", "timestamp"}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithUserDetails("admin")
class ErrorResponseTest {

    @Autowired MockMvc mvc;

    private static void assertError(ResultActions result, int status, String code) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.codigo").value(code))
                .andExpect(jsonPath("$.error").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")));
    }

    @Test
    void escenario12_crearSedeSinNombre_400() throws Exception {
        assertError(mvc.perform(post("/api/sedes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ciudad\":\"Bogotá\",\"direccion\":\"Calle 1\",\"horaApertura\":\"08:00\",\"horaCierre\":\"18:00\"}")),
                400, "DATOS_INVALIDOS");
        mvc.perform(post("/api/sedes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ciudad\":\"Bogotá\",\"direccion\":\"Calle 1\",\"horaApertura\":\"08:00\",\"horaCierre\":\"18:00\"}"))
                .andExpect(jsonPath("$.error").value(containsString("nombre")));
    }

    @Test
    void datosConFormatoInvalido_400() throws Exception {
        assertError(mvc.perform(post("/api/sedes").contentType(MediaType.APPLICATION_JSON).content("{malformado")),
                400, "DATOS_INVALIDOS");
        assertError(mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"X\",\"tipo\":\"PISCINA\",\"capacidad\":1,\"tarifaHora\":1,\"sedeId\":1}")),
                400, "DATOS_INVALIDOS");
        assertError(mvc.perform(get("/api/miembros").param("estado", "FOO")), 400, "DATOS_INVALIDOS");
        assertError(mvc.perform(get("/api/sedes/abc")), 400, "DATOS_INVALIDOS");
        assertError(mvc.perform(get("/api/reservas")), 400, "DATOS_INVALIDOS");
        assertError(mvc.perform(get("/api/disponibilidad").param("sedeId", "1")), 400, "DATOS_INVALIDOS");
    }

    @Test
    void recursoInexistente_404() throws Exception {
        assertError(mvc.perform(get("/api/sedes/999")), 404, "NO_ENCONTRADO");
        assertError(mvc.perform(get("/api/reservas/999")), 404, "NO_ENCONTRADO");
        assertError(mvc.perform(get("/api/no-existe")), 404, "NO_ENCONTRADO");
    }

    @Test
    void escenario11_eliminarSalaConReservas_409() throws Exception {
        assertError(mvc.perform(delete("/api/salas/1")), 409, "RN-12");
    }

    @Test
    void escenario8_confirmarReservaCancelada_409() throws Exception {
        // La reserva 5 de import.sql está CANCELLED
        assertError(mvc.perform(post("/api/reservas/5/confirmar")), 409, "RN-09");
    }

    @Test
    void duplicado_409() throws Exception {
        assertError(mvc.perform(post("/api/equipamientos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Proyector\"}")), 409, "RN-11");
    }

    @Test
    void metodoNoPermitido_405() throws Exception {
        assertError(mvc.perform(patch("/api/sedes")), 405, "METODO_NO_PERMITIDO");
    }

    @Test
    void eliminarResponde200SinCuerpo() throws Exception {
        mvc.perform(delete("/api/equipamientos/1")).andExpect(status().isConflict()); // en uso
        String body = mvc.perform(post("/api/equipamientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Parlante\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");
        mvc.perform(delete("/api/equipamientos/" + id))
                .andExpect(status().isOk())
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertEquals("", result.getResponse().getContentAsString()));
    }
}
