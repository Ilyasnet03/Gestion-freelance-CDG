package com.cdg.freelance.dto.dashboard;

import com.cdg.freelance.dto.alerte.AlerteDto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DashboardKpiDto {

    // 4 cartes principales
    private long projetsActifsCount;
    private BigDecimal budgetConsommeTotal = BigDecimal.ZERO;
    private BigDecimal budgetGlobalTotal = BigDecimal.ZERO;
    private long timesheetsAValiderCount;
    private long alertesCritiquesCount;
    private long alertesAttentionCount;

    // Graphique : Consommation de budget par projet
    private List<ProjetBudgetChartItem> consommationBudgetProjets = new ArrayList<>();

    // Graphique : Avancement par projet
    private List<ProjetAvancementChartItem> avancementProjets = new ArrayList<>();

    // Dernières alertes
    private List<AlerteDto> dernieresAlertes = new ArrayList<>();

    public DashboardKpiDto() {}

    public long getProjetsActifsCount() {
        return projetsActifsCount;
    }

    public void setProjetsActifsCount(long projetsActifsCount) {
        this.projetsActifsCount = projetsActifsCount;
    }

    public BigDecimal getBudgetConsommeTotal() {
        return budgetConsommeTotal;
    }

    public void setBudgetConsommeTotal(BigDecimal budgetConsommeTotal) {
        this.budgetConsommeTotal = budgetConsommeTotal;
    }

    public BigDecimal getBudgetGlobalTotal() {
        return budgetGlobalTotal;
    }

    public void setBudgetGlobalTotal(BigDecimal budgetGlobalTotal) {
        this.budgetGlobalTotal = budgetGlobalTotal;
    }

    public long getTimesheetsAValiderCount() {
        return timesheetsAValiderCount;
    }

    public void setTimesheetsAValiderCount(long timesheetsAValiderCount) {
        this.timesheetsAValiderCount = timesheetsAValiderCount;
    }

    public long getAlertesCritiquesCount() {
        return alertesCritiquesCount;
    }

    public void setAlertesCritiquesCount(long alertesCritiquesCount) {
        this.alertesCritiquesCount = alertesCritiquesCount;
    }

    public long getAlertesAttentionCount() {
        return alertesAttentionCount;
    }

    public void setAlertesAttentionCount(long alertesAttentionCount) {
        this.alertesAttentionCount = alertesAttentionCount;
    }

    public List<ProjetBudgetChartItem> getConsommationBudgetProjets() {
        return consommationBudgetProjets;
    }

    public void setConsommationBudgetProjets(List<ProjetBudgetChartItem> consommationBudgetProjets) {
        this.consommationBudgetProjets = consommationBudgetProjets;
    }

    public List<ProjetAvancementChartItem> getAvancementProjets() {
        return avancementProjets;
    }

    public void setAvancementProjets(List<ProjetAvancementChartItem> avancementProjets) {
        this.avancementProjets = avancementProjets;
    }

    public List<AlerteDto> getDernieresAlertes() {
        return dernieresAlertes;
    }

    public void setDernieresAlertes(List<AlerteDto> dernieresAlertes) {
        this.dernieresAlertes = dernieresAlertes;
    }

    public static class ProjetBudgetChartItem {
        private String code;
        private String nom;
        private BigDecimal budgetTotal;
        private BigDecimal budgetConsomme;
        private Double taux;

        public ProjetBudgetChartItem() {}

        public ProjetBudgetChartItem(String code, String nom, BigDecimal budgetTotal, BigDecimal budgetConsomme, Double taux) {
            this.code = code;
            this.nom = nom;
            this.budgetTotal = budgetTotal;
            this.budgetConsomme = budgetConsomme;
            this.taux = taux;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getNom() {
            return nom;
        }

        public void setNom(String nom) {
            this.nom = nom;
        }

        public BigDecimal getBudgetTotal() {
            return budgetTotal;
        }

        public void setBudgetTotal(BigDecimal budgetTotal) {
            this.budgetTotal = budgetTotal;
        }

        public BigDecimal getBudgetConsomme() {
            return budgetConsomme;
        }

        public void setBudgetConsomme(BigDecimal budgetConsomme) {
            this.budgetConsomme = budgetConsomme;
        }

        public Double getTaux() {
            return taux;
        }

        public void setTaux(Double taux) {
            this.taux = taux;
        }
    }

    public static class ProjetAvancementChartItem {
        private String code;
        private String nom;
        private Integer avancementReel;
        private Double avancementAttendu;

        public ProjetAvancementChartItem() {}

        public ProjetAvancementChartItem(String code, String nom, Integer avancementReel, Double avancementAttendu) {
            this.code = code;
            this.nom = nom;
            this.avancementReel = avancementReel;
            this.avancementAttendu = avancementAttendu;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getNom() {
            return nom;
        }

        public void setNom(String nom) {
            this.nom = nom;
        }

        public Integer getAvancementReel() {
            return avancementReel;
        }

        public void setAvancementReel(Integer avancementReel) {
            this.avancementReel = avancementReel;
        }

        public Double getAvancementAttendu() {
            return avancementAttendu;
        }

        public void setAvancementAttendu(Double avancementAttendu) {
            this.avancementAttendu = avancementAttendu;
        }
    }
}
