package com.axelor.apps.hr.service.EtatsalaireService;

import com.axelor.apps.base.service.app.AppBaseServiceImpl;
import com.axelor.apps.configuration.db.*;
import com.axelor.apps.configuration.db.repo.*;
import com.axelor.apps.configuration.service.ServiceUtil;
import com.axelor.apps.hr.db.*;
import com.axelor.apps.hr.db.repo.*;
import com.axelor.apps.hr.service.EmployeeAdvanceService;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public
class EtatSalaireServiceImpl extends AppBaseServiceImpl implements EtatSalaireService {

    @Inject
    EmployeeAdvanceService employeeAdvanceService;
    @Inject
    EtatSalaireRepository etatSalaireRepository;
    @Inject
    private EmployeeRepository employeeRepository;
    @Inject
    private GestionSalaireRepository gestionSalaireRepository;
    @Inject
    private GestionCreditRepository gestionCreditRepository;
    @Inject
    private ServiceUtil serviceUtil;

    @Transactional
    public
    List<EtatSalaire> AddDataToEtatSalaire(int annee, int mois) {
        String dateDebut1 = "";
        List<EtatSalaire> ls = new ArrayList<>();
        if (mois >= 10) {
            dateDebut1 = String.valueOf(annee) + "-" + String.valueOf(mois) + "-" + "01";
        } else {
            dateDebut1 = String.valueOf(annee) + "-0" + String.valueOf(mois) + "-" + "01";
        }
        LocalDate d1 = LocalDate.parse(dateDebut1);
        LocalDate d2 = d1.plusMonths(1).minusDays(1);
        List<Employee> listEmployee =
                employeeRepository
                        .all()
                        .filter(
                                "self.daterecrutement<=?1 and (self.archived is null or self.archived = false)",
                                LocalDate.parse(dateDebut1))
                        .fetch();
        for (Employee employee : listEmployee) {
            EtatSalaire etatSalaire = new EtatSalaire();
            AddPersonalEmployeeInfo(employee, etatSalaire, annee, mois);
            ls.add(etatSalaireRepository.save(etatSalaire));
        }
        return ls;
    }

    @Transactional
    public
    void AddPersonalEmployeeInfo(
            Employee employee, EtatSalaire etatSalaire, int annee, int mois) {

        RappelEmploye rappelEmploye =
                Beans.get(RappelEmployeRepository.class)
                        .all()
                        .filter("self.employe.id=:id and self.mois=:mois and self.annee=:annee")
                        .bind("id", employee.getId())
                        .bind("mois", mois)
                        .bind("annee", annee)
                        .fetchOne();

        if (rappelEmploye != null) {
            etatSalaire.setRcar_rappel(
                    rappelEmploye.getRcar_rappel().multiply(BigDecimal.valueOf(rappelEmploye.getNbr_mois())));
            etatSalaire.setAmo_rappel(
                    rappelEmploye.getAmo_rappel().multiply(BigDecimal.valueOf(rappelEmploye.getNbr_mois())));
            etatSalaire.setComp_rappel(
                    rappelEmploye.getComp_rappel().multiply(BigDecimal.valueOf(rappelEmploye.getNbr_mois())));

            etatSalaire.setIr_rappel(
                    rappelEmploye.getIr_rappel().multiply(BigDecimal.valueOf(rappelEmploye.getNbr_mois())));

            etatSalaire.setNetAPayer_rappel(
                    rappelEmploye
                            .getNetAPayer_rappel()
                            .multiply(BigDecimal.valueOf(rappelEmploye.getNbr_mois())));
            etatSalaire.setIndemniteLogemBrut_rappel(
                    rappelEmploye
                            .getIndemniteLogemBrut_rappel()
                            .multiply(BigDecimal.valueOf(rappelEmploye.getNbr_mois())));
            etatSalaire.setIndemnitFonctionNet_rappel(
                    rappelEmploye
                            .getIndemnitFonctionNet_rappel()
                            .multiply(BigDecimal.valueOf(rappelEmploye.getNbr_mois())));
            etatSalaire.setIndemnitVoitureNet_rappel(
                    rappelEmploye
                            .getIndemnitVoitureNet_rappel()
                            .multiply(BigDecimal.valueOf(rappelEmploye.getNbr_mois())));
            etatSalaire.setIndemnitRepresentNet_rappel(
                    rappelEmploye
                            .getIndemnitRepresentNet_rappel()
                            .multiply(BigDecimal.valueOf(rappelEmploye.getNbr_mois())));
            etatSalaire.setTotalSalairMensuelBrutImposable_rappel(
                    rappelEmploye
                            .getTotalSalairMensuelBrutImposable_rappel()
                            .multiply(BigDecimal.valueOf(rappelEmploye.getNbr_mois())));
            etatSalaire.setMutuelleOmfamSm_rappel(
                    rappelEmploye
                            .getMutuelleOmfamSm_rappel()
                            .multiply(BigDecimal.valueOf(rappelEmploye.getNbr_mois())));
            etatSalaire.setMutuelleOmfamCaad_rappel(
                    rappelEmploye
                            .getMutuelleOmfamCaad_rappel()
                            .multiply(BigDecimal.valueOf(rappelEmploye.getNbr_mois())));
            etatSalaire.setMutuelleMgpapSM_rappel(
                    rappelEmploye
                            .getMutuelleMgpapSM_rappel()
                            .multiply(BigDecimal.valueOf(rappelEmploye.getNbr_mois())));
            etatSalaire.setMutuelleMgpapCcd_rappel(
                    rappelEmploye
                            .getMutuelleMgpapCcd_rappel()
                            .multiply(BigDecimal.valueOf(rappelEmploye.getNbr_mois())));
        }

        String dateDebut = "";
        if (mois >= 10) {
            dateDebut = String.valueOf(annee) + "-" + String.valueOf(mois) + "-" + "01";
        } else {
            dateDebut = String.valueOf(annee) + "-0" + String.valueOf(mois) + "-" + "01";
        }
        LocalDate date1 = LocalDate.parse(dateDebut);

        int count =
                employee.getEmploymentContractListNaissances().stream()
                        .filter(
                                naissances -> {
                                    if (naissances.getDateNaiss() != null) {
                                        return naissances.getDateNaiss().getMonthValue() == mois
                                                && naissances.getDateNaiss().getYear() == annee;
                                    }
                                    return false;
                                })
                        .collect(Collectors.toList())
                        .size();

        BigDecimal dd = BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(300));

