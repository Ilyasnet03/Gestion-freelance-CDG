package com.cdg.freelance.service;

import com.cdg.freelance.dto.facturation.PvDto;
import com.cdg.freelance.entity.*;
import com.cdg.freelance.exception.BusinessException;
import com.cdg.freelance.exception.ResourceNotFoundException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.PaiementRepository;
import com.cdg.freelance.repository.LigneTimesheetRepository;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class PvService {

    private final PvReceptionRepository pvReceptionRepository;
    private final LigneTimesheetRepository ligneTimesheetRepository;
    private final PaiementRepository paiementRepository;
    private final EntityMapper mapper;

    @Value("${application.storage.pv-dir:./uploads/pv}")
    private String pvStorageDir;

    public PvService(PvReceptionRepository pvReceptionRepository,
                     LigneTimesheetRepository ligneTimesheetRepository,
                     PaiementRepository paiementRepository,
                     EntityMapper mapper) {
        this.pvReceptionRepository = pvReceptionRepository;
        this.ligneTimesheetRepository = ligneTimesheetRepository;
        this.paiementRepository = paiementRepository;
        this.mapper = mapper;
    }

    @Transactional
    public PvReception generatePv(Timesheet timesheet, BigDecimal tjmApplique) {
        if (pvReceptionRepository.existsByTimesheetId(timesheet.getId())) {
            return pvReceptionRepository.findByTimesheetId(timesheet.getId()).orElseThrow();
        }

        // RG7 : le montant du PV vaut le total d'heures divisé par 8, multiplié par le TJM
        List<LigneTimesheet> lignes = ligneTimesheetRepository.findAllByTimesheetId(timesheet.getId());
        if (lignes.isEmpty() && timesheet.getLignes() != null) {
            lignes = timesheet.getLignes();
        }
        BigDecimal totalHeures = lignes.stream()
                .map(LigneTimesheet::getHeures)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
            Map<Long, List<LigneTimesheet>> linesByTimesheet = new HashMap<>();
            List<Long> timesheetIds = pvs.stream()
                    .map(PvReception::getTimesheet)
                    .filter(Objects::nonNull)
                    .map(Timesheet::getId)
                    .collect(Collectors.toList());
            if (!timesheetIds.isEmpty()) {
                for (LigneTimesheet line : ligneTimesheetRepository.findAllByTimesheetIdIn(timesheetIds)) {
                    linesByTimesheet.computeIfAbsent(line.getTimesheet().getId(), key -> new java.util.ArrayList<>())
                            .add(line);
                }
            }
            pvs = pvs.stream()
                    .filter(pv -> isManagedByChef(pv, currentUser.getId(),
                            linesByTimesheet.getOrDefault(
                                    pv.getTimesheet() != null ? pv.getTimesheet().getId() : -1L,
                                    List.of())))
                    .collect(Collectors.toList());
        }

        Map<Long, Paiement> paymentsByPv = new HashMap<>();
        if (currentUser.getRole() != Role.FREELANCE) {
            for (Paiement paiement : paiementRepository.findAll()) {
                if (paiement.getPvReception() != null) {
                    paymentsByPv.put(paiement.getPvReception().getId(), paiement);
                }
            }
        }
        return pvs.stream().map(pv -> {
            PvDto dto = mapper.toPvDto(pv);
            if (currentUser.getRole() != Role.FREELANCE) {
                Paiement paiement = paymentsByPv.get(pv.getId());
                if (paiement != null) {
                    dto.setPaiementId(paiement.getId());
                    dto.setPaiementStatut(paiement.getStatut());
                }
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
                BigDecimal tjm = determineTjmApplique(pv.getTimesheet());
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

        if (currentUser.getRole() == Role.CHEF_PROJET) {
            List<LigneTimesheet> lignes = ligneTimesheetRepository.findAllByTimesheetId(timesheet.getId());
            if (!lignes.isEmpty()) {
                boolean belongsToChef = lignes.stream()
                    .map(LigneTimesheet::getTache)
                    .filter(java.util.Objects::nonNull)
                    .map(Tache::getProjet)
                    .filter(java.util.Objects::nonNull)
                    .map(Projet::getChefProjet)
                    .filter(java.util.Objects::nonNull)
                    .allMatch(chef -> chef.getId().equals(currentUser.getId()))
                    && lignes.stream().allMatch(line -> line.getTache() != null
                        && line.getTache().getProjet() != null
                        && line.getTache().getProjet().getChefProjet() != null);
                if (belongsToChef) {
                    return;
                }
            }
        }

        throw new AccessDeniedException("Ce PV ne relève pas de votre périmètre");
    }

    private boolean isManagedByChef(PvReception pv, Long chefProjetId) {
        Timesheet timesheet = pv.getTimesheet();
        List<LigneTimesheet> lignes = timesheet != null
                ? ligneTimesheetRepository.findAllByTimesheetId(timesheet.getId())
                : List.of();
        return isManagedByChef(pv, chefProjetId, lignes);
    }

    private boolean isManagedByChef(PvReception pv, Long chefProjetId, List<LigneTimesheet> lignes) {
        Timesheet timesheet = pv.getTimesheet();
        return timesheet != null
                && !lignes.isEmpty()
                && lignes.stream().allMatch(line -> line.getTache() != null
                    && line.getTache().getProjet() != null
                    && line.getTache().getProjet().getChefProjet() != null
                    && line.getTache().getProjet().getChefProjet().getId().equals(chefProjetId));
    }

    private BigDecimal determineTjmApplique(Timesheet timesheet) {
        if (timesheet == null) {
            return BigDecimal.ZERO;
        }
        List<LigneTimesheet> lignes = ligneTimesheetRepository.findAllByTimesheetId(timesheet.getId());
        for (LigneTimesheet ligne : lignes) {
            if (ligne.getTache() != null
                    && ligne.getTache().getAffectation() != null
                    && ligne.getTache().getAffectation().getTjmApplique() != null) {
                return ligne.getTache().getAffectation().getTjmApplique();
            }
        }
        return timesheet.getFreelance() != null ? timesheet.getFreelance().getTjm() : BigDecimal.ZERO;
    }

    private String generatePdfDocument(PvReception pv, Timesheet timesheet, BigDecimal tjmApplique) {
        try {
            File dir = new File(pvStorageDir);
            Files.createDirectories(dir.toPath());

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
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 48, 48, PdfBranding.MARGIN_TOP, PdfBranding.MARGIN_BOTTOM);
        PdfWriter writer = PdfWriter.getInstance(document, output);
        writer.setPageEvent(new PdfBranding("Procès-verbal de réception"));
        document.open();

        java.awt.Color navy = new java.awt.Color(18, 51, 84);
        java.awt.Color teal = new java.awt.Color(0, 145, 154);
        java.awt.Color lightBlue = new java.awt.Color(238, 246, 248);
        java.awt.Color grey = new java.awt.Color(102, 112, 123);
        java.awt.Color green = new java.awt.Color(22, 122, 91);

        Font sectionFont = new Font(Font.HELVETICA, 11, Font.BOLD, navy);
        Font labelFont = new Font(Font.HELVETICA, 8, Font.BOLD, grey);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.BOLD, navy);
        Font bodyFont = new Font(Font.HELVETICA, 9, Font.NORMAL, navy);
        Font amountFont = new Font(Font.HELVETICA, 16, Font.BOLD, green);

        PdfPTable header = new PdfPTable(new float[]{1f});
        header.setWidthPercentage(100);
        header.setSpacingAfter(16);
        PdfPCell banner = new PdfPCell();
        banner.setBorder(Rectangle.NO_BORDER);
        banner.setBackgroundColor(navy);
        banner.setPadding(16);
        Font bannerTitle = new Font(Font.HELVETICA, 20, Font.BOLD, java.awt.Color.WHITE);
        Font bannerSub = new Font(Font.HELVETICA, 10, Font.NORMAL, new java.awt.Color(190, 225, 228));
        banner.addElement(new Paragraph("PROCÈS-VERBAL DE RÉCEPTION", bannerTitle));
        banner.addElement(new Paragraph("N° " + numero + "  ·  Émis le " + dateGen, bannerSub));
        header.addCell(banner);
        document.add(header);

        PdfPTable meta = new PdfPTable(new float[]{1f, 1f});
        meta.setWidthPercentage(100);
        meta.setSpacingAfter(14);
        addMetaCell(meta, "NUMÉRO DU PV", numero, labelFont, valueFont);
        addMetaCell(meta, "DATE D’ÉMISSION", dateGen, labelFont, valueFont);
        addMetaCell(meta, "PÉRIODE", "Du " + dateDebut + " au " + dateFin, labelFont, valueFont);
        addMetaCell(meta, "PROJET", projetNom, labelFont, valueFont);
        document.add(meta);

        document.add(sectionTitle("1. PRESTATAIRE", sectionFont));
        PdfPTable freelance = new PdfPTable(new float[]{1.15f, 1.15f, 1f});
        freelance.setWidthPercentage(100);
        freelance.setSpacingAfter(12);
        addInfoCell(freelance, "Nom et prénom", freelanceNom, labelFont, bodyFont);
        addInfoCell(freelance, "E-mail", freelanceEmail, labelFont, bodyFont);
        addInfoCell(freelance, "Spécialité", specialite, labelFont, bodyFont);
        document.add(freelance);

        document.add(sectionTitle("2. VALIDATION", sectionFont));
        PdfPTable validation = new PdfPTable(new float[]{1f, 1f});
        validation.setWidthPercentage(100);
        validation.setSpacingAfter(16);
        addInfoCell(validation, "Chef de projet", cdpNom, labelFont, bodyFont);
        addInfoCell(validation, "Décision", "VALIDÉ — Bon pour réception", labelFont, bodyFont);
        document.add(validation);

        document.add(sectionTitle("3. DÉCOMPTE", sectionFont));
        PdfPTable amountTable = new PdfPTable(new float[]{2.2f, 1f, 1.2f, 1.4f});
        amountTable.setWidthPercentage(100);
        amountTable.setHeaderRows(1);
        addTableHeader(amountTable, "Rubrique", labelFont);
        addTableHeader(amountTable, "Quantité", labelFont);
        addTableHeader(amountTable, "TJM HT", labelFont);
        addTableHeader(amountTable, "Montant HT", labelFont);

        BigDecimal jours = totalHeures.divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP);
        addTableRow(amountTable, "Prestation validée", totalHeures + " h", tjm + " MAD", montant + " MAD",
                bodyFont);
        addTableRow(amountTable, "Équivalent ouvrable", jours + " jour(s)", "—", "—", bodyFont);
        document.add(amountTable);

        PdfPTable total = new PdfPTable(new float[]{2f, 1f});
        total.setWidthPercentage(100);
        total.setSpacingBefore(10);
        total.setSpacingAfter(18);
        PdfPCell totalLabel = new PdfPCell(new Phrase("MONTANT TOTAL À PAYER", labelFont));
        totalLabel.setBorder(Rectangle.BOX);
        totalLabel.setBorderColor(teal);
        totalLabel.setPadding(12);
        totalLabel.setBackgroundColor(lightBlue);
        total.addCell(totalLabel);

        PdfPCell totalValue = new PdfPCell(new Phrase(montant + " MAD HT", amountFont));
        totalValue.setBorder(Rectangle.BOX);
        totalValue.setBorderColor(teal);
        totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValue.setPadding(12);
        totalValue.setBackgroundColor(lightBlue);
        total.addCell(totalValue);
        document.add(total);

        PdfPTable footer = new PdfPTable(new float[]{1f, 1f});
        footer.setWidthPercentage(100);
        PdfPCell visa = new PdfPCell();
        visa.setBorder(Rectangle.NO_BORDER);
        visa.addElement(new Paragraph("Visa du chef de projet", labelFont));
        visa.addElement(new Paragraph(cdpNom, bodyFont));
        footer.addCell(visa);

        PdfPCell signature = new PdfPCell();
        signature.setBorder(Rectangle.NO_BORDER);
        signature.setHorizontalAlignment(Element.ALIGN_RIGHT);
        signature.addElement(new Paragraph("Document validé électroniquement", labelFont));
        signature.addElement(new Paragraph("Plateforme Gestion des Freelances", bodyFont));
        footer.addCell(signature);
        document.add(footer);

        document.close();
        return output.toByteArray();
    }

    private Paragraph sectionTitle(String title, Font font) {
        Paragraph paragraph = new Paragraph(title, font);
        paragraph.setSpacingBefore(4);
        paragraph.setSpacingAfter(7);
        return paragraph;
    }

    private void addMetaCell(PdfPTable table, String label, String value,
                             Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new java.awt.Color(218, 228, 232));
        cell.setPadding(9);
        cell.addElement(new Paragraph(label, labelFont));
        cell.addElement(new Paragraph(value, valueFont));
        table.addCell(cell);
    }

    private void addInfoCell(PdfPTable table, String label, String value,
                             Font labelFont, Font bodyFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new java.awt.Color(218, 228, 232));
        cell.setPadding(9);
        cell.addElement(new Paragraph(label, labelFont));
        cell.addElement(new Paragraph(value, bodyFont));
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String label, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(label, font));
        cell.setBackgroundColor(new java.awt.Color(18, 51, 84));
        cell.setBorderColor(new java.awt.Color(18, 51, 84));
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTableRow(PdfPTable table, String rubrique, String quantite,
                             String tjm, String montant, Font font) {
        table.addCell(tableCell(rubrique, font));
        table.addCell(tableCell(quantite, font));
        table.addCell(tableCell(tjm, font));
        table.addCell(tableCell(montant, font));
    }

    private PdfPCell tableCell(String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setBorderColor(new java.awt.Color(218, 228, 232));
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
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
