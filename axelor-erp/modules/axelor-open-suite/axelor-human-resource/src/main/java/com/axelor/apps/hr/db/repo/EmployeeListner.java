package com.axelor.apps.hr.db.repo;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.service.EmployeeAdvanceService;
import com.google.inject.Inject;
import java.time.LocalDate;
import javax.persistence.PreUpdate;

public class EmployeeListner {

  @Inject EmployeeAdvanceService appservice;

 /* @PreUpdate
  private void onPostPersistOrUpdate(Employee emp) {
    if (emp != null && emp.getArchived() != null) {
      if (emp.getArchived()) {
        if (emp.getDateArchived() == null) {
          emp.setDateArchived(LocalDate.now());
        }
      } else {
        if (emp.getDateArchived() != null) {
          emp.setDateArchived(null);
        }
      }
    }

    System.out.println("employee etat_archived : " + emp.getArchived());
    System.out.println("employee date_archived : " + emp.getDateArchived());
  }*/

  /*@PreRemove
  private void onPreRemoveEmployerListner(Employee emp) {
      System.out.println("Delete Start ...");
      appservice.removeAugmentationByEmployer(emp.getId());
      System.out.println("Delete End ...");
  }*/

}
