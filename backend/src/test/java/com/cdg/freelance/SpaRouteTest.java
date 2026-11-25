package com.cdg.freelance;

import com.jayway.jsonpath.JsonPath;
import com.cdg.freelance.entity.*;
import com.cdg.freelance.repository.*;
import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.service.TacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class SpaRouteTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired private FreelanceRepository freelanceRepository;
    @Autowired private TacheRepository tacheRepository;
    @Autowired private TimesheetRepository timesheetRepository;
    @Autowired private LigneTimesheetRepository ligneRepository;
    @Autowired private TacheService tacheService;

    @Test
    @Transactional
    void taskAvailabilityCountsDraftAndRejectedHours() {
        Freelance freelance = freelanceRepository.findByUtilisateurEmail("mehdi.tazi@freelance.cdg.ma").orElseThrow();
        Tache task = tacheRepository.findAllByAffectationFreelanceId(freelance.getId()).get(0);
        task.setHeuresPrevues(new BigDecimal("8"));
        tacheRepository.saveAndFlush(task);
        Timesheet sheet = new Timesheet();
        sheet.setFreelance(freelance);
        sheet.setSemaineDebut(LocalDate.of(2026, 11, 23));
        sheet.setStatut(TimesheetStatut.REJETEE);
        sheet = timesheetRepository.saveAndFlush(sheet);
        LigneTimesheet line = ligneRepository.saveAndFlush(
                new LigneTimesheet(sheet, task, sheet.getSemaineDebut(), new BigDecimal("5")));
        Timesheet draft = new Timesheet();
        draft.setFreelance(freelance);
        draft.setSemaineDebut(sheet.getSemaineDebut().plusWeeks(1));
        draft = timesheetRepository.saveAndFlush(draft);
        ligneRepository.saveAndFlush(new LigneTimesheet(draft, task, draft.getSemaineDebut(), new BigDecimal("3")));
        UserPrincipal principal = new UserPrincipal(freelance.getUtilisateur());

        assertEquals(0, ligneRepository.sumHeuresByFreelanceAndDate(
                freelance.getId(), draft.getSemaineDebut(), null).compareTo(new BigDecimal("3")));
        assertEquals(0, ligneRepository.sumHeuresByTache(task.getId(), line.getId()).compareTo(new BigDecimal("3")));
        assertEquals(0, ligneRepository.sumHeuresByTache(task.getId(), null).compareTo(new BigDecimal("8")));
        assertTrue(tacheService.listMesMissions(principal, true).stream().noneMatch(dto -> dto.getId().equals(task.getId())));
        assertTrue(tacheService.listMesMissions(principal, false).stream().anyMatch(dto -> dto.getId().equals(task.getId())));
        assertTrue(tacheRepository.findForUpdateById(task.getId()).isPresent());
        ligneRepository.delete(line);
        ligneRepository.flush();
        assertTrue(tacheService.listMesMissions(principal, true).stream().anyMatch(dto -> dto.getId().equals(task.getId())));
    }

    @Test
    void loginRouteForwardsToAngularIndex() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void dashboardRouteForwardsToAngularIndex() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void loginTokenAllowsAccessToProjectsWithIsoDates() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@cdg.ma\",\"motDePasse\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.token");

        mockMvc.perform(get("/api/projets").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dateDebut", matchesPattern("\\d{4}-\\d{2}-\\d{2}")));
    }

    @Test
    void invalidLoginReturnsFormattedJsonError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void openApiDocumentationIsAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists());
    }
}
