package org.psu;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListDraftsResponse;
import com.google.api.services.gmail.model.Message;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class GmailService {
    private static final String APPLICATION_NAME = "Gmail Drafts Manager";
    private final Gmail service;
    private static final String USER_ID = "me"; // Идентификатор текущего пользователя

    public GmailService() throws GeneralSecurityException, IOException {
        Credential credential = Auth.authorize();
        this.service = new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    // CREATE
    public void createDraft(Draft draft) throws MessagingException, IOException {
        MimeMessage mimeMessage = createEmail("to@example.com", "me", draft.getSubject(), draft.getBody());
        Message message = createMessageWithEmail(mimeMessage);
        com.google.api.services.gmail.model.Draft gmailDraft = new com.google.api.services.gmail.model.Draft().setMessage(message);
        service.users().drafts().create(USER_ID, gmailDraft).execute();
    }

    // READ (List)
    public List<Draft> getDrafts() throws IOException {
        ListDraftsResponse response = service.users().drafts().list(USER_ID).execute();
        List<com.google.api.services.gmail.model.Draft> drafts = new ArrayList<>();
        if (response.getDrafts() != null) {
            drafts.addAll(response.getDrafts());
        }
        return drafts.stream().map(d -> new Draft().setId(d.getId())).collect(Collectors.toList());
    }

    // READ (Single)
    public Draft getDraftById(String draftId) throws IOException {
        com.google.api.services.gmail.model.Draft gmailDraft = service.users().drafts().get(USER_ID, draftId).setFormat("full").execute();
        Message message = gmailDraft.getMessage();
        String subject = message.getPayload().getHeaders().stream()
                .filter(h -> h.getName().equals("Subject"))
                .findFirst().map(h -> h.getValue()).orElse("");
        // Примечание: получение тела письма сложнее и зависит от его структуры (простой текст, html, вложения)
        // Здесь для простоты мы оставим тело пустым.
        return new Draft().setId(gmailDraft.getId()).setSubject(subject).setBody("Body not parsed for simplicity");
    }

    // UPDATE
    public void updateDraft(Draft draft) throws MessagingException, IOException {
        MimeMessage mimeMessage = createEmail("to@example.com", "me", draft.getSubject(), draft.getBody());
        Message message = createMessageWithEmail(mimeMessage);
        com.google.api.services.gmail.model.Draft gmailDraft = new com.google.api.services.gmail.model.Draft().setMessage(message);
        service.users().drafts().update(USER_ID, draft.getId(), gmailDraft).execute();
    }

    // DELETE
    public void deleteDraft(String draftId) throws IOException {
        service.users().drafts().delete(USER_ID, draftId).execute();
    }

    private MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    private Message createMessageWithEmail(MimeMessage email) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
}
