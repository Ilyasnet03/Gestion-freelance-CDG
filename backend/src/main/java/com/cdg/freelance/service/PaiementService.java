package com.cdg.freelance.service;

import com.cdg.freelance.dto.facturation.PaiementDto;
import com.cdg.freelance.dto.facturation.UpdatePaiementRequest;
import com.cdg.freelance.entity.Affectation;
import com.cdg.freelance.entity.LigneTimesheet;
import com.cdg.freelance.entity.Paiement;
import com.cdg.freelance.entity.PaiementStatut;
import com.cdg.freelance.entity.Projet;
import com.cdg.freelance.entity.Role;
import com.cdg.freelance.entity.Tache;
import com.cdg.freelance.entity.Timesheet;
import com.cdg.freelance.exception.BusinessException;
import com.cdg.freelance.exception.ResourceNotFoundException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.AffectationRepository;
import com.cdg.freelance.repository.LigneTimesheetRepository;
import com.cdg.freelance.repository.PaiementRepository;
import com.cdg.freelance.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final LigneTimesheetRepository ligneTimesheetRepository;
    private final AffectationRepository affectationRepository;
    private final EntityMapper mapper;

    public PaiementService(PaiementRepository paiementRepository,
                           LigneTimesheetRepository ligneTimesheetRepository,
                           AffectationRepository affectationRepository,
                           EntityMapper mapper) {
        this.paiementRepository = paiementRepository;
        this.ligneTimesheetRepository = ligneTimesheetRepository;
        this.affectationRepository = affectationRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PaiementDto> listPaiements(UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return paiementRepository.findAll().stream()
                    .map(mapper::toPaiementDto)
                    .collect(Collectors.toList());
        }

        if (currentUser.getRole() != Role.CHEF_PROJET) {
            throw new AccessDeniedException("Le suivi des paiements est réservé aux chefs de projet et administrateurs");
        }

        return paiementRepository.findAll().stream()
                .filter(p -> isManagedByChef(p, currentUser.getId()))
                .map(mapper::toPaiementDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaiementDto updatePaiement(Long id, UpdatePaiementRequest req, UserPrincipal currentUser) {
        Paiement p = paiementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement non trouvé avec l'id : " + id));

        if (currentUser.getRole() != Role.ADMIN
                && (currentUser.getRole() != Role.CHEF_PROJET || !isManagedByChef(p, currentUser.getId()))) {
            throw new AccessDeniedException("Ce paiement ne relève pas de votre périmètre");
        }
        if (req.getStatut() != PaiementStatut.PAYE) {
            throw new BusinessException("Seule la confirmation du paiement est autorisée");
        }
        if (p.getStatut() == PaiementStatut.PAYE) {
            throw new BusinessException("Ce paiement est déjà confirmé");
        }

        p.setStatut(PaiementStatut.PAYE);
        p.setDatePaiement(req.getDatePaiement() != null ? req.getDatePaiement() : LocalDate.now());
        if (req.getReference() != null) {
            p.setReference(req.getReference().trim());
        }

        Paiement saved = paiementRepository.save(p);
        return mapper.toPaiementDto(saved);
    }

    private boolean isManagedByChef(Paiement paiement, Long chefProjetId) {
        if (paiement.getPvReception() == null || paiement.getPvReception().getTimesheet() == null) {
            return false;
        }

        Timesheet timesheet = paiement.getPvReception().getTimesheet();
        List<LigneTimesheet> lignes = ligneTimesheetRepository.findAllByTimesheetId(timesheet.getId());
        if (!lignes.isEmpty()) {
            return lignes.stream().allMatch(line -> {
                Tache task = line.getTache();
                Projet project = task == null ? null : task.getProjet();
                return project != null
                        && project.getChefProjet() != null
                        && project.getChefProjet().getId().equals(chefProjetId);
            });
        }

        if (timesheet.getFreelance() == null || timesheet.getSemaineDebut() == null) {
            return false;
        }
        List<Affectation> affectations = affectationRepository.findActiveAffectationsByFreelanceAndDate(
                timesheet.getFreelance().getId(), timesheet.getSemaineDebut());
        return !affectations.isEmpty() && affectations.stream()
                .allMatch(affectation -> affectation.getProjet() != null
                        && affectation.getProjet().getChefProjet() != null
                        && affectation.getProjet().getChefProjet().getId().equals(chefProjetId));
    }
}
