package com.cdg.freelance.service;

import com.cdg.freelance.dto.freelance.CreateFreelanceRequest;
import com.cdg.freelance.dto.freelance.FreelanceDto;
import com.cdg.freelance.dto.freelance.UpdateFreelanceRequest;
import com.cdg.freelance.entity.Freelance;
import com.cdg.freelance.entity.Role;
import com.cdg.freelance.entity.Utilisateur;
import com.cdg.freelance.exception.BusinessException;
import com.cdg.freelance.exception.ResourceNotFoundException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.FreelanceRepository;
import com.cdg.freelance.repository.UtilisateurRepository;
import com.cdg.freelance.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FreelanceService {

    private final FreelanceRepository freelanceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EntityMapper mapper;

    public FreelanceService(FreelanceRepository freelanceRepository,
                            UtilisateurRepository utilisateurRepository,
                            EntityMapper mapper) {
        this.freelanceRepository = freelanceRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<FreelanceDto> listFreelances() {
        return freelanceRepository.findAll().stream()
                .map(mapper::toFreelanceDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FreelanceDto getFreelanceById(Long id, UserPrincipal currentUser) {
        Freelance f = freelanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Freelance non trouvé avec l'id : " + id));
        if (currentUser.getRole() == Role.FREELANCE
                && (f.getUtilisateur() == null || !f.getUtilisateur().getId().equals(currentUser.getId()))) {
            throw new AccessDeniedException("Vous ne pouvez consulter que votre propre fiche freelance");
        }
        return mapper.toFreelanceDto(f);
    }

    @Transactional(readOnly = true)
    public FreelanceDto getFreelanceByUserId(Long userId, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.FREELANCE && !currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Vous ne pouvez consulter que votre propre fiche freelance");
        }
        Freelance f = freelanceRepository.findByUtilisateurId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche freelance non trouvée pour cet utilisateur"));
        return mapper.toFreelanceDto(f);
    }

    @Transactional(readOnly = true)
    public FreelanceDto getFreelanceByEmail(String email, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.FREELANCE && !currentUser.getUsername().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("Vous ne pouvez consulter que votre propre fiche freelance");
        }
        Freelance f = freelanceRepository.findByUtilisateurEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche freelance non trouvée pour l'email : " + email));
        return mapper.toFreelanceDto(f);
    }

    @Transactional
    public FreelanceDto createFreelance(CreateFreelanceRequest req) {
        Utilisateur user = utilisateurRepository.findById(req.getUtilisateurId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id : " + req.getUtilisateurId()));

        if (freelanceRepository.findByUtilisateurId(user.getId()).isPresent()) {
            throw new BusinessException("Cet utilisateur possède déjà une fiche freelance");
        }

        user.setRole(Role.FREELANCE);
        utilisateurRepository.save(user);

        Freelance f = new Freelance(user, req.getTjm(), req.getSpecialite(), req.getStatut());
        Freelance saved = freelanceRepository.save(f);
        return mapper.toFreelanceDto(saved);
    }

    @Transactional
    public FreelanceDto updateFreelance(Long id, UpdateFreelanceRequest req, UserPrincipal currentUser) {
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.CHEF_PROJET) {
            throw new AccessDeniedException("Seuls les chefs de projet et administrateurs peuvent gérer les fiches freelance");
        }
        Freelance f = freelanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Freelance non trouvé avec l'id : " + id));

        if (currentUser.getRole() == Role.CHEF_PROJET
                && req.getTjm() != null
                && f.getTjm().compareTo(req.getTjm()) != 0) {
            throw new AccessDeniedException("Seul un administrateur peut modifier le TJM de référence");
        }
        if (currentUser.getRole() == Role.ADMIN) {
            f.setTjm(req.getTjm());
        }
        if (req.getSpecialite() != null) {
            f.setSpecialite(req.getSpecialite());
        }
        if (req.getStatut() != null) {
            f.setStatut(req.getStatut());
        }

        Freelance updated = freelanceRepository.save(f);
        return mapper.toFreelanceDto(updated);
    }
}
