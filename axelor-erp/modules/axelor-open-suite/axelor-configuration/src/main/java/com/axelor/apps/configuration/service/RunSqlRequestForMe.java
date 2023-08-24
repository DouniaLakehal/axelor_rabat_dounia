package com.axelor.apps.configuration.service;

import com.axelor.db.JPA;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.util.List;
import javax.persistence.Query;

public class RunSqlRequestForMe {

  public static BigDecimal runSqlRequest_Bigdecimal(String request) {

    Query query = JPA.em().createNativeQuery(request);
    if (query.getSingleResult() == null) {
      return BigDecimal.ZERO;
    }
    BigDecimal requestResult = (BigDecimal) query.getSingleResult();
    return requestResult;
  }

  public static Object runSqlRequest_Object(String request) {
    Query query = JPA.em().createNativeQuery(request);
    Object requestResult = (Object) query.getSingleResult();
    return requestResult;
  }

  public static List<Object> runSqlRequest_ObjectList(String request) {

    Query query = JPA.em().createNativeQuery(request);

    List<Object> requestResult = (List<Object>) query.getResultList();

    return requestResult;
  }

  @Transactional
  public static void runSqlRequest(String request) {
    Query query = JPA.em().createNativeQuery(request);
    query.executeUpdate();
  }
}
