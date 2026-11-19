package com.cdg.freelance.service;

import com.cdg.freelance.dto.auth.LoginRequest;
import com.cdg.freelance.dto.auth.LoginResponse;
import com.cdg.freelance.dto.auth.UserDto;
import com.cdg.freelance.entity.Freelance;
import com.cdg.freelance.entity.Role;
import com.cdg.freelance.entity.Utilisateur;
import com.cdg.freelance.exception.BusinessException;
import com.cdg.freelance.exception.UnauthorizedException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.FreelanceRepository;
import com.cdg.freelance.repository.UtilisateurRepository;
import com.cdg.freelance.security.JwtService;
import com.cdg.freelance.security.UserPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final FreelanceRepository freelanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EntityMapper mapper;

    public AuthService(UtilisateurRepository utilisateurRepository,
                       FreelanceRepository freelanceRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EntityMapper mapper) {
        this.utilisateurRepository = utilisateurRepository;
        this.freelanceRepository = freelanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Utilisateur user = utilisateurRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Identifiants incorrects"));

        if (!Boolean.TRUE.equals(user.getActif())) {
            throw new UnauthorizedException("Ce compte utilisateur est désactivé");
        }

        if (!passwordEncoder.matches(request.getMotDePasse(), user.getMotDePasse())) {
            throw new UnauthorizedException("Identifiants incorrects");
        }

        Long freelanceId = null;
        if (user.getRole() == Role.FREELANCE) {
            freelanceId = freelanceRepository.findByUtilisateurId(user.getId())
                    .map(Freelance::getId)
                    .orElse(null);
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        UserDto userDto = mapper.toUserDto(user, freelanceId);

        return new LoginResponse(token, userDto);
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Utilisateur non authentifié");
        }
        Utilisateur user = utilisateurRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable"));

        Long freelanceId = null;
        if (user.getRole() == Role.FREELANCE) {
            freelanceId = freelanceRepository.findByUtilisateurId(user.getId())
                    .map(Freelance::getId)
                    .orElse(null);
        }

        return mapper.toUserDto(user, freelanceId);
    }
}
