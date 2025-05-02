package com.raulb.db_unify_be.controller;


import com.raulb.db_unify_be.dtos.QueryRequest;
import com.raulb.db_unify_be.entity.ParsedQuery;
import com.raulb.db_unify_be.join.QueryService;
import com.raulb.db_unify_be.service.SqlParsingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(("/sqlCommand"))
@CrossOrigin(origins = "http://localhost:3000")
public class QueryController {
    private final QueryService queryService;
    private final SqlParsingService sqlParsingService;

    @PostMapping("/parse")
    @ResponseStatus(HttpStatus.OK)
    public ParsedQuery query(@RequestBody QueryRequest request){
        return sqlParsingService.parse(request.query());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Map<String, Object>> execute(@RequestBody QueryRequest request){
        return queryService.execute(request.query());
    }
}
