import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class Main {

    // Chemins vers les fichiers de logs (à adapter selon ton dossier)
    static final String GENERAL_LOG = "/Users/rayanbouabid/Downloads/logs/general.log";
    static final String TRAITE_LOG = "/Users/rayanbouabid/Downloads/logs/traitees.log";

    public static void main(String[] args) {

        // Contenu global de l’email (un seul email pour tous les lots en erreur)
        StringBuilder emailContent = new StringBuilder();

        try {
            // Connexion à la base de données
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/GDExpert",
                    "root",
                    "10011234"
            );

            System.out.println("Connexion réussie");

            // Requête pour récupérer uniquement les lots en erreur(discuter aujourd'hui)
            String query = "SELECT id, etape FROM batches WHERE status = 'ERROR'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            // Parcours des résultats
            while (rs.next()) {

                int id = rs.getInt("id");
                String etape = rs.getString("etape");

                // Vérification si le lot a déjà été notifié (éviter les doublons)
                if (isAlreadyNotified(id, etape)) {
                    continue;
                }

                // Écriture dans le log général (avec date, heure, statut NOTIFIED)
                logGeneral(id, etape);

                // Ajout dans le contenu de l’email
                emailContent.append("ID=")
                        .append(id)
                        .append(" - ETAPE=")
                        .append(etape)
                        .append("\n");

                // Écriture dans le log des lots déjà traités
                logTraite(id, etape);
            }

            // Envoi d’un seul email si au moins un lot en erreur existe
            if (emailContent.length() > 0) {
                sendEmail(emailContent.toString());
            }

            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Fonction qui écrit dans le fichier general.log avec date et heure
    static void logGeneral(int id, String etape) {
        try {
            FileWriter fw = new FileWriter(GENERAL_LOG, true);

            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new Date());

            fw.write(date + " - NOTIFIED - ID=" + id + " - ETAPE=" + etape + "\n");

            fw.close();

        } catch (Exception e) {
            System.out.println("Erreur logGeneral");
        }
    }

    // Fonction qui écrit dans traitees.log (uniquement ID et étape)
    static void logTraite(int id, String etape) {
        try {
            FileWriter fw = new FileWriter(TRAITE_LOG, true);

            fw.write("ID=" + id + " - ETAPE=" + etape + "\n");

            fw.close();

        } catch (Exception e) {
            System.out.println("Erreur logTraite");
        }
    }

    // Vérifie si un lot a déjà été notifié en lisant le fichier traitees.log
    static boolean isAlreadyNotified(int id, String etape) {

        try {
            File file = new File(TRAITE_LOG);

            // Si le fichier n’existe pas encore, aucun lot n’est notifié
            if (!file.exists()) {
                return false;
            }

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            // Lecture ligne par ligne pour chercher une correspondance
            while ((line = br.readLine()) != null) {

                if (line.contains("ID=" + id) && line.contains("ETAPE=" + etape)) {
                    br.close();
                    return true;
                }
            }

            br.close();

        } catch (Exception e) {
            System.out.println("Erreur lecture log");
        }

        return false;
    }

    // Fonction d’envoi d’email (un seul email contenant tous les lots)
    static void sendEmail(String content) {

        final String from = "YOUR_EMAIL";
        final String password = "YOUR_PASSWORD";

        Properties props = new Properties();

        // Configuration SMTP pour Gmail
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(from, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(from));

            // Envoi vers le même email (test)
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(from));

            message.setSubject("Alert - Lots en erreur détectés");

            message.setText(content);

            Transport.send(message);

            System.out.println("Email envoyé");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
