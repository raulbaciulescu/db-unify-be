package com.raulb.db_unify_be.service;



import com.raulb.db_unify_be.dtos.QueryRequest;

import java.util.List;
import java.util.Map;

public interface QueryService {
    List<Map<String, String>> executeQuery(QueryRequest request);
}