        etatSalaire.setAllocation_rappel(dd);

        int count_ir =
                employee.getEmploymentContractListNaissances().stream()
                        .filter(
                                naissances -> {
                                    if (naissances.getDateNaiss() != null) {
                                        return naissances.getDateNaiss().getMonthValue() == mois
                                                && naissances.getDateNaiss().getYear() == annee;
                                    }
                                    return false;
                                })
                        .collect(Collectors.toList())
                        .size();

        boolean hasPrime = employeHasPrimeAtDate(employee.getId(), date1);
        BigDecimal prime_emp = BigDecimal.valueOf(0);
        if (hasPrime) {
            prime_emp = employee.getMontant_prime();
        }

        BigDecimal ir_dd = BigDecimal.valueOf(count_ir).multiply(BigDecimal.valueOf(30)).negate();

        etatSalaire.setIr_rappel(ir_dd);
        etatSalaire.setNetAPayer_rappel(ir_dd.abs().add(dd).add(prime_emp));

        Corps corps_old = null;
        Grade grade_old = null;
        Echelle echelle_old = null;
        Echelon echelon_old = null;
        etatSalaire.setNomPrenom(employee.getContactPartner().getSimpleFullName());
        etatSalaire.setEmployee(employee);
        String imatrString = employee.getImatriculation();
        //    int imatri=Integer.valueOf(employee.getImatriculation());
        etatSalaire.setImatriculation(
                employee.getMatriculationInterne() != null ? employee.getMatriculationInterne() : 0);
        // contractuuel vs titulaire
        if (employee.getTypePersonnel() == false || employee.getTypePersonnel() == null) {
            etatSalaire.setTitulaire(true);
        } else {
            etatSalaire.setTitulaire(false);
        }
        // situation familiale
        YearMonth yearMonthObject = YearMonth.of(annee, mois);
        int daysInMonth = yearMonthObject.lengthOfMonth();

        String dateFinMois = LocalDate.parse(dateDebut).plusMonths(1).minusDays(1).toString();
        Disposition disposition = getDispositionByEmployee(employee, dateFinMois.toString());
        if (disposition == null) {
            etatSalaire.setIsDisposition(false);
        } else {
            etatSalaire.setIsDisposition(true);
            etatSalaire.setDateDebutDisposition(disposition.getDateDebut());
            etatSalaire.setDateFinDisposition(disposition.getDateFin());
        }
        etatSalaire.setMois(mois);
        etatSalaire.setAnnee(annee);
        if (employee.getMaritalStatus() != null
                && !employee.getMaritalStatus().equals("")
                && employee.getMarriageDate() != null) {
            etatSalaire.setSituationfamiliale(getSitualtionAtDate(dateDebut, employee.getMarriageDate()));

        } else {
            etatSalaire.setSituationfamiliale("C");
        }
        // nombre des enfants
        etatSalaire.setNbEnfants(
                String.valueOf(employeeAdvanceService.getNombreEnfantAtDate_21(employee, dateDebut)));
        // echelle
        Augmentation a1 =
                employeeAdvanceService.getEchelleAtDate(dateDebut, dateDebut, employee, "Debut");
        if (a1 == null) {
            etatSalaire.setEchelle(employee.getEchelle().getName());
            etatSalaire.setEchelon(employee.getEchelon().getName());
            etatSalaire.setIndice(employee.getIndice());
            corps_old = employee.getCorps();
            grade_old = employee.getGrade();
            echelle_old = employee.getEchelle();
            echelon_old = employee.getEchelon();
            etatSalaire.setGradeName(employee.getGrade().getName());

        } else if (a1.getDateRappel() != null) {
            if (a1.getDateRappel().withDayOfMonth(1).isAfter(date1)) {
                etatSalaire.setEchelle(String.valueOf(a1.getEchelle_old().getName()));
                etatSalaire.setEchelon(String.valueOf(a1.getEchelon_old().getName()));
                etatSalaire.setIndice(String.valueOf(a1.getEchelon_old().getIndice()));
                etatSalaire.setGradeName(a1.getGrade_old().getName());
                corps_old = a1.getGrade_old().getCorps();
                grade_old = a1.getGrade_old();
                echelle_old = a1.getEchelle_old();
                echelon_old = a1.getEchelon_old();
            } else {
                etatSalaire.setEchelle(String.valueOf(a1.getEchelle().getName()));
                etatSalaire.setEchelon(String.valueOf(a1.getEchelon().getName()));
                etatSalaire.setIndice(String.valueOf(a1.getEchelon().getIndice()));
                etatSalaire.setGradeName(a1.getGrade().getName());
                corps_old = a1.getGrade().getCorps();
                grade_old = a1.getGrade();
                echelle_old = a1.getEchelle();
                echelon_old = a1.getEchelon();
            }
        } else {
            etatSalaire.setEchelle(String.valueOf(a1.getEchelle().getName()));
            etatSalaire.setEchelon(String.valueOf(a1.getEchelon().getName()));
            etatSalaire.setIndice(String.valueOf(a1.getEchelon().getIndice()));
            etatSalaire.setGradeName(a1.getGrade().getName());
            corps_old = a1.getGrade().getCorps();
            grade_old = a1.getGrade();
            echelle_old = a1.getEchelle();
            echelon_old = a1.getEchelon();
        }

