package com.academy.coworkhub.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Roles y dueño del recurso (RN-13). En import.sql: ana es el miembro 1 (reserva 1) y carlos el 2 (reserva 2). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityTest {

    @Autowired MockMvc mvc;

    @Test
    void escenario10_sinCredenciales_401() throws Exception {
        mvc.perform(get("/api/sedes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mvc.perform(get("/api/sedes").with(httpBasic("admin", "clave-incorrecta")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void publicos_sinCredenciales() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("ana")
    void escenario9_miembroConsultaReservaAjena_403() throws Exception {
        mvc.perform(get("/api/reservas/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        mvc.perform(get("/api/reservas/1")).andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("ana")
    void miembro_soloSusDatos() throws Exception {
        mvc.perform(get("/api/miembros/1")).andExpect(status().isOk());
        mvc.perform(get("/api/miembros/1/consumo")).andExpect(status().isOk());
        mvc.perform(get("/api/reservas").param("miembroId", "1")).andExpect(status().isOk());

        mvc.perform(get("/api/miembros/2")).andExpect(status().isForbidden());
        mvc.perform(get("/api/miembros/2/consumo")).andExpect(status().isForbidden());
        mvc.perform(get("/api/miembros")).andExpect(status().isForbidden());
        mvc.perform(get("/api/reservas").param("miembroId", "2")).andExpect(status().isForbidden());
        mvc.perform(get("/api/reservas").param("estado", "PENDING")).andExpect(status().isForbidden());
        mvc.perform(post("/api/reservas/1/confirmar")).andExpect(status().isForbidden());
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salaId\":1,\"miembroId\":2,\"inicio\":\"2030-06-03T10:00:00\",\"fin\":\"2030-06-03T11:00:00\",\"asistentes\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("ana")
    void miembro_leeCatalogosPeroNoLosModifica() throws Exception {
        mvc.perform(get("/api/sedes")).andExpect(status().isOk());
        mvc.perform(delete("/api/equipamientos/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("recepcion")
    void recepcion_gestionaMiembrosPeroNoCatalogos() throws Exception {
        mvc.perform(get("/api/miembros")).andExpect(status().isOk());
        mvc.perform(get("/api/reservas/2")).andExpect(status().isOk());
        mvc.perform(post("/api/equipamientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Parlante\"}"))
                .andExpect(status().isForbidden());
    }
}
