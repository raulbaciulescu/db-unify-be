package com.raulb.db_unify_be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Component
@RequiredArgsConstructor
public class BootstrapInitializer implements ApplicationRunner {
    private final ConnectionService service;
    private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/bootstrapdb";
    private static final String JDBC_USER = "postgres";
    private static final String JDBC_PASSWORD = "postgres";

    private static final int TOTAL_RECORDS = 10_000_000;
    private static final int BATCH_SIZE = 1000; // Insert 1000 at once for efficiency
    @Override
    public void run(ApplicationArguments args) {
        service.initializeAllConnections();
        //extracted();
    }

    private static void extracted() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            conn.setAutoCommit(false); // Batch insert, manual commit

            String sql = "INSERT INTO student10M (name, age, email) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 1; i <= TOTAL_RECORDS; i++) {
                    String name = "Student" + i;
                    int age = 18 + (i % 5); // Age between 18 and 22
                    String email = "student" + i + "@example.com";

                    pstmt.setString(1, name);
                    pstmt.setInt(2, age);
                    pstmt.setString(3, email);

                    pstmt.addBatch();

                    if (i % BATCH_SIZE == 0) {
                        pstmt.executeBatch();
                        conn.commit(); // Commit after each batch
                        System.out.println(i + " records inserted...");
                    }
                }
                // Insert remaining records
                pstmt.executeBatch();
                conn.commit();
            }
            System.out.println("All " + TOTAL_RECORDS + " student records inserted successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}