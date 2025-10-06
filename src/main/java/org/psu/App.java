package org.psu;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class App {
    public static void main(String[] args) {
        try {
            GmailService gmailService = new GmailService();

            // 1. CREATE a new draft
            System.out.println(">> Creating a new draft...");
            Draft newDraft = new Draft()
                    .setSubject("Demo Subject " + UUID.randomUUID())
                    .setBody("This is the body of the demo draft.");
            gmailService.createDraft(newDraft);
            System.out.println("Draft created successfully.");

            // 2. READ all drafts
            System.out.println("\n>> Reading all drafts...");
            List<Draft> drafts = gmailService.getDrafts();
            printDrafts(drafts);

            if (drafts.isEmpty()) {
                System.out.println("No drafts found to demonstrate update and delete.");
                return;
            }

            TimeUnit.SECONDS.sleep(15);

            // 3. UPDATE a draft
            System.out.println("\n>> Updating a draft...");
            Draft draftToUpdate = drafts.get(0); // Берем первый черновик для примера
            draftToUpdate = gmailService.getDraftById(draftToUpdate.getId()); // Получаем полную информацию
            draftToUpdate.setSubject("Updated Subject " + UUID.randomUUID());
            draftToUpdate.setBody("This is the updated body of the draft.");
            gmailService.updateDraft(draftToUpdate);
            System.out.println("Draft " + draftToUpdate.getId() + " updated.");

            // 4. READ all drafts again to see the update
            System.out.println("\n>> Reading all drafts after update...");
            printDrafts(gmailService.getDrafts());

            TimeUnit.SECONDS.sleep(15);

            // 5. DELETE a draft
            System.out.println("\n>> Deleting a draft...");
            Draft draftToDelete = gmailService.getDrafts().get(0); // Снова берем первый
            gmailService.deleteDraft(draftToDelete.getId());
            System.out.println("Draft " + draftToDelete.getId() + " deleted.");

            // 6. READ all drafts one last time
            System.out.println("\n>> Reading all drafts after deletion...");
            printDrafts(gmailService.getDrafts());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printDrafts(List<Draft> drafts) {
        if (drafts.isEmpty()) {
            System.out.println("No drafts found.");
            return;
        }
        for (Draft draft : drafts) {
            System.out.println("Draft ID: " + draft.getId());
        }
    }
}