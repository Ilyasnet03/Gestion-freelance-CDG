package com.cdg.freelance.service;

import com.cdg.freelance.dto.facturation.PvDto;
import com.cdg.freelance.entity.*;
import com.cdg.freelance.exception.BusinessException;
import com.cdg.freelance.exception.ResourceNotFoundException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.PaiementRepository;
import com.cdg.freelance.repository.PvReceptionRepository;
import com.cdg.freelance.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PvService {

    private final PvReceptionRepository pvReceptionRepository;
    private final PaiementRepository paiementRepository;
    private final EntityMapper mapper;

    @Value("${application.storage.pv-dir:./uploads/pv}")
    private String pvStorageDir;

    public PvService(PvReceptionRepository pvReceptionRepository,
                     PaiementRepository paiementRepository,
                     EntityMapper mapper) {
        this.pvReceptionRepository = pvReceptionRepository;
        this.paiementRepository = paiementRepository;
        this.mapper = mapper;
    }

    @Transactional
    public PvReception generatePv(Timesheet timesheet, BigDecimal tjmApplique) {
        if (pvReceptionRepository.existsByTimesheetId(timesheet.getId())) {
            return pvReceptionRepository.findByTimesheetId(timesheet.getId()).orElseThrow();
        }

        // RG7 : le montant du PV vaut le total d'heures divisé par 8, multiplié par le TJM
        BigDecimal totalHeures = BigDecimal.ZERO;
        if (timesheet.getLignes() != null) {
            totalHeures = timesheet.getLignes().stream()
                    .map(LigneTimesheet::getHeures)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal jours = totalHeures.divide(BigDecimal.valueOf(8), 4, RoundingMode.HALF_UP);
        BigDecimal montant = jours.multiply(tjmApplique).setScale(2, RoundingMode.HALF_UP);

        String annee = String.valueOf(timesheet.getSemaineDebut().getYear());
        String numero = String.format("PV-%s-CDG-%05d", annee, timesheet.getId());

        PvReception pv = new PvReception();
        pv.setTimesheet(timesheet);
        pv.setNumero(numero);
        pv.setTotalHeures(totalHeures);
        pv.setMontant(montant);
        pv.setDateGeneration(LocalDateTime.now());

        // Generate PDF
        String pdfPath = generatePdfDocument(pv, timesheet, tjmApplique);
        pv.setCheminPdf(pdfPath);

        PvReception saved = pvReceptionRepository.save(pv);

        // Automaticaly create payment in 'EN_ATTENTE' status (as requested in functional specs)
        Paiement paiement = new Paiement();
        paiement.setPvReception(saved);
        paiement.setStatut(PaiementStatut.EN_ATTENTE);
        paiementRepository.save(paiement);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<PvDto> listPvs(UserPrincipal currentUser) {
        List<PvReception> pvs = pvReceptionRepository.findAll();

        if (currentUser.getRole() == Role.FREELANCE) {
            pvs = pvs.stream()
                    .filter(pv -> pv.getTimesheet() != null &&
                            pv.getTimesheet().getFreelance() != null &&
                            pv.getTimesheet().getFreelance().getUtilisateur().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
        } else if (currentUser.getRole() == Role.CHEF_PROJET) {
            pvs = pvs.stream()
                    .filter(pv -> isManagedByChef(pv, currentUser.getId()))
                    .collect(Collectors.toList());
        }

        return pvs.stream().map(pv -> {
            PvDto dto = mapper.toPvDto(pv);
            if (currentUser.getRole() != Role.FREELANCE) {
                paiementRepository.findByPvReceptionId(pv.getId()).ifPresent(p -> {
                    dto.setPaiementId(p.getId());
                    dto.setPaiementStatut(p.getStatut());
                });
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PvDto getPvById(Long id, UserPrincipal currentUser) {
        PvReception pv = pvReceptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PV de réception non trouvé avec l'id : " + id));
        ensureUserCanAccessPv(pv, currentUser);

        PvDto dto = mapper.toPvDto(pv);
        if (currentUser.getRole() != Role.FREELANCE) {
            paiementRepository.findByPvReceptionId(pv.getId()).ifPresent(p -> {
                dto.setPaiementId(p.getId());
                dto.setPaiementStatut(p.getStatut());
            });
        }
        return dto;
    }

    @Transactional
    public byte[] getPdfBytes(Long pvId, UserPrincipal currentUser) {
        PvReception pv = pvReceptionRepository.findById(pvId)
                .orElseThrow(() -> new ResourceNotFoundException("PV de réception non trouvé"));
        ensureUserCanAccessPv(pv, currentUser);

        if (pv.getCheminPdf() == null) {
            throw new ResourceNotFoundException("Fichier PDF non disponible pour ce PV");
        }

        try {
            Path path = Paths.get(pv.getCheminPdf());
            if (!Files.exists(path)) {
                // Regenerate if lost
                BigDecimal tjm = pv.getTimesheet().getFreelance().getTjm();
                String newPath = generatePdfDocument(pv, pv.getTimesheet(), tjm);
                pv.setCheminPdf(newPath);
                pvReceptionRepository.save(pv);
                path = Paths.get(newPath);
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new BusinessException("Erreur lors de la lecture du fichier PDF : " + e.getMessage());
        }
    }

    private void ensureUserCanAccessPv(PvReception pv, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        Timesheet timesheet = pv.getTimesheet();
        if (timesheet == null) {
            throw new AccessDeniedException("Ce PV n'est associé à aucune feuille de temps accessible");
        }

        if (currentUser.getRole() == Role.FREELANCE) {
            if (timesheet.getFreelance() != null
                    && timesheet.getFreelance().getUtilisateur() != null
                    && timesheet.getFreelance().getUtilisateur().getId().equals(currentUser.getId())) {
                return;
            }
            throw new AccessDeniedException("Ce PV ne relève pas de vos missions");
        }

        if (currentUser.getRole() == Role.CHEF_PROJET
                && timesheet.getLignes() != null
                && !timesheet.getLignes().isEmpty()) {
            boolean belongsToChef = timesheet.getLignes().stream()
                    .map(LigneTimesheet::getTache)
                    .filter(java.util.Objects::nonNull)
                    .map(Tache::getProjet)
                    .filter(java.util.Objects::nonNull)
                    .map(Projet::getChefProjet)
                    .filter(java.util.Objects::nonNull)
                    .allMatch(chef -> chef.getId().equals(currentUser.getId()))
                    && timesheet.getLignes().stream().allMatch(line -> line.getTache() != null
                        && line.getTache().getProjet() != null
                        && line.getTache().getProjet().getChefProjet() != null);
            if (belongsToChef) {
                return;
            }
        }

        throw new AccessDeniedException("Ce PV ne relève pas de votre périmètre");
    }

    private boolean isManagedByChef(PvReception pv, Long chefProjetId) {
        Timesheet timesheet = pv.getTimesheet();
        return timesheet != null
                && timesheet.getLignes() != null
                && !timesheet.getLignes().isEmpty()
                && timesheet.getLignes().stream().allMatch(line -> line.getTache() != null
                    && line.getTache().getProjet() != null
                    && line.getTache().getProjet().getChefProjet() != null
                    && line.getTache().getProjet().getChefProjet().getId().equals(chefProjetId));
    }

    private String generatePdfDocument(PvReception pv, Timesheet timesheet, BigDecimal tjmApplique) {
        try {
            File dir = new File(pvStorageDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename = pv.getNumero() + ".pdf";
            File file = new File(dir, filename);

            Freelance f = timesheet.getFreelance();
            String freelanceNom = f != null && f.getUtilisateur() != null ?
                    f.getUtilisateur().getPrenom() + " " + f.getUtilisateur().getNom() : "Freelance";
            String freelanceEmail = f != null && f.getUtilisateur() != null ? f.getUtilisateur().getEmail() : "";
            String specialite = f != null ? f.getSpecialite() : "Ingénieur";

            String projetNom = "Projet CDG";
            String cdpNom = timesheet.getValidePar() != null ?
                    timesheet.getValidePar().getPrenom() + " " + timesheet.getValidePar().getNom() : "Chef de projet CDG";

            if (timesheet.getLignes() != null && !timesheet.getLignes().isEmpty()) {
                Tache t = timesheet.getLignes().get(0).getTache();
                if (t != null && t.getProjet() != null) {
                    projetNom = t.getProjet().getNom() + " (" + t.getProjet().getCode() + ")";
                }
            }

            byte[] pdfContent = createCleanPdf(
                    pv.getNumero(),
                    pv.getDateGeneration().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    timesheet.getSemaineDebut().toString(),
                    timesheet.getSemaineDebut().plusDays(6).toString(),
                    freelanceNom,
                    freelanceEmail,
                    specialite,
                    projetNom,
                    cdpNom,
                    pv.getTotalHeures(),
                    tjmApplique,
                    pv.getMontant()
            );

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(pdfContent);
            }

            return file.getAbsolutePath();
        } catch (Exception e) {
            throw new BusinessException("Impossible de générer le document PDF du PV : " + e.getMessage());
        }
    }

    private byte[] createCleanPdf(String numero, String dateGen, String dateDebut, String dateFin,
                                  String freelanceNom, String freelanceEmail, String specialite,
                                  String projetNom, String cdpNom,
                                  BigDecimal totalHeures, BigDecimal tjm, BigDecimal montant) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        StringBuilder text = new StringBuilder();
        text.append("BT\n");
        text.append("/F1 18 Tf\n");
        text.append("50 780 Td\n");
        text.append("(GROUPE CAISSE DE DEPOT ET DE GESTION - CDG BEP) Tj\n");

        text.append("/F1 14 Tf\n");
        text.append("0 -30 Td\n");
        text.append("(PROCES-VERBAL DE RECEPTION DES PRESTATIONS) Tj\n");

        text.append("/F2 11 Tf\n");
        text.append("0 -25 Td\n");
        text.append("(Numero du PV : ").append(sanitizePdfText(numero)).append(") Tj\n");
        text.append("0 -18 Td\n");
        text.append("(Date d'emission : ").append(sanitizePdfText(dateGen)).append(") Tj\n");
        text.append("0 -18 Td\n");
        text.append("(Periode : du ").append(sanitizePdfText(dateDebut)).append(" au ").append(sanitizePdfText(dateFin)).append(") Tj\n");

        text.append("0 -28 Td\n");
        text.append("/F1 12 Tf\n");
        text.append("(1. INFORMATIONS DU PRESTATAIRE FREELANCE) Tj\n");
        text.append("/F2 10 Tf\n");
        text.append("0 -18 Td\n");
        text.append("(Nom et Prenom : ").append(sanitizePdfText(freelanceNom)).append(") Tj\n");
        text.append("0 -16 Td\n");
        text.append("(Email : ").append(sanitizePdfText(freelanceEmail)).append(") Tj\n");
        text.append("0 -16 Td\n");
        text.append("(Specialite : ").append(sanitizePdfText(specialite)).append(") Tj\n");

        text.append("0 -26 Td\n");
        text.append("/F1 12 Tf\n");
        text.append("(2. MISSION ET PROJET CDG BEP) Tj\n");
        text.append("/F2 10 Tf\n");
        text.append("0 -18 Td\n");
        text.append("(Projet : ").append(sanitizePdfText(projetNom)).append(") Tj\n");
        text.append("0 -16 Td\n");
        text.append("(Valide par le Chef de Projet : ").append(sanitizePdfText(cdpNom)).append(") Tj\n");

        text.append("0 -26 Td\n");
        text.append("/F1 12 Tf\n");
        text.append("(3. DECOMPTE DES HEURES ET REGLEMENT) Tj\n");
        text.append("/F2 10 Tf\n");
        text.append("0 -18 Td\n");
        text.append("(Volume d'heures validees : ").append(totalHeures).append(" heures) Tj\n");
        text.append("0 -16 Td\n");
        text.append("(Equivalent jours ouvrables (Base 8h/j) : ").append(totalHeures.divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP)).append(" j) Tj\n");
        text.append("0 -16 Td\n");
        text.append("(Tarif Journalier Moyen applique (TJM) : ").append(tjm).append(" MAD HT) Tj\n");
        text.append("0 -20 Td\n");
        text.append("/F1 13 Tf\n");
        text.append("(MONTANT TOTAL A PAYER : ").append(montant).append(" MAD HT) Tj\n");

        text.append("/F2 10 Tf\n");
        text.append("0 -40 Td\n");
        text.append("(Visa CDG BEP : Bon pour acceptation et mise en paiement comptable) Tj\n");
        text.append("0 -20 Td\n");
        text.append("(Signature electronique validee sur la Plateforme de Gestion des Freelances) Tj\n");

        text.append("ET\n");

        String streamContent = text.toString();
        byte[] streamBytes = streamContent.getBytes(StandardCharsets.ISO_8859_1);

        // Standard minimal PDF 1.4 output
        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        pdf.append("%\u00E2\u00E3\u00CF\u00D3\n");

        int offset1 = pdf.length();
        pdf.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        int offset2 = pdf.length();
        pdf.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

        int offset3 = pdf.length();
        pdf.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\nendobj\n");

        int offset4 = pdf.length();
        pdf.append("4 0 obj\n<< /Length ").append(streamBytes.length).append(" >>\nstream\n");
        baos.write(pdf.toString().getBytes(StandardCharsets.ISO_8859_1));
        baos.write(streamBytes);

        StringBuilder pdfEnd = new StringBuilder();
        pdfEnd.append("\nendstream\nendobj\n");

        int offset5 = baos.size() + pdfEnd.length() - ("\nendstream\nendobj\n".length());
        pdfEnd.append("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n");

        int offset6 = baos.size() + pdfEnd.length();
        pdfEnd.append("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

        int xrefOffset = baos.size() + pdfEnd.length();
        pdfEnd.append("xref\n");
        pdfEnd.append("0 7\n");
        pdfEnd.append("0000000000 65535 f \n");
        pdfEnd.append(String.format("%010d 00000 n \n", offset1));
        pdfEnd.append(String.format("%010d 00000 n \n", offset2));
        pdfEnd.append(String.format("%010d 00000 n \n", offset3));
        pdfEnd.append(String.format("%010d 00000 n \n", offset4));
        pdfEnd.append(String.format("%010d 00000 n \n", offset5));
        pdfEnd.append(String.format("%010d 00000 n \n", offset6));
        pdfEnd.append("trailer\n<< /Size 7 /Root 1 0 R >>\n");
        pdfEnd.append("startxref\n").append(xrefOffset).append("\n%%EOF\n");

        baos.write(pdfEnd.toString().getBytes(StandardCharsets.ISO_8859_1));
        return baos.toByteArray();
    }

    private String sanitizePdfText(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("à", "a")
                .replace("ç", "c")
                .replace("ô", "o")
                .replace("î", "i");
    }
}
