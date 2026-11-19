package com.cdg.freelance.mapper;

import com.cdg.freelance.dto.admin.ParametreDto;
import com.cdg.freelance.dto.admin.TypeProjetDto;
import com.cdg.freelance.dto.alerte.AlerteDto;
import com.cdg.freelance.dto.auth.UserDto;
import com.cdg.freelance.dto.facturation.PaiementDto;
import com.cdg.freelance.dto.facturation.PvDto;
import com.cdg.freelance.dto.freelance.FreelanceDto;
import com.cdg.freelance.dto.projet.AffectationDto;
import com.cdg.freelance.dto.projet.ProjetDto;
import com.cdg.freelance.dto.tache.TacheDto;
import com.cdg.freelance.dto.timesheet.LigneTimesheetDto;
import com.cdg.freelance.dto.timesheet.TimesheetDto;
import com.cdg.freelance.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Component
public class EntityMapper {

    public UserDto toUserDto(Utilisateur u, Long freelanceId) {
        if (u == null) return null;
        return new UserDto(
                u.getId(),
                u.getNom(),
                u.getPrenom(),
                u.getEmail(),
                u.getRole(),
                u.getActif(),
                freelanceId
        );
    }

    public FreelanceDto toFreelanceDto(Freelance f) {
        if (f == null) return null;
        FreelanceDto dto = new FreelanceDto();
        dto.setId(f.getId());
        dto.setTjm(f.getTjm());
        dto.setSpecialite(f.getSpecialite());
        dto.setStatut(f.getStatut());
        if (f.getUtilisateur() != null) {
            dto.setUtilisateurId(f.getUtilisateur().getId());
            dto.setNom(f.getUtilisateur().getNom());
            dto.setPrenom(f.getUtilisateur().getPrenom());
            dto.setEmail(f.getUtilisateur().getEmail());
        }
        return dto;
    }

    public TypeProjetDto toTypeProjetDto(TypeProjet tp) {
        if (tp == null) return null;
        return new TypeProjetDto(tp.getId(), tp.getLibelle());
    }

    public ParametreDto toParametreDto(Parametre p) {
        if (p == null) return null;
        return new ParametreDto(p.getCle(), p.getValeur());
    }

    public ProjetDto toProjetDto(Projet p) {
        if (p == null) return null;
        ProjetDto dto = new ProjetDto();
        dto.setId(p.getId());
        dto.setCode(p.getCode());
        dto.setNom(p.getNom());
        dto.setDescription(p.getDescription());
        dto.setBudget(p.getBudget());
        dto.setDateDebut(p.getDateDebut());
        dto.setDateFinPrevue(p.getDateFinPrevue());
        dto.setStatut(p.getStatut());
        if (p.getTypeProjet() != null) {
            dto.setTypeProjetId(p.getTypeProjet().getId());
            dto.setTypeProjetLibelle(p.getTypeProjet().getLibelle());
        }
        if (p.getChefProjet() != null) {
            dto.setChefProjetId(p.getChefProjet().getId());
            dto.setChefProjetNomComplet(p.getChefProjet().getPrenom() + " " + p.getChefProjet().getNom());
        }
        return dto;
    }

    public AffectationDto toAffectationDto(Affectation a) {
        if (a == null) return null;
        AffectationDto dto = new AffectationDto();
        dto.setId(a.getId());
        if (a.getProjet() != null) {
            dto.setProjetId(a.getProjet().getId());
            dto.setProjetNom(a.getProjet().getNom());
        }
        if (a.getFreelance() != null) {
            dto.setFreelanceId(a.getFreelance().getId());
            dto.setFreelanceSpecialite(a.getFreelance().getSpecialite());
            if (a.getFreelance().getUtilisateur() != null) {
                dto.setFreelanceNomComplet(a.getFreelance().getUtilisateur().getPrenom() + " " + a.getFreelance().getUtilisateur().getNom());
            }
        }
        dto.setTjmApplique(a.getTjmApplique());
        dto.setDateDebut(a.getDateDebut());
        dto.setDateFin(a.getDateFin());
        return dto;
    }

    public TacheDto toTacheDto(Tache t) {
        if (t == null) return null;
        TacheDto dto = new TacheDto();
        dto.setId(t.getId());
        dto.setTitre(t.getTitre());
        dto.setChargeEstimee(t.getChargeEstimee());
        dto.setAvancement(t.getAvancement());
        dto.setStatut(t.getStatut());
        dto.setDateDebut(t.getDateDebut());
        dto.setDateFin(t.getDateFin());
        if (t.getProjet() != null) {
            dto.setProjetId(t.getProjet().getId());
            dto.setProjetNom(t.getProjet().getNom());
        }
        if (t.getAffectation() != null) {
            dto.setAffectationId(t.getAffectation().getId());
            if (t.getAffectation().getFreelance() != null) {
                dto.setFreelanceId(t.getAffectation().getFreelance().getId());
                if (t.getAffectation().getFreelance().getUtilisateur() != null) {
                    dto.setFreelanceNomComplet(t.getAffectation().getFreelance().getUtilisateur().getPrenom() + " " + t.getAffectation().getFreelance().getUtilisateur().getNom());
                }
            }
        }
        return dto;
    }

    public LigneTimesheetDto toLigneTimesheetDto(LigneTimesheet l) {
        if (l == null) return null;
        LigneTimesheetDto dto = new LigneTimesheetDto();
        dto.setId(l.getId());
        if (l.getTimesheet() != null) {
            dto.setTimesheetId(l.getTimesheet().getId());
        }
        if (l.getTache() != null) {
            dto.setTacheId(l.getTache().getId());
            dto.setTacheTitre(l.getTache().getTitre());
            if (l.getTache().getProjet() != null) {
                dto.setProjetId(l.getTache().getProjet().getId());
                dto.setProjetNom(l.getTache().getProjet().getNom());
            }
        }
        dto.setDateSaisie(l.getDateSaisie());
        dto.setHeures(l.getHeures());
        return dto;
    }

