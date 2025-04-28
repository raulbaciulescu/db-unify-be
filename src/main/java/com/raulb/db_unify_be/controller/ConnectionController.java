package com.raulb.db_unify_be.controller;

import com.raulb.db_unify_be.dtos.ConnectionResponse;
import com.raulb.db_unify_be.dtos.RefreshConnectionResult;
import com.raulb.db_unify_be.entity.Connection;
import com.raulb.db_unify_be.service.ConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/connections")
@RequiredArgsConstructor
@CrossOrigin
public class ConnectionController {
    private final ConnectionService connectionService;

    @PostMapping
    public ResponseEntity<Connection> createConnection(@RequestBody Connection conn) {
        Connection created = connectionService.createAndConnect(conn); // throws if connection fails
        return ResponseEntity.ok(created);
    }

    @PostMapping("/{id}/refresh")
    public ResponseEntity<RefreshConnectionResult> refreshConnection(@PathVariable Long id) {
        System.out.println("Testing connection for " + id);
        try {
            connectionService.refreshConnection(id);
            return ResponseEntity.ok(new RefreshConnectionResult(true));
        } catch (Exception e) {
            return ResponseEntity.ok(new RefreshConnectionResult(false));
        }
    }

    @GetMapping
    public ResponseEntity<List<ConnectionResponse>> getAll() {
        return ResponseEntity.ok(connectionService.findAll());
    }

//    @DeleteMapping("/{dbType}/{id}")
//    public ResponseEntity<Void> delete(@PathVariable String dbType, @PathVariable Long id) {
//        connectionService.delete(dbType, id);
//        return ResponseEntity.noContent().build();
//    }

    //    @GetMapping("/{dbType}")
//    public ResponseEntity<List<Connection>> getAll(@PathVariable String dbType) {
//        return ResponseEntity.ok(connectionService.findAll(dbType));
//    }
}
