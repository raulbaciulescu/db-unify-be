package com.raulb.db_unify_be;

import com.raulb.db_unify_be.service.ConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BootstrapInitializer implements ApplicationRunner {

    private final ConnectionService service;
    private static final Random random = new Random();
    private static final int TOTAL_RECORDS = 1000;
    private static final int COMMON_CNP_COUNT = 300;
    private List<String> commonCnps;

    @Override
    public void run(ApplicationArguments args) {
        service.initializeAllConnections();
//
//        generateCommonCnps();
//        populatePopulation();
//        populateDecathlonCustomers();
//        populateMarathonParticipants();
//        populateGdprConsents();
    }

    private void generateCommonCnps() {
        commonCnps = new ArrayList<>();
        for (int i = 0; i < COMMON_CNP_COUNT; i++) {
            commonCnps.add(generateRandomCnp());
        }
    }

    private String getRandomCommonCnp() {
        return commonCnps.get(random.nextInt(commonCnps.size()));
    }

    private void populatePopulation() {
        String jdbcUrl = "jdbc:sqlserver://localhost:1433;databaseName=population_db;integratedSecurity=true;trustServerCertificate=true";

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            conn.setAutoCommit(false);

            String sql = "INSERT INTO population (cnp, first_name, last_name, location) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                Set<String> used = new HashSet<>();
                for (int i = 0; i < TOTAL_RECORDS; i++) {
                    String cnp = (i < COMMON_CNP_COUNT) ? commonCnps.get(i) : generateRandomCnp(used);
                    pstmt.setString(1, cnp);
                    pstmt.setString(2, randomFirstName());
                    pstmt.setString(3, randomLastName());
                    pstmt.setString(4, randomCity());

                    pstmt.addBatch();

                    if (i % 100 == 0) {
                        pstmt.executeBatch();
                        conn.commit();
                    }
                }
                pstmt.executeBatch();
                conn.commit();
            }
            System.out.println("Population table populated.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void populateDecathlonCustomers() {
        String jdbcUrl = "jdbc:postgresql://localhost:5432/decathlon";
        String user = "postgres";
        String password = "postgres";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
            conn.setAutoCommit(false);

            String sql = "INSERT INTO decathlon_customers (email, cnp) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < TOTAL_RECORDS; i++) {
                    String cnp = (i < COMMON_CNP_COUNT) ? commonCnps.get(i) : generateRandomCnp();
                    pstmt.setString(1, generateRandomEmail());
                    pstmt.setString(2, cnp);

                    pstmt.addBatch();

                    if (i % 100 == 0) {
                        pstmt.executeBatch();
                        conn.commit();
                    }
                }
                pstmt.executeBatch();
                conn.commit();
            }
            System.out.println("Decathlon customers table populated.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void populateMarathonParticipants() {
        String jdbcUrl = "jdbc:mysql://localhost:3306/marathon_db";
        String user = "root";
        String password = "266259";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
            conn.setAutoCommit(false);

            String sql = "INSERT INTO marathon_participants (cnp, event_name, year) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < TOTAL_RECORDS; i++) {
                    String cnp = (i < COMMON_CNP_COUNT) ? commonCnps.get(i) : generateRandomCnp();
                    pstmt.setString(1, cnp);
                    pstmt.setString(2, "Maraton Cluj");
                    pstmt.setInt(3, 2025);

                    pstmt.addBatch();

                    if (i % 100 == 0) {
                        pstmt.executeBatch();
                        conn.commit();
                    }
                }
                pstmt.executeBatch();
                conn.commit();
            }
            System.out.println("Marathon participants table populated.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void populateGdprConsents() {
        String jdbcUrl = "jdbc:oracle:thin:@193.231.20.20:15211:orcl19c";
        String user = "brmbd1r02";
        String password = "brmbd1r02";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
            conn.setAutoCommit(false);

            String sql = "INSERT INTO gdpr_consents (cnp, consent_given, consent_date) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < TOTAL_RECORDS; i++) {
                    String cnp = (i < COMMON_CNP_COUNT) ? commonCnps.get(i) : generateRandomCnp();
                    pstmt.setString(1, cnp);
                    pstmt.setInt(2, 1); // force GDPR accepted to make the JOIN successful
                    pstmt.setDate(3, java.sql.Date.valueOf(LocalDate.now().minusDays(random.nextInt(365))));

                    pstmt.addBatch();

                    if (i % 100 == 0) {
                        pstmt.executeBatch();
                        conn.commit();
                    }
                }
                pstmt.executeBatch();
                conn.commit();
            }
            System.out.println("GDPR consents table populated.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String generateRandomCnp() {
        return String.valueOf(1000000000000L + random.nextInt(900000000));
    }

    private static String generateRandomCnp(Set<String> used) {
        String cnp;
        do {
            cnp = generateRandomCnp();
        } while (used.contains(cnp));
        used.add(cnp);
        return cnp;
    }

    private static String randomFirstName() {
        String[] names = {"Ana", "Ion", "Maria", "Vasile", "Elena", "George", "Ioana", "Stefan"};
        return names[random.nextInt(names.length)];
    }

    private static String randomLastName() {
        String[] names = {"Popescu", "Ionescu", "Georgescu", "Marin", "Dumitrescu", "Stan", "Enache"};
        return names[random.nextInt(names.length)];
    }

    private static String randomCity() {
        String[] cities = {"Cluj-Napoca", "Bucuresti", "Iasi", "Timisoara", "Brasov", "Constanta"};
        return cities[random.nextInt(cities.length)];
    }

    private static String generateRandomEmail() {
        return UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
