package com.raulb.db_unify_be.controller;


import com.raulb.db_unify_be.dtos.QueryRequest;
import com.raulb.db_unify_be.entity.ParsedQuery;
import com.raulb.db_unify_be.service.SqlParsingService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping(("/sqlCommand"))
@CrossOrigin(origins = "http://localhost:3000")
public class QueryController {
    private SqlParsingService service;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ParsedQuery query(@RequestBody QueryRequest request){
        return service.parse(request.query());
    }
}