        /*-----------------------------------------------------Salaires bruts et émoluments                    -----------*/
        // Traitement de base
        GestionSalaire gs_old =
                gestionSalaireRepository
                        .all()
                        .filter(
                                "self.corps=?1 and self.grade=?2 and self.echelon=?3 and self.indice=?4 and self.annee=?5",
                                corps_old,
                                grade_old,
                                echelon_old.getName(),
                                echelon_old.getIndice(),
                                date1.getYear())
                        .fetchOne();
        etatSalaire.setTraitementDebase(gs_old.getTraitementDeBase().setScale(2, RoundingMode.HALF_UP));
        // Indemnité de résidence
        BigDecimal pourc = new BigDecimal(1);
        if (employee.getZoneEmployee().getId() == 3) pourc = BigDecimal.valueOf(0.10);
        else if (employee.getZoneEmployee().getId() == 2) pourc = BigDecimal.valueOf(0.10);
        else if (employee.getZoneEmployee().getId() == 1) pourc = BigDecimal.valueOf(0.10);
        BigDecimal resid_old = gs_old.getTraitementDeBase().multiply(pourc);
        etatSalaire.setIndemniteDeResidence(
                (gs_old.getTraitementDeBase().multiply(pourc)).setScale(2, RoundingMode.HALF_UP));
        // IAS - Ind hier - Ind de tech
        etatSalaire.setIasIndhierIndDeTech(
                (gs_old.getIndemniteDeHierarchieAdministrative()).setScale(2, RoundingMode.HALF_UP));
        // Indemnité de sujétion
        etatSalaire.setIndemniteDeSujetion(
                (gs_old.getIndemniteDeSujetion()).setScale(2, RoundingMode.HALF_UP));
        // Indemnité d'encadrement
        etatSalaire.setIndemniteDencadrement(
                (gs_old.getIndemniteEncadrement()).setScale(2, RoundingMode.HALF_UP));
        // Ttaitement de base et indemnités statutaires
        etatSalaire.setTraitBaseIndemStatu(
                (gs_old
                        .getTraitementDeBase()
                        .add(gs_old.getTraitementDeBase().multiply(pourc))
                        .add(gs_old.getIndemniteDeHierarchieAdministrative())
                        .add(gs_old.getIndemniteDeSujetion())
                        .add(gs_old.getIndemniteEncadrement()))
                        .setScale(2, RoundingMode.HALF_UP));
        // Allocations familiales
        int child_old = employeeAdvanceService.getNombreEnfantAtDate_21(employee, dateDebut);
        int child_mt_old, child_mt_new;
        child_mt_old = child_mt_new = 0;
        if (child_old < 6) {
            if (child_old < 3) {
                child_mt_old = child_old * 300;
            } else {
                if (date1.getYear() > 2022) {
                    child_mt_old = 900 + ((child_old - 3) * 100);
                } else {
                    child_mt_old = 900 + ((child_old - 3) * 36);
                }
            }
        } else {
            child_mt_old = 1008;
        }
        etatSalaire.setAllocationfamil(
                BigDecimal.valueOf(Double.valueOf(String.valueOf(child_mt_old)))
                        .setScale(2, RoundingMode.HALF_UP));

        /*-----------------------------------------------------   Indemnités de responsabilité         -----------*/
        int nbrHp =
                employee.getResponsabiliteList() != null ? employee.getResponsabiliteList().size() : 0;

