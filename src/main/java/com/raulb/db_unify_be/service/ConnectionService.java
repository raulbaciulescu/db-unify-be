package com.raulb.db_unify_be.service;

import com.raulb.db_unify_be.dtos.ConnectionResponse;
import com.raulb.db_unify_be.entity.Connection;
import com.raulb.db_unify_be.repository.ConnectionRepository;
import com.raulb.db_unify_be.util.ConnectionMapper;
import com.raulb.db_unify_be.util.CryptoJsAesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConnectionService {
    private final ConnectionRepository repo;
    private final DynamicDataSourceFactory factory;
    private final CryptoJsAesService cryptoJsAesService;

    public Connection createAndConnect(Connection conn) {
        Connection saved = repo.save(conn);
        saved = repo.findById(saved.getId()).orElseThrow();
        try {
            String decryptedPassword = cryptoJsAesService.decrypt(conn.getPassword());
            conn.setPassword(decryptedPassword);
            factory.createAndValidate(saved);
        } catch (Exception e) {
            e.printStackTrace();
            repo.delete(saved);
            return null;
        }

        return saved;
    }

    public List<ConnectionResponse> findAll() {
        return repo.findAll()
                .stream()
                .map(c -> {
                    String decryptedPassword = cryptoJsAesService.decrypt(c.getPassword());
                    c.setPassword(decryptedPassword);
                    factory.createAndValidate(c);
                    return ConnectionMapper.toResponse(c, "connected");
                })
                .toList();
    }

    public void refreshConnection(Long id) {
        Connection conn = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found"));
        String decryptedPassword = cryptoJsAesService.decrypt(conn.getPassword());
        conn.setPassword(decryptedPassword);
        // Attempt to create and validate new DataSource
        factory.createAndValidate(conn);
    }

    public void initializeAllConnections() {
        repo.findAll().forEach(conn -> {
            try {
                String decryptedPassword = cryptoJsAesService.decrypt(conn.getPassword());
                conn.setPassword(decryptedPassword);
                factory.createAndValidate(conn);
                System.out.println("Connected: " + conn.getName());
            } catch (Exception e) {
                System.err.println("Connection failed: " + conn.getName() + " - " + e.getMessage());
            }
        });
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
