package com.raulb.db_unify_be.controller;


import com.raulb.db_unify_be.dtos.QueryRequest;
import com.raulb.db_unify_be.dtos.QueryResult;
import com.raulb.db_unify_be.entity.ParsedQuery;
import com.raulb.db_unify_be.service.QueryService;
import com.raulb.db_unify_be.service.SqlParsingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(("/sqlCommand"))
@CrossOrigin
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
    public QueryResult execute(@RequestBody QueryRequest request){
        return queryService.execute(request.query(), request.offset());
    }
}
