package com.cdg.freelance.service;

import com.cdg.freelance.dto.tache.CreateTacheRequest;
import com.cdg.freelance.dto.tache.TacheDto;
import com.cdg.freelance.dto.tache.UpdateAvancementRequest;
import com.cdg.freelance.dto.tache.UpdateTacheRequest;
import com.cdg.freelance.entity.Affectation;
import com.cdg.freelance.entity.Freelance;
import com.cdg.freelance.entity.Projet;
import com.cdg.freelance.entity.Role;
import com.cdg.freelance.entity.Tache;
import com.cdg.freelance.entity.TacheStatut;
import com.cdg.freelance.exception.BusinessException;
import com.cdg.freelance.exception.ResourceNotFoundException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.AffectationRepository;
import com.cdg.freelance.repository.FreelanceRepository;
import com.cdg.freelance.repository.ProjetRepository;
import com.cdg.freelance.repository.TacheRepository;
import com.cdg.freelance.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TacheService {

    private final TacheRepository tacheRepository;
    private final ProjetRepository projetRepository;
    private final AffectationRepository affectationRepository;
    private final FreelanceRepository freelanceRepository;
    private final EntityMapper mapper;

    public TacheService(TacheRepository tacheRepository,
                        ProjetRepository projetRepository,
                        AffectationRepository affectationRepository,
                        FreelanceRepository freelanceRepository,
                        EntityMapper mapper) {
        this.tacheRepository = tacheRepository;
        this.projetRepository = projetRepository;
        this.affectationRepository = affectationRepository;
        this.freelanceRepository = freelanceRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TacheDto> listMesMissions(UserPrincipal currentUser) {
        if (currentUser.getRole() != Role.FREELANCE) {
            throw new AccessDeniedException("Cette liste est réservée au freelance connecté");
        }
        Freelance freelance = freelanceRepository.findByUtilisateurId(currentUser.getId())
                .orElseThrow(() -> new BusinessException("Aucun profil freelance n'est associé à votre compte"));
        return tacheRepository.findAllByAffectationFreelanceId(freelance.getId()).stream()
                .map(mapper::toTacheDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TacheDto> listTachesByProjet(Long projetId) {
        return tacheRepository.findAllByProjetId(projetId).stream()
                .map(mapper::toTacheDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TacheDto getTacheById(Long id) {
        Tache t = tacheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'id : " + id));
        return mapper.toTacheDto(t);
    }

    @Transactional
    public TacheDto createTache(Long projetId, CreateTacheRequest req) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé avec l'id : " + projetId));

        Affectation affectation = null;
        if (req.getAffectationId() != null) {
            affectation = affectationRepository.findById(req.getAffectationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Affectation non trouvée"));
            if (!affectation.getProjet().getId().equals(projetId)) {
                throw new BusinessException("L'affectation choisie n'appartient pas à ce projet");
            }
        }

        Tache t = new Tache();
        t.setProjet(projet);
        t.setAffectation(affectation);
        t.setTitre(req.getTitre().trim());
        t.setChargeEstimee(req.getChargeEstimee());
        t.setAvancement(0);
        t.setStatut(TacheStatut.A_FAIRE);
        t.setDateDebut(req.getDateDebut());
        t.setDateFin(req.getDateFin());

        Tache saved = tacheRepository.save(t);
        return mapper.toTacheDto(saved);
    }

    @Transactional
    public TacheDto updateTache(Long id, UpdateTacheRequest req) {
        Tache t = tacheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'id : " + id));

        t.setTitre(req.getTitre().trim());
        t.setChargeEstimee(req.getChargeEstimee());
        if (req.getAvancement() != null) {
            t.setAvancement(req.getAvancement());
            if (req.getAvancement() == 100) {
                t.setStatut(TacheStatut.TERMINEE);
            } else if (req.getAvancement() > 0 && t.getStatut() == TacheStatut.A_FAIRE) {
                t.setStatut(TacheStatut.EN_COURS);
            }
        }
        if (req.getStatut() != null) {
            t.setStatut(req.getStatut());
        }
        if (req.getAffectationId() != null) {
            Affectation aff = affectationRepository.findById(req.getAffectationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Affectation non trouvée"));
            if (!aff.getProjet().getId().equals(t.getProjet().getId())) {
                throw new BusinessException("L'affectation n'appartient pas au projet de la tâche");
            }
            t.setAffectation(aff);
        }
        t.setDateDebut(req.getDateDebut());
        t.setDateFin(req.getDateFin());

        Tache updated = tacheRepository.save(t);
        return mapper.toTacheDto(updated);
    }

    @Transactional
    public TacheDto updateAvancement(Long id, UpdateAvancementRequest req, UserPrincipal currentUser) {
        Tache t = tacheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'id : " + id));

        if (currentUser.getRole() != Role.CHEF_PROJET
                || t.getProjet() == null
                || t.getProjet().getChefProjet() == null
                || !t.getProjet().getChefProjet().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Seul le chef de projet concerné peut modifier l'avancement de cette tâche");
        }

        t.setAvancement(req.getAvancement());
        if (req.getAvancement() == 100) {
            t.setStatut(TacheStatut.TERMINEE);
        } else if (req.getAvancement() > 0 && t.getStatut() == TacheStatut.A_FAIRE) {
            t.setStatut(TacheStatut.EN_COURS);
        }

        Tache updated = tacheRepository.save(t);
        return mapper.toTacheDto(updated);
    }
}