        BigDecimal indem_log_old = new BigDecimal(0);
        BigDecimal indem_log_new = new BigDecimal(0);
        BigDecimal indem_foncNet_old = new BigDecimal(0);
        BigDecimal indem_foncNet_new = new BigDecimal(0);
        BigDecimal indem_voitur_old = new BigDecimal(0);
        BigDecimal indem_voitur_new = new BigDecimal(0);
        BigDecimal indem_repres_old = new BigDecimal(0);
        BigDecimal indem_repres_new = new BigDecimal(0);
        HistoriqueResponsabilite hp_old = null;
        ResponsabiliteList newResp = null;
        if (nbrHp == 0) {
            setreponsabiliteNull(etatSalaire);
        } else {
            ResponsabiliteList oldResp =
                    Beans.get(EmployeeAdvanceService.class)
                            .getResponsabiliteAtDate(employee, LocalDate.parse(dateDebut));
            newResp =
                    Beans.get(EmployeeAdvanceService.class)
                            .getResponsabiliteAtDate(employee, LocalDate.parse(dateFinMois));
            if (oldResp != null
                    && newResp != null
                    && newResp.getResponsabilite_select().getId()
                    != oldResp.getResponsabilite_select().getId()
                    && newResp.getDate_debut().getDayOfMonth() != 1) {
                System.out.println("calculer");
                indem_log_new =
                        Beans.get(EmployeeAdvanceService.class)
                                .calcule_indemnite(1, oldResp, newResp, LocalDate.parse(dateFinMois));
                indem_foncNet_new =
                        Beans.get(EmployeeAdvanceService.class)
                                .calcule_indemnite(2, oldResp, newResp, LocalDate.parse(dateFinMois));
                indem_voitur_new =
                        Beans.get(EmployeeAdvanceService.class)
                                .calcule_indemnite(3, oldResp, newResp, LocalDate.parse(dateFinMois));
                indem_repres_new =
                        Beans.get(EmployeeAdvanceService.class)
                                .calcule_indemnite(4, oldResp, newResp, LocalDate.parse(dateFinMois));

            } else if (newResp != null
                    && LocalDate.parse(dateDebut).compareTo(newResp.getDate_debut()) >= 0) {
                indem_log_new = newResp.getResponsabilite_select().getIndemnitLogement();
                indem_foncNet_new = newResp.getResponsabilite_select().getIndemnitFonction();
                indem_voitur_new = newResp.getResponsabilite_select().getIndemnitVoiture();
                indem_repres_new = newResp.getResponsabilite_select().getIndemnitRepresentation();
            } else {
                if (newResp != null && newResp.getDate_debut().compareTo(LocalDate.parse(dateDebut)) >= 0) {
                    indem_log_new =
                            Beans.get(EmployeeAdvanceService.class)
                                    .calcule_indemnite_middelDate(1, newResp, LocalDate.parse(dateFinMois));
                    indem_foncNet_new =
                            Beans.get(EmployeeAdvanceService.class)
                                    .calcule_indemnite_middelDate(2, newResp, LocalDate.parse(dateFinMois));
                    indem_voitur_new =
                            Beans.get(EmployeeAdvanceService.class)
                                    .calcule_indemnite_middelDate(3, newResp, LocalDate.parse(dateFinMois));
                    indem_repres_new =
                            Beans.get(EmployeeAdvanceService.class)
                                    .calcule_indemnite_middelDate(4, newResp, LocalDate.parse(dateFinMois));
                }
            }

            if (newResp == null) {
                setreponsabiliteNull(etatSalaire);
            } else {
                // Indemnite de logement brute
                etatSalaire.setIndemniteLogemBrut(indem_log_new);
                // Indemnité de fonction nette
                etatSalaire.setIndemnitFonctionNet(indem_foncNet_new);
                // Indemnité de voiture nette
                etatSalaire.setIndemnitVoitureNet(indem_voitur_new);
                // Indemnité de représentation
                etatSalaire.setIndemnitRepresentNet(indem_repres_new);
            }
        }
        // TOTAL MENSUEL BRUT 617111
        BigDecimal totalBrut =
                etatSalaire.getTraitBaseIndemStatu()
                        .setScale(2, RoundingMode.HALF_UP);
        // Total salaire mensuel bru²t imposable
        //totalBrut = totalBrut.add(BigDecimal.valueOf(child_mt_old));
        etatSalaire.setTotalSalairMensuelBrutImposable(
                totalBrut.add(BigDecimal.valueOf(child_mt_old)).setScale(2, RoundingMode.HALF_UP));
        // RCAR
        RCAR rcar_old = serviceUtil.getRcarByYear(LocalDate.parse(dateDebut).getYear());
        BigDecimal rcar_mt_old = new BigDecimal("0").setScale(2, RoundingMode.HALF_UP);
        BigDecimal _prcent = new BigDecimal("100");

