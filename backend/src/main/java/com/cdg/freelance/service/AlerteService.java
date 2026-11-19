package com.cdg.freelance.service;

import com.cdg.freelance.dto.alerte.AlerteDto;
import com.cdg.freelance.entity.Alerte;
import com.cdg.freelance.entity.Freelance;
import com.cdg.freelance.entity.Role;
import com.cdg.freelance.exception.ResourceNotFoundException;
import com.cdg.freelance.exception.UnauthorizedException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.AffectationRepository;
import com.cdg.freelance.repository.AlerteRepository;
import com.cdg.freelance.repository.FreelanceRepository;
import com.cdg.freelance.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlerteService {

    private final AlerteRepository alerteRepository;
    private final FreelanceRepository freelanceRepository;
    private final AffectationRepository affectationRepository;
    private final EntityMapper mapper;

    public AlerteService(AlerteRepository alerteRepository,
                         FreelanceRepository freelanceRepository,
                         AffectationRepository affectationRepository,
                         EntityMapper mapper) {
        this.alerteRepository = alerteRepository;
        this.freelanceRepository = freelanceRepository;
        this.affectationRepository = affectationRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AlerteDto> listAlertes(UserPrincipal currentUser) {
        List<Alerte> alertes;
        if (currentUser.getRole() == Role.CHEF_PROJET) {
            alertes = alerteRepository.findAll().stream()
                    .filter(a -> a.getProjet() != null && a.getProjet().getChefProjet() != null && a.getProjet().getChefProjet().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
        } else if (currentUser.getRole() == Role.FREELANCE) {
            Freelance freelance = freelanceRepository.findByUtilisateurId(currentUser.getId()).orElse(null);
            if (freelance == null) {
                return List.of();
            }
            var projetsIds = affectationRepository.findAllByFreelanceId(freelance.getId()).stream()
                    .map(a -> a.getProjet() != null ? a.getProjet().getId() : null)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());

            alertes = alerteRepository.findAll().stream()
                    .filter(a -> a.getProjet() != null && projetsIds.contains(a.getProjet().getId()))
                    .collect(Collectors.toList());
        } else {
            alertes = alerteRepository.findAll();
        }

        return alertes.stream().map(mapper::toAlerteDto).collect(Collectors.toList());
    }

    @Transactional
    public AlerteDto markAsTraitee(Long id, UserPrincipal currentUser) {
        Alerte a = alerteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte non trouvée avec l'id : " + id));

        if (currentUser.getRole() == Role.CHEF_PROJET &&
                a.getProjet() != null &&
                (a.getProjet().getChefProjet() == null || !a.getProjet().getChefProjet().getId().equals(currentUser.getId()))) {
            throw new UnauthorizedException("Cette alerte ne relève pas de vos projets");
        }

        if (currentUser.getRole() == Role.FREELANCE && a.getProjet() != null) {
            Freelance freelance = freelanceRepository.findByUtilisateurId(currentUser.getId()).orElse(null);
            if (freelance == null) {
                throw new UnauthorizedException("Aucune fiche freelance associée à cet utilisateur");
            }
            boolean hasAccess = affectationRepository.findAllByFreelanceId(freelance.getId()).stream()
                    .anyMatch(aff -> aff.getProjet() != null && aff.getProjet().getId().equals(a.getProjet().getId()));
            if (!hasAccess) {
                throw new UnauthorizedException("Cette alerte ne relève pas de vos projets");
            }
        }

        a.setTraitee(true);
        Alerte saved = alerteRepository.save(a);
        return mapper.toAlerteDto(saved);
    }
}
