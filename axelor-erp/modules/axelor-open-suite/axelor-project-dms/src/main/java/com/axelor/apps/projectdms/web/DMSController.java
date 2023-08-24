/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2021 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.projectdms.web;

import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class DMSController {

  public void sendEmail(ActionRequest request, ActionResponse response) throws MessagingException {
    // DMSServiceUtil.send("kouidi.hatim@gmail.com","ae226d10@@KOUIDI##HATIM||1991","boumedraimane@gmail.com","sujet","message");
    Properties props = new Properties();
    props.put("mail.smtp.user", "kouidi.hatim@gmail.com");
    props.put("mail.smtp.password", "ae226d10@@KOUIDI##HATIM||1991");
    props.put("mail.smtp.host", "smtp.gmail.com");
    props.put("mail.smtp.port", "465");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.ssl.enable", "true");
    // Get the Session object.// and pass username and password
    Session session =
        Session.getInstance(
            props,
            new javax.mail.Authenticator() {

              protected PasswordAuthentication getPasswordAuthentication() {

                return new PasswordAuthentication(
                    "kouidi.hatim@gmail.com", "ae226d10@@KOUIDI##HATIM||1991");
              }
            });

    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress("kouidi.hatim@gmail.com"));
    message.addRecipient(Message.RecipientType.TO, new InternetAddress("boumedraimane@gmail.com"));
    message.setSubject("Test de notification");
    message.setContent("Partage du document alfresco", "text/html");
    // Send message
    Transport.send(message);
  }
}
