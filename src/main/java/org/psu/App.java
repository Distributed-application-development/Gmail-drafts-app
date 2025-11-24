package org.psu;

import java.util.List;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        try {
            AuthService authService = new AuthService();
            GmailApiService apiService = new GmailApiService();
            Scanner scanner = new Scanner(System.in);

            System.out.println("Authenticating...");
            String accessToken = authService.getAccessToken();
            System.out.println("Authentication successful!");

            while (true) {
                System.out.println("\n--- Gmail Drafts Manager ---");
                System.out.println("1. List drafts");
                System.out.println("2. Create a new draft");
                System.out.println("3. Update a draft");
                System.out.println("4. Delete a draft");
                System.out.println("5. Exit");
                System.out.print("Choose an option: ");

                int choice;
                try {
                    choice = Integer.parseInt(scanner.nextLine());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a number.");
                    continue;
                }

                switch (choice) {
                    case 1: // List drafts
                        List<Draft> drafts = apiService.getDrafts(accessToken);
                        if (drafts.isEmpty()) {
                            System.out.println("No drafts found.");
                        } else {
                            System.out.println("Found " + drafts.size() + " draft(s):");
                            for (Draft d : drafts) {
                                Draft fullDraft = apiService.getDraftById(d.getId(), accessToken);
                                System.out.println("  - ID: " + fullDraft.getId() + ", Subject: '" + fullDraft.getSubject() + "'");
                            }
                        }
                        break;
                    case 2: // Create draft
                        System.out.print("Enter subject: ");
                        String subject = scanner.nextLine();
                        System.out.print("Enter body: ");
                        String body = scanner.nextLine();
                        Draft newDraft = new Draft().setSubject(subject).setBody(body);
                        apiService.createDraft(accessToken, newDraft);
                        System.out.println("Draft created successfully!");
                        break;
                    case 3: // Update draft
                        List<Draft> draftsToUpdate = apiService.getDrafts(accessToken);
                        if (draftsToUpdate.isEmpty()) {
                            System.out.println("No drafts to update.");
                            break;
                        }

                        System.out.println("Select a draft to update:");
                        for (int i = 0; i < draftsToUpdate.size(); i++) {
                            Draft fullDraft = apiService.getDraftById(draftsToUpdate.get(i).getId(), accessToken);
                            System.out.println("  " + (i + 1) + ". ID: " + fullDraft.getId() + ", Subject: '" + fullDraft.getSubject() + "'");
                        }

                        System.out.print("Enter number: ");
                        int numToUpdate = Integer.parseInt(scanner.nextLine()) - 1;

                        if (numToUpdate >= 0 && numToUpdate < draftsToUpdate.size()) {
                            Draft draftToUpdate = draftsToUpdate.get(numToUpdate);

                            System.out.print("Enter new subject: ");
                            String newSubject = scanner.nextLine();
                            System.out.print("Enter new body: ");
                            String newBody = scanner.nextLine();

                            draftToUpdate.setSubject(newSubject).setBody(newBody);

                            apiService.updateDraft(accessToken, draftToUpdate);
                            System.out.println("Draft " + draftToUpdate.getId() + " updated successfully!");
                        } else {
                            System.out.println("Invalid number.");
                        }
                        break;
                    case 4: // Delete draft
                        List<Draft> draftsToDelete = apiService.getDrafts(accessToken);
                        if (draftsToDelete.isEmpty()) {
                            System.out.println("No drafts to delete.");
                            break;
                        }
                        System.out.println("Select a draft to delete:");
                        for (int i = 0; i < draftsToDelete.size(); i++) {
                            Draft fullDraft = apiService.getDraftById(draftsToDelete.get(i).getId(), accessToken);
                            System.out.println("  " + (i + 1) + ". ID: " + fullDraft.getId() + ", Subject: '" + fullDraft.getSubject() + "'");
                        }
                        System.out.print("Enter number: ");
                        int numToDelete = Integer.parseInt(scanner.nextLine()) - 1;
                        if (numToDelete >= 0 && numToDelete < draftsToDelete.size()) {
                            String idToDelete = draftsToDelete.get(numToDelete).getId();
                            apiService.deleteDraft(accessToken, idToDelete);
                            System.out.println("Draft " + idToDelete + " deleted.");
                        } else {
                            System.out.println("Invalid number.");
                        }
                        break;
                    case 5: // Exit
                        System.out.println("Exiting.");
                        return;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            }
        } catch (Exception e) {
            System.err.println("An error occurred:");
            e.printStackTrace();
        }
    }
}