        if (!employee.getTypePersonnel()
                && getDispositionByEmployee(employee, dateFinMois.toString()) == null) {
            if (totalBrut
                    .multiply(rcar_old.getPors().divide(_prcent))
                    .compareTo(rcar_old.getMontant_max())
                    >= 0) {
                rcar_mt_old = rcar_old.getMontant_max().setScale(2, RoundingMode.HALF_UP);
                etatSalaire.setRcarRg(rcar_old.getMontant_max().setScale(2, RoundingMode.HALF_UP));
            } else {
                rcar_mt_old =
                        totalBrut
                                .multiply(rcar_old.getPors().divide(_prcent))
                                .setScale(2, RoundingMode.HALF_UP);
                etatSalaire.setRcarRg(
                        totalBrut
                                .multiply(rcar_old.getPors().divide(_prcent))
                                .setScale(2, RoundingMode.HALF_UP));
            }
        }
        etatSalaire.setRcarRg(rcar_mt_old.setScale(2, RoundingMode.HALF_UP));

        // RCAR complementaire
        RCARC comp_rcar_old =
                employeeAdvanceService.getComplementRcarByYear(LocalDate.parse(dateDebut).getYear());
        BigDecimal compRcar_mt_old = new BigDecimal(0).setScale(2, RoundingMode.HALF_UP);
        if (!employee.getTypePersonnel() && getDispositionByEmployee(employee, dateFinMois) == null) {
            if (totalBrut.compareTo(comp_rcar_old.getMontant()) > 0) {
                compRcar_mt_old =
                        totalBrut
                                .subtract(comp_rcar_old.getMontant())
                                .multiply(comp_rcar_old.getPors().divide(_prcent));
            }
        }

