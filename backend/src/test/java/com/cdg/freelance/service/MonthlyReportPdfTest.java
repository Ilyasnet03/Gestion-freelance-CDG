package com.cdg.freelance.service;

import com.cdg.freelance.dto.projet.ProjetDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonthlyReportPdfTest {

    @Test
    void rendersBrandedPdfWithManyProjects() {
        List<ProjetDto> projects = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            ProjetDto p = new ProjetDto();
            p.setCode("P-" + i);
            p.setNom("Projet é" + i);
            p.setBudget(BigDecimal.valueOf(100000));
            p.setBudgetConsomme(BigDecimal.valueOf(i * 3000));
            p.setAvancementGlobal(i * 2);
            projects.add(p);
        }
        byte[] pdf = MonthlyReportPdf.render(YearMonth.of(2025, 5), "ADMIN", projects, List.of(),
                BigDecimal.valueOf(4000000), BigDecimal.valueOf(1200000));
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
        assertTrue(pdf.length > 5000);
    }
}
