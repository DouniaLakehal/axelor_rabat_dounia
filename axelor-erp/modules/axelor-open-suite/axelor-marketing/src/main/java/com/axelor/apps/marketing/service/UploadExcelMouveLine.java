package com.axelor.apps.marketing.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UploadExcelMouveLine {
  String code_journale;
  String numFacture;
  LocalDate date;
  String code_compte;
  String name_tiers;
  String libelle;
  BigDecimal debit;
  BigDecimal credit;

  public UploadExcelMouveLine() {
    this.code_journale = "";
    this.numFacture = "";
    LocalDate date = LocalDate.now();
    this.code_compte = "";
    this.name_tiers = "";
    this.libelle = "";
    BigDecimal debit = BigDecimal.ZERO;
    BigDecimal credit = BigDecimal.ZERO;
  }

  public UploadExcelMouveLine(
      String code_journale,
      String numFacture,
      LocalDate date,
      String code_compte,
      String name_tiers,
      String libelle,
      BigDecimal debit,
      BigDecimal credit) {
    this.code_journale = code_journale;
    this.numFacture = numFacture;
    this.date = date;
    this.code_compte = code_compte;
    this.name_tiers = name_tiers;
    this.libelle = libelle;
    this.debit = debit;
    this.credit = credit;
  }

  public String getCode_journale() {
    return code_journale;
  }

  public void setCode_journale(String code_journale) {
    this.code_journale = code_journale;
  }

  public String getNumFacture() {
    return numFacture;
  }

  public void setNumFacture(String numFacture) {
    this.numFacture = numFacture;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public String getCode_compte() {
    return code_compte;
  }

  public void setCode_compte(String code_compte) {
    this.code_compte = code_compte;
  }

  public String getName_tiers() {
    return name_tiers;
  }

  public void setName_tiers(String name_tiers) {
    this.name_tiers = name_tiers;
  }

  public String getLibelle() {
    return libelle;
  }

  public void setLibelle(String libelle) {
    this.libelle = libelle;
  }

  public BigDecimal getDebit() {
    return debit;
  }

  public void setDebit(BigDecimal debit) {
    this.debit = debit;
  }

  public BigDecimal getCredit() {
    return credit;
  }

  public void setCredit(BigDecimal credit) {
    this.credit = credit;
  }
}
