package com.cdg.freelance;

import com.cdg.freelance.controller.AdminController;
import com.cdg.freelance.controller.FacturationController;
import com.cdg.freelance.controller.ProjetController;
import com.cdg.freelance.controller.TimesheetController;
import com.cdg.freelance.dto.auth.UserDto;
import com.cdg.freelance.dto.facturation.PvDto;
import com.cdg.freelance.dto.projet.ProjetDto;
import com.cdg.freelance.dto.timesheet.TimesheetDto;
import com.cdg.freelance.entity.Role;
import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.entity.Utilisateur;
import com.cdg.freelance.service.AdminService;
import com.cdg.freelance.service.PaiementService;
import com.cdg.freelance.service.ProjetService;
import com.cdg.freelance.service.PvService;
import com.cdg.freelance.service.TimesheetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CriticalControllersWebMvcTest {

    private static final HandlerMethodArgumentResolver AUTHENTICATION_PRINCIPAL_RESOLVER =
            new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                }

                @Override
                public Object resolveArgument(MethodParameter parameter,
                                              ModelAndViewContainer mavContainer,
                                              NativeWebRequest webRequest,
                                              WebDataBinderFactory binderFactory) {
                    Object principal = webRequest.getUserPrincipal();
                    if (principal instanceof UsernamePasswordAuthenticationToken authentication) {
                        return authentication.getPrincipal();
                    }
                    return principal;
                }
            };

    private PvService pvService;
    private PaiementService paiementService;
    private ProjetService projetService;
    private TimesheetService timesheetService;
    private AdminService adminService;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        pvService = mock(PvService.class);
        paiementService = mock(PaiementService.class);
        projetService = mock(ProjetService.class);
        timesheetService = mock(TimesheetService.class);
        adminService = mock(AdminService.class);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setNom("Test");
        utilisateur.setPrenom("Admin");
        utilisateur.setEmail("test@cdg.ma");
        utilisateur.setMotDePasse("password");
        utilisateur.setActif(true);
        utilisateur.setRole(Role.ADMIN);
        UserPrincipal principal = new UserPrincipal(utilisateur);
        authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void facturationEndpointReturnsPvs() throws Exception {
        PvDto pv = new PvDto();
        pv.setId(12L);
        pv.setNumero("PV-2026-12");
        when(pvService.listPvs(any())).thenReturn(List.of(pv));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new FacturationController(pvService, paiementService)
        ).setCustomArgumentResolvers(AUTHENTICATION_PRINCIPAL_RESOLVER).build();

        mvc.perform(get("/api/pv").with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].numero").value("PV-2026-12"));
    }

    @Test
    void projetEndpointReturnsProjects() throws Exception {
        ProjetDto projet = new ProjetDto();
        projet.setId(7L);
        projet.setCode("PRJ-TEST");
        when(projetService.listProjets(any())).thenReturn(List.of(projet));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new ProjetController(projetService)
        ).setCustomArgumentResolvers(AUTHENTICATION_PRINCIPAL_RESOLVER).build();

        mvc.perform(get("/api/projets").with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].code").value("PRJ-TEST"));
    }

    @Test
    void timesheetEndpointReturnsTimesheets() throws Exception {
        TimesheetDto timesheet = new TimesheetDto();
        timesheet.setId(21L);
        when(timesheetService.listTimesheets(any(), any(), any())).thenReturn(List.of(timesheet));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new TimesheetController(timesheetService)
        ).setCustomArgumentResolvers(AUTHENTICATION_PRINCIPAL_RESOLVER).build();

        mvc.perform(get("/api/timesheets").with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(21));
    }

    @Test
    void adminEndpointReturnsUsers() throws Exception {
        when(adminService.listUsers()).thenReturn(List.of(
                new UserDto(1L, "Test", "Admin", "test@cdg.ma", Role.ADMIN, true, null)
        ));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new AdminController(adminService)
        ).build();

        mvc.perform(get("/api/admin/utilisateurs").with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("test@cdg.ma"));
    }
}
