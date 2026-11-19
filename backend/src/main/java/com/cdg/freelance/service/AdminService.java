package com.cdg.freelance.service;

import com.cdg.freelance.dto.admin.CreateUserRequest;
import com.cdg.freelance.dto.admin.ParametreDto;
import com.cdg.freelance.dto.admin.TypeProjetDto;
import com.cdg.freelance.dto.admin.UpdateUserRequest;
import com.cdg.freelance.dto.auth.UserDto;
import com.cdg.freelance.entity.*;
import com.cdg.freelance.exception.BusinessException;
import com.cdg.freelance.exception.ResourceNotFoundException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.FreelanceRepository;
import com.cdg.freelance.repository.ParametreRepository;
import com.cdg.freelance.repository.TypeProjetRepository;
import com.cdg.freelance.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UtilisateurRepository utilisateurRepository;
    private final FreelanceRepository freelanceRepository;
    private final ParametreRepository parametreRepository;
    private final TypeProjetRepository typeProjetRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityMapper mapper;

    public AdminService(UtilisateurRepository utilisateurRepository,
                        FreelanceRepository freelanceRepository,
                        ParametreRepository parametreRepository,
                        TypeProjetRepository typeProjetRepository,
                        PasswordEncoder passwordEncoder,
                        EntityMapper mapper) {
        this.utilisateurRepository = utilisateurRepository;
        this.freelanceRepository = freelanceRepository;
        this.parametreRepository = parametreRepository;
        this.typeProjetRepository = typeProjetRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<UserDto> listUsers() {
        return utilisateurRepository.findAll().stream().map(u -> {
            Long fId = null;
            if (u.getRole() == Role.FREELANCE) {
                fId = freelanceRepository.findByUtilisateurId(u.getId()).map(Freelance::getId).orElse(null);
            }
            return mapper.toUserDto(u, fId);
        }).collect(Collectors.toList());
    }

    @Transactional
    public UserDto createUser(CreateUserRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (utilisateurRepository.existsByEmail(email)) {
            throw new BusinessException("Un utilisateur avec cet email existe déjà : " + email);
        }

        Utilisateur u = new Utilisateur();
        u.setNom(req.getNom().trim());
        u.setPrenom(req.getPrenom().trim());
        u.setEmail(email);
        u.setMotDePasse(passwordEncoder.encode(req.getMotDePasse()));
        u.setRole(req.getRole());
        u.setActif(true);

        Utilisateur saved = utilisateurRepository.save(u);

        Long freelanceId = null;
        if (saved.getRole() == Role.FREELANCE) {
            BigDecimal tjm = req.getTjm();
            if (tjm == null || tjm.compareTo(BigDecimal.ZERO) <= 0) {
                // Get default TJM from parameters or fallback 4500
                String defTjm = parametreRepository.findByCle("TJM_DEFAUT").map(Parametre::getValeur).orElse("4500");
                tjm = new BigDecimal(defTjm);
            }
            Freelance f = new Freelance(saved, tjm, req.getSpecialite(), "ACTIF");
            Freelance savedFreelance = freelanceRepository.save(f);
            freelanceId = savedFreelance.getId();
        }

        return mapper.toUserDto(saved, freelanceId);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest req) {
        Utilisateur u = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id : " + id));

        String email = req.getEmail().trim().toLowerCase();
        if (!u.getEmail().equalsIgnoreCase(email) && utilisateurRepository.existsByEmail(email)) {
            throw new BusinessException("Un utilisateur avec cet email existe déjà : " + email);
        }

        u.setNom(req.getNom().trim());
        u.setPrenom(req.getPrenom().trim());
        u.setEmail(email);
        u.setRole(req.getRole());
        if (req.getActif() != null) {
            u.setActif(req.getActif());
        }
        if (req.getMotDePasse() != null && !req.getMotDePasse().isBlank()) {
            u.setMotDePasse(passwordEncoder.encode(req.getMotDePasse()));
        }

        Utilisateur updated = utilisateurRepository.save(u);

        Long freelanceId = null;
        if (updated.getRole() == Role.FREELANCE) {
            freelanceId = freelanceRepository.findByUtilisateurId(updated.getId())
                    .map(Freelance::getId)
                    .orElseGet(() -> {
                        String defTjm = parametreRepository.findByCle("TJM_DEFAUT").map(Parametre::getValeur).orElse("4500");
                        Freelance newF = new Freelance(updated, new BigDecimal(defTjm), "Développeur", "ACTIF");
                        return freelanceRepository.save(newF).getId();
                    });
        }

        return mapper.toUserDto(updated, freelanceId);
    }

    @Transactional
    public UserDto toggleUserStatus(Long id) {
        Utilisateur u = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id : " + id));
        u.setActif(!Boolean.TRUE.equals(u.getActif()));
        Utilisateur updated = utilisateurRepository.save(u);
        Long fId = freelanceRepository.findByUtilisateurId(u.getId()).map(Freelance::getId).orElse(null);
        return mapper.toUserDto(updated, fId);
    }

    @Transactional(readOnly = true)
    public List<ParametreDto> listParametres() {
        return parametreRepository.findAll().stream().map(mapper::toParametreDto).collect(Collectors.toList());
    }

    @Transactional
    public ParametreDto updateParametre(String cle, String valeur) {
        Parametre p = parametreRepository.findByCle(cle)
                .orElse(new Parametre(cle, valeur));
        p.setValeur(valeur);
        return mapper.toParametreDto(parametreRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<TypeProjetDto> listTypesProjet() {
        return typeProjetRepository.findAll().stream().map(mapper::toTypeProjetDto).collect(Collectors.toList());
    }

    @Transactional
    public TypeProjetDto createTypeProjet(String libelle) {
        if (typeProjetRepository.existsByLibelle(libelle)) {
            throw new BusinessException("Ce type de projet existe déjà : " + libelle);
        }
        TypeProjet tp = new TypeProjet(libelle);
        return mapper.toTypeProjetDto(typeProjetRepository.save(tp));
    }
}
