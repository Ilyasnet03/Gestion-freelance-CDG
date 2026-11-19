package com.cdg.freelance.service;

import com.cdg.freelance.dto.facturation.PaiementDto;
import com.cdg.freelance.dto.facturation.UpdatePaiementRequest;
import com.cdg.freelance.entity.Paiement;
import com.cdg.freelance.entity.Role;
import com.cdg.freelance.exception.ResourceNotFoundException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.PaiementRepository;
import com.cdg.freelance.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final EntityMapper mapper;

    public PaiementService(PaiementRepository paiementRepository, EntityMapper mapper) {
        this.paiementRepository = paiementRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PaiementDto> listPaiements(UserPrincipal currentUser) {
        List<Paiement> paiements = paiementRepository.findAll();

        if (currentUser.getRole() == Role.FREELANCE) {
            paiements = paiements.stream()
                    .filter(p -> p.getPvReception() != null &&
                            p.getPvReception().getTimesheet() != null &&
                            p.getPvReception().getTimesheet().getFreelance().getUtilisateur().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
        }

        return paiements.stream().map(mapper::toPaiementDto).collect(Collectors.toList());
    }

    @Transactional
    public PaiementDto updatePaiement(Long id, UpdatePaiementRequest req) {
        Paiement p = paiementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement non trouvé avec l'id : " + id));

        p.setStatut(req.getStatut());
        p.setDatePaiement(req.getDatePaiement() != null ? req.getDatePaiement() : LocalDate.now());
        if (req.getReference() != null) {
            p.setReference(req.getReference().trim());
        }

        Paiement saved = paiementRepository.save(p);
        return mapper.toPaiementDto(saved);
    }
}