        etatSalaire.setRcarRComp(compRcar_mt_old.setScale(2, RoundingMode.HALF_UP));
        // RECORE (retraite comp)
        if (employee.getIsCotisation()) {
            etatSalaire.setRecore(employee.getMontantCotisation().setScale(2, RoundingMode.HALF_UP));

        } else {
            etatSalaire.setRecore(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        // AMO
        // MUTUELLE ( A.M.O )
        BigDecimal amo_mt_old = new BigDecimal(0).setScale(2, RoundingMode.HALF_UP);
        MUTUELLE sec1 = null;
        MUTUELLE ccd2 = null;
        MUTUELLE sec3 = null;
        MUTUELLE ccd4 = null;
        MUTUELLE amo = null;
        BigDecimal MgpapCcd = BigDecimal.ZERO;
        BigDecimal MgpapSM = BigDecimal.ZERO;
        BigDecimal OmfamCaad = BigDecimal.ZERO;
        BigDecimal OmfamSm = BigDecimal.ZERO;
        OrganismeMetuelle o = employee.getOrganismeMetuelle2();
        amo = employeeAdvanceService.getMutuelleById(1L);
        BigDecimal total_2_old =
                totalBrut.subtract(indem_log_old);
        if (getDispositionByEmployee(employee, dateFinMois.toString()) == null) {
            if (total_2_old.multiply(amo.getPors().divide(_prcent)).compareTo(amo.getMontant_max()) > 0) {
                amo_mt_old = amo.getMontant_max().setScale(2, RoundingMode.HALF_UP);
            } else if (total_2_old.multiply(amo.getPors().divide(_prcent)).compareTo(amo.getMontant_min())
                    < 0) {
                amo_mt_old = amo.getMontant_min().setScale(2, RoundingMode.HALF_UP);
            } else {
                amo_mt_old =
                        total_2_old.multiply(amo.getPors().divide(_prcent)).setScale(2, RoundingMode.HALF_UP);
            }
        }

        etatSalaire.setAmo(amo_mt_old.setScale(2, RoundingMode.HALF_UP));
        if (o.getId() == 1) {
            sec1 = employeeAdvanceService.getMutuelleById(3L);
            if (getDispositionByEmployee(employee, dateFinMois.toString()) == null) {
                MgpapCcd =
                        calculMGPAP(gs_old.getTraitementDeBase(), sec1).setScale(2, RoundingMode.HALF_UP);
            }

            etatSalaire.setMutuelleMgpapCcd(MgpapCcd.setScale(2, RoundingMode.HALF_UP));
            ccd2 = employeeAdvanceService.getMutuelleById(2L);
            if (getDispositionByEmployee(employee, dateFinMois.toString()) == null) {
                MgpapSM = calculMGPAP(total_2_old, ccd2).setScale(2, RoundingMode.HALF_UP);
            }
            etatSalaire.setMutuelleMgpapSM(MgpapSM.setScale(2, RoundingMode.HALF_UP));
            etatSalaire.setMutuelleOmfamCaad(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            etatSalaire.setMutuelleOmfamSm(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        } else if (o.getId() == 2) {
            etatSalaire.setMutuelleMgpapCcd(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)); // ////dounia
            etatSalaire.setMutuelleMgpapSM(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            sec3 = employeeAdvanceService.getMutuelleById(4L);
            if (getDispositionByEmployee(employee, dateFinMois.toString()) == null) {
                OmfamSm = calculMGPAP(total_2_old, sec3);
            }

            etatSalaire.setMutuelleOmfamSm(OmfamSm.setScale(2, RoundingMode.HALF_UP));
            ccd4 = employeeAdvanceService.getMutuelleById(5L);
            BigDecimal total_3_old = new BigDecimal(0);
            total_3_old = total_3_old.add(gs_old.getTraitementDeBase()).add(resid_old);
            if (getDispositionByEmployee(employee, dateFinMois.toString()) == null) {
                OmfamCaad =
                        total_3_old.multiply(ccd4.getPors().divide(_prcent)).setScale(2, RoundingMode.HALF_UP);
                OmfamCaad =
                        OmfamCaad.compareTo(ccd4.getMontant_max()) > 0
                                ? ccd4.getMontant_max().setScale(2, RoundingMode.HALF_UP)
                                : OmfamCaad;
            }
            if (employee.getId() == 167) {
                etatSalaire.setMutuelleOmfamCaad(BigDecimal.valueOf(40));
            } else {
                etatSalaire.setMutuelleOmfamCaad(OmfamCaad.setScale(2, RoundingMode.HALF_UP));
            }
        }

        // PRÊT AXACREDIT
        BigDecimal credit_mt_old = new BigDecimal(0);
        credit_mt_old = getcreditAtdate(employee, LocalDate.parse(dateDebut));
        if (employer_has_credit(employee)) {
            etatSalaire.setPretAxaCredit(credit_mt_old.setScale(2, RoundingMode.HALF_UP));
        } else {
            etatSalaire.setPretAxaCredit(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        // ir Rcar Complement 617131
        if (employee.getIsIrRcar_6171313() && newResp != null) {
            if (newResp.getResponsabilite_select().getIndemnitLogement().compareTo(BigDecimal.ZERO) > 0) {
                etatSalaire.setRcarComp617131(
                        employee.getMt_rcar_6171313().setScale(2, RoundingMode.HALF_UP));
                etatSalaire.setiR617131(employee.getMt_ir_6171313().setScale(2, RoundingMode.HALF_UP));
                BigDecimal tt =
                        indem_log_new
                                .subtract(etatSalaire.getiR617131())
                                .subtract(etatSalaire.getRcarComp617131());
                etatSalaire.setIndemniteLogementNet617131(tt.setScale(2, RoundingMode.HALF_UP));
            } else {
                etatSalaire.setRcarComp617131(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                etatSalaire.setiR617131(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                etatSalaire.setIndemniteLogementNet617131(
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }
        }
        // CSS
        BigDecimal css_old = BigDecimal.ZERO;
        if (annee == 2021) {
            if (totalBrut.compareTo(BigDecimal.valueOf(30000l)) >= 0) {
                if (totalBrut.compareTo(BigDecimal.valueOf(70000l)) > 0)
                    css_old = totalBrut.multiply(BigDecimal.valueOf(6l)).divide(_prcent);
                else if (totalBrut.compareTo(BigDecimal.valueOf(50000l)) > 0)
                    css_old = totalBrut.multiply(BigDecimal.valueOf(4l)).divide(_prcent);
                else css_old = totalBrut.multiply(BigDecimal.valueOf(2l)).divide(_prcent);
            }
            etatSalaire.setCss(css_old.setScale(2, RoundingMode.HALF_UP));
        } else {
            etatSalaire.setCss(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        // IMPOT SUR REVENU
        BigDecimal total_x_old = new BigDecimal(0);
        BigDecimal total_x_new = new BigDecimal(0);
        BigDecimal tva = new BigDecimal(20);
        BigDecimal mt1 = new BigDecimal(2500);

        if (date1.getYear() > 2022) {
            if (totalBrut.compareTo(new BigDecimal(6500)) > 0) {
                tva = new BigDecimal(25);
                mt1 = new BigDecimal(2916.66);
            } else {
                tva = new BigDecimal(35);
                mt1 = new BigDecimal(2500);
            }
        }

        total_x_old =
                totalBrut
                        .multiply(tva.divide(_prcent))
                        .setScale(2, RoundingMode.HALF_UP);

        if (total_x_old.compareTo(mt1) > 0) {
            total_x_old = mt1;
        }

        BigDecimal some1_old =
                totalBrut
                        .subtract(total_x_old)
                        .subtract(rcar_mt_old)
                        .subtract(compRcar_mt_old)
                        .subtract(amo_mt_old)
                        .subtract(MgpapCcd)
                        .subtract(MgpapSM)
                        .subtract(OmfamCaad)
                        .subtract(OmfamSm)
                        .subtract(
                                employee.getIsCotisation()
                                        ? employee.getMontantCotisation()
                                        : BigDecimal.valueOf(0))
                        .setScale(2, RoundingMode.HALF_UP);
        BigDecimal ir_old = new BigDecimal(0);
        BigDecimal mariage_old = new BigDecimal(0);
        if (employee.getMaritalStatus() != null && employee.getMaritalStatus().equals("1")) {
            String situation_old = getSitualtionAtDate(dateDebut, employee.getMarriageDate());
            mariage_old =
                    situation_old.equals("M") && employee.getContactPartner().getTitleSelect() == 1
                            ? BigDecimal.valueOf(1)
                            : BigDecimal.valueOf(0);

        } else {
            String situation_old = "C";
            mariage_old = BigDecimal.valueOf(0);
        }
        // return tranche by montant
        IR ir_tmp_old = serviceUtil.getIRByMontnat(some1_old);
        int child_old_n =
                employeeAdvanceService.getNombreEnfantAtDate_27(employee, LocalDate.parse(dateFinMois));
        // verifier si exonerer

        if (ir_tmp_old == null
                || ir_tmp_old.getExonerer()
                || getDispositionByEmployee(employee, dateFinMois.toString()) != null) {
            ir_old = BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP);
        } else {
            // si non exonerer calculer IR
            ir_old =
                    (some1_old
                            .multiply(ir_tmp_old.getPors().divide(_prcent))
                            .setScale(2, RoundingMode.HALF_UP))
                            .subtract(ir_tmp_old.getMontant_red())
                            .subtract(BigDecimal.valueOf(30).multiply(new BigDecimal(child_old_n)))
                            .subtract(BigDecimal.valueOf(30).multiply(mariage_old))
                            .setScale(2, RoundingMode.HALF_UP);
            etatSalaire.setCharge_familiale(
                    mariage_old.multiply(
                            BigDecimal.valueOf(30)
                                    .add(new BigDecimal(child_old_n).multiply(BigDecimal.valueOf(30)))));
        }

        if (employee.getId() == 133) {
            ir_old = BigDecimal.valueOf(1309);
        }

        ir_old =
                BigDecimal.ZERO.compareTo(ir_old) > 0
                        ? BigDecimal.ZERO.setScale(0, RoundingMode.HALF_UP)
                        : ir_old.setScale(0, RoundingMode.HALF_UP);
        etatSalaire.setiR(ir_old.setScale(0, RoundingMode.HALF_UP));


        Aos aos = employeeAdvanceService.employeHasAOSAtDate(employee.getId(), date1);
        BigDecimal mnt = BigDecimal.ZERO;
        AOS_config ao = Beans.get(AOS_configRepository.class).all()
                .filter("self.grade.id=:grade")
                .bind("grade", employee.getGrade().getId()).fetchOne();
        mnt = ((ao != null && employee.getIs_aos()) ? ao.getMontant() : BigDecimal.ZERO).add(
                aos != null ? aos.getRemboursement() : BigDecimal.ZERO);

        etatSalaire.setAos(mnt);

        //  TOT DES RETENUES
        BigDecimal tot_ret_old = new BigDecimal(0);
        tot_ret_old =
                rcar_mt_old
                        .setScale(2, RoundingMode.HALF_UP)
                        .add(compRcar_mt_old.setScale(2, RoundingMode.HALF_UP))
                        .add(amo_mt_old.setScale(2, RoundingMode.HALF_UP))
                        .add(MgpapCcd.setScale(2, RoundingMode.HALF_UP))
                        .add(MgpapSM.setScale(2, RoundingMode.HALF_UP))
                        .add(etatSalaire.getMutuelleOmfamCaad().setScale(2, RoundingMode.HALF_UP))
                        .add(OmfamSm.setScale(2, RoundingMode.HALF_UP))
                        .add(
                                getcreditAtdate(employee, LocalDate.parse(dateDebut))
                                        .setScale(2, RoundingMode.HALF_UP))
                        .add(
                                employee.getIsCotisation()
                                        ? employee.getMontantCotisation().setScale(2, RoundingMode.HALF_UP)
                                        : BigDecimal.valueOf(0))
                        .add(ir_old)
                        .add(css_old);
        etatSalaire.setTotalRetenue(tot_ret_old.setScale(2, RoundingMode.HALF_UP));
        // SALAIRE NET 617111
        BigDecimal salaireNet =
                totalBrut.subtract(tot_ret_old).add(BigDecimal.valueOf(Double.valueOf(child_mt_old)));
        etatSalaire.setSalaireNet617111(salaireNet.setScale(2, RoundingMode.HALF_UP));
        // NET A ORDONNANCER");
        BigDecimal net_ordo_old = new BigDecimal(0);
        if (!employee.getIsIrRcar_6171313()) {
            net_ordo_old =
                    totalBrut
                            .subtract(tot_ret_old)
                            .subtract(etatSalaire.getAos())
                            .add(BigDecimal.valueOf(child_mt_old))
                            .add(BigDecimal.ZERO)
                            .add(BigDecimal.ZERO)
                            .add(BigDecimal.ZERO);


            etatSalaire.setNetAPayer(net_ordo_old.setScale(2, RoundingMode.HALF_UP));
        } else {
            net_ordo_old =
                    totalBrut
                            .subtract(tot_ret_old)
                            .add(BigDecimal.valueOf(child_mt_old))
                            .add(newResp != null ? indem_foncNet_new : BigDecimal.ZERO)
                            .add(newResp != null ? indem_voitur_new : BigDecimal.ZERO)
                            .add(newResp != null ? indem_repres_new : BigDecimal.ZERO)
                            .add(newResp != null ? indem_log_new : BigDecimal.ZERO)
                            .subtract(etatSalaire.getAos());
            etatSalaire.setNetAPayer(net_ordo_old.setScale(2, RoundingMode.HALF_UP));
        }
    }

    public
    boolean employeHasPrimeAtDate(Long idemp, LocalDate d) {
        boolean res = false;
        Employee emp = employeeRepository.find(idemp);
        if (emp.getIs_prime()
                && emp.getDate_prime() != null
                && emp.getDate_prime().getMonthValue() == d.getMonthValue()
                && emp.getDate_prime().getYear() == d.getYear()) return true;
        return res;
    }

    private
    void setreponsabiliteNull(EtatSalaire etatSalaire) {
        // Indemnité de logement
        etatSalaire.setIndemniteLogemBrut(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        // Indemnité de fonction nette
        etatSalaire.setIndemnitFonctionNet((BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)));
        // Indemnité de voiture nette
        etatSalaire.setIndemnitVoitureNet(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        // Indemnité de représentation
        etatSalaire.setIndemnitRepresentNet(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    public
    int getNumberOfEtatSalaire(int annee, int mois) {
        return etatSalaireRepository
                .all()
                .filter("self.annee=?1 and self.mois=?2", annee, mois)
                .fetch()
                .size();
    }

    public
    String getSitualtionAtDate(String date1, LocalDate marriageDate) {
        String res = " - ";
        if (date1 != null && marriageDate != null) {
            LocalDate date = LocalDate.parse(date1);
            int x = marriageDate.compareTo(date);
            if (x < 0) res = "M";
            else res = "C";
        } else {
            res = "C";
        }
        return res;
    }

    public
    BigDecimal calculMGPAP(BigDecimal total_2_old, MUTUELLE sec) {
        BigDecimal _prcent = new BigDecimal("100");
        BigDecimal sec_mt_old = new BigDecimal(0);
        if (total_2_old.multiply(sec.getPors().divide(_prcent)).compareTo(sec.getMontant_max()) > 0) {
            sec_mt_old = sec.getMontant_max();
        } else if (total_2_old.multiply(sec.getPors().divide(_prcent)).compareTo(sec.getMontant_min())
                < 0) {
            sec_mt_old = sec.getMontant_min();
        } else {
            sec_mt_old = total_2_old.multiply(sec.getPors().divide(_prcent));
        }
        return sec_mt_old;
    }

    public
    boolean employer_has_credit(Employee employee) {
        boolean res = false;
        List<GestionCredit> ls =
                gestionCreditRepository.all().filter("self.employee=?1", employee).fetch();
        if (ls != null && ls.size() > 0) res = true;
        return res;
    }

    private
    BigDecimal getcreditAtdate(Employee employee, LocalDate date) {
        BigDecimal res = new BigDecimal(0);
        LocalDate start = date.withDayOfMonth(1);
        LocalDate end = date.withDayOfMonth(date.lengthOfMonth());
        if (employee.getEmploymentGestionCreditList() != null
                && employee.getEmploymentGestionCreditList().size() > 0) {
            List<GestionCredit> gestionCreditList =
                    gestionCreditRepository
                            .all()
                            .filter(
                                    "self.employee=?1 and ((date_debut<=?2 and date_fin>=?2) or (date_debut BETWEEN ?2 AND ?3) or (date_fin BETWEEN ?2 AND ?3))  ",
                                    employee,
                                    date,
                                    end)
                            .fetch();
            if ((gestionCreditList.size() != 0) && (gestionCreditList != null)) {
                for (GestionCredit cred : gestionCreditList) res = res.add(cred.getRemboursement());
            }
        }
        return res;
    }

    public
    String ConvertMoisToLettre(int mois) {
        String moisString = "";
        if (mois == 1) {
            moisString = "Janvier";
        } else if (mois == 2) {
            moisString = "Février";
        } else if (mois == 3) {
            moisString = "Mars";
        } else if (mois == 4) {
            moisString = "Avril";
        } else if (mois == 5) {
            moisString = "Mai";
        } else if (mois == 6) {
            moisString = "Juin";
        } else if (mois == 7) {
            moisString = "Juillet";
        } else if (mois == 8) {
            moisString = "Aout";
        } else if (mois == 9) {
            moisString = "Septembre";
        } else if (mois == 10) {
            moisString = "Octobre";
        } else if (mois == 11) {
            moisString = "Novembre";
        } else if (mois == 12) {
            moisString = "Décembre";
        }
        return moisString;
    }

    @Transactional
    public
    void supprimerTousEtatSalaire(int annee, int mois) {
        List<EtatSalaire> listEtatsalaire =
                etatSalaireRepository.all().filter("self.annee=?1 and self.mois=?2", annee, mois).fetch();
        for (EtatSalaire e : listEtatsalaire) {
            etatSalaireRepository.remove(e);
        }
    }

    public
    Disposition getDispositionByEmployee(Employee employee, String dateEtat) {
        LocalDate date = LocalDate.parse(dateEtat);
        if (employee.getDisposition() != null && employee.getDisposition().size() > 0) {
            List<Disposition> ls = employee.getDisposition();
            for (Disposition ds : ls) {
                if (date.compareTo(ds.getDateDebut()) >= 0 && ds.getDateFin().compareTo(date) >= 0) {
                    return ds;
                }
            }
        }
        return null;
    }
}