    public TimesheetDto toTimesheetDto(Timesheet t) {
        if (t == null) return null;
        TimesheetDto dto = new TimesheetDto();
        dto.setId(t.getId());
        dto.setSemaineDebut(t.getSemaineDebut());
        dto.setStatut(t.getStatut());
        dto.setMotifRejet(t.getMotifRejet());
        dto.setDateSoumission(t.getDateSoumission());
        dto.setDateDecision(t.getDateDecision());

        if (t.getFreelance() != null) {
            dto.setFreelanceId(t.getFreelance().getId());
            if (t.getFreelance().getUtilisateur() != null) {
                dto.setFreelanceNomComplet(t.getFreelance().getUtilisateur().getPrenom() + " " + t.getFreelance().getUtilisateur().getNom());
            }
        }

        if (t.getValidePar() != null) {
            dto.setValideParId(t.getValidePar().getId());
            dto.setValideParNomComplet(t.getValidePar().getPrenom() + " " + t.getValidePar().getNom());
        }

        if (t.getLignes() != null) {
            dto.setLignes(t.getLignes().stream().map(this::toLigneTimesheetDto).collect(Collectors.toList()));
            BigDecimal totalHeures = t.getLignes().stream()
                    .map(LigneTimesheet::getHeures)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setTotalHeuresSemaine(totalHeures);

            // Determine associated project from the first line's task
            if (!t.getLignes().isEmpty() && t.getLignes().get(0).getTache() != null && t.getLignes().get(0).getTache().getProjet() != null) {
                dto.setProjetId(t.getLignes().get(0).getTache().getProjet().getId());
                dto.setProjetNom(t.getLignes().get(0).getTache().getProjet().getNom());
            }
        }

        return dto;
    }

    public PvDto toPvDto(PvReception pv) {
        if (pv == null) return null;
        PvDto dto = new PvDto();
        dto.setId(pv.getId());
        dto.setNumero(pv.getNumero());
        dto.setTotalHeures(pv.getTotalHeures());
        dto.setMontant(pv.getMontant());
        dto.setCheminPdf(pv.getCheminPdf());
        dto.setDateGeneration(pv.getDateGeneration());

        if (pv.getTimesheet() != null) {
            dto.setTimesheetId(pv.getTimesheet().getId());
            dto.setSemaineDebut(pv.getTimesheet().getSemaineDebut());
            if (pv.getTimesheet().getFreelance() != null) {
                dto.setFreelanceSpecialite(pv.getTimesheet().getFreelance().getSpecialite());
                if (pv.getTimesheet().getFreelance().getUtilisateur() != null) {
                    dto.setFreelanceNomComplet(pv.getTimesheet().getFreelance().getUtilisateur().getPrenom() + " " + pv.getTimesheet().getFreelance().getUtilisateur().getNom());
                }
            }
            if (pv.getTimesheet().getLignes() != null && !pv.getTimesheet().getLignes().isEmpty()) {
                Tache firstTache = pv.getTimesheet().getLignes().get(0).getTache();
                if (firstTache != null && firstTache.getProjet() != null) {
                    dto.setProjetCode(firstTache.getProjet().getCode());
                    dto.setProjetNom(firstTache.getProjet().getNom());
                }
            }
        }
        return dto;
    }

    public PaiementDto toPaiementDto(Paiement p) {
        if (p == null) return null;
        PaiementDto dto = new PaiementDto();
        dto.setId(p.getId());
        dto.setStatut(p.getStatut());
        dto.setDatePaiement(p.getDatePaiement());
        dto.setReference(p.getReference());
        if (p.getPvReception() != null) {
            dto.setPvId(p.getPvReception().getId());
            dto.setNumeroPv(p.getPvReception().getNumero());
            dto.setMontant(p.getPvReception().getMontant());
            if (p.getPvReception().getTimesheet() != null) {
                dto.setTimesheetId(p.getPvReception().getTimesheet().getId());
                if (p.getPvReception().getTimesheet().getFreelance() != null && p.getPvReception().getTimesheet().getFreelance().getUtilisateur() != null) {
                    dto.setFreelanceNomComplet(p.getPvReception().getTimesheet().getFreelance().getUtilisateur().getPrenom() + " " + p.getPvReception().getTimesheet().getFreelance().getUtilisateur().getNom());
                }
                if (p.getPvReception().getTimesheet().getLignes() != null && !p.getPvReception().getTimesheet().getLignes().isEmpty()) {
                    Tache firstTache = p.getPvReception().getTimesheet().getLignes().get(0).getTache();
                    if (firstTache != null && firstTache.getProjet() != null) {
                        dto.setProjetNom(firstTache.getProjet().getNom());
                    }
                }
            }
        }
        return dto;
    }

    public AlerteDto toAlerteDto(Alerte a) {
        if (a == null) return null;
        AlerteDto dto = new AlerteDto();
        dto.setId(a.getId());
        if (a.getProjet() != null) {
            dto.setProjetId(a.getProjet().getId());
            dto.setProjetCode(a.getProjet().getCode());
            dto.setProjetNom(a.getProjet().getNom());
        }
        dto.setType(a.getType());
        dto.setNiveau(a.getNiveau());
        dto.setMessage(a.getMessage());
        dto.setDateCreation(a.getDateCreation());
        dto.setTraitee(a.getTraitee());
        return dto;
    }
}
