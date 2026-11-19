package com.cdg.freelance.service;

import com.cdg.freelance.dto.alerte.AlerteDto;
import com.cdg.freelance.entity.Alerte;
import com.cdg.freelance.entity.Role;
import com.cdg.freelance.exception.ResourceNotFoundException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.AlerteRepository;
import com.cdg.freelance.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlerteService {

    private final AlerteRepository alerteRepository;
    private final EntityMapper mapper;

    public AlerteService(AlerteRepository alerteRepository, EntityMapper mapper) {
        this.alerteRepository = alerteRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AlerteDto> listAlertes(UserPrincipal currentUser) {
        requireAnalyst(currentUser);
        List<Alerte> alertes = alerteRepository.findAll();
        if (currentUser.getRole() == Role.CHEF_PROJET) {
            alertes = alertes.stream()
                    .filter(a -> a.getProjet() != null && a.getProjet().getChefProjet() != null && a.getProjet().getChefProjet().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
        }

        return alertes.stream().map(mapper::toAlerteDto).collect(Collectors.toList());
    }

    @Transactional
    public AlerteDto markAsTraitee(Long id, UserPrincipal currentUser) {
        requireAnalyst(currentUser);
        Alerte a = alerteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte non trouvée avec l'id : " + id));

        if (currentUser.getRole() == Role.CHEF_PROJET
                && (a.getProjet() == null
                || a.getProjet().getChefProjet() == null
                || !a.getProjet().getChefProjet().getId().equals(currentUser.getId()))) {
            throw new AccessDeniedException("Cette alerte ne relève pas de vos projets");
        }

        a.setTraitee(true);
        Alerte saved = alerteRepository.save(a);
        return mapper.toAlerteDto(saved);
    }

    private void requireAnalyst(UserPrincipal currentUser) {
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.CHEF_PROJET) {
            throw new AccessDeniedException("Les analyses et alertes sont réservées aux chefs de projet et administrateurs");
        }
    }
}
