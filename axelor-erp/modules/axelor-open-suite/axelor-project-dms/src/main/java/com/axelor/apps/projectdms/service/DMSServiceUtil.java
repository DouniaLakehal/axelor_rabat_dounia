package com.axelor.apps.projectdms.service;

import com.axelor.apps.configuration.service.RunSqlRequestForMe;
import com.axelor.auth.db.User;
import java.util.List;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class DMSServiceUtil {

  public static void send(String to, String sub, String msg) {
    // Get properties object
    Properties props = new Properties();
    props.put("mail.smtp.user", "no.reply.ccisbk@gmail.com");
    props.put("mail.smtp.password", "azerty@@2022");
    props.put("mail.smtp.host", "smtp.gmail.com");
    props.put("mail.smtp.port", "465");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.ssl.enable", "true");
    // get Session
    Session session =
        Session.getDefaultInstance(
            props,
            new javax.mail.Authenticator() {
              protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("no.reply.ccisbk@gmail.com", "azerty@@2022");
              }
            });
    // compose message
    try {
      MimeMessage message = new MimeMessage(session);
      message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
      message.setSubject(sub);
      message.setText(msg);
      // send message
      Transport.send(message);
      System.out.println("message sent successfully");
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<Object> getListNotificationUser(User user) {
    List<Object> ls =
        RunSqlRequestForMe.runSqlRequest_ObjectList(
            "select doc_owner, file_name,destination from dms_notification_user where destination = '"
                + user.getId()
                + "'");
    return ls;
  }
}
