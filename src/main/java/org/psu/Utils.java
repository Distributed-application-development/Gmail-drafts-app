package org.psu;

import javax.mail.internet.MimeMessage;

class Utils {
    public static MimeMessage createEmail(String to, String from, String subject, String bodyText) throws Exception {
        java.util.Properties props = new java.util.Properties();
        javax.mail.Session session = javax.mail.Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new javax.mail.internet.InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new javax.mail.internet.InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }
}
