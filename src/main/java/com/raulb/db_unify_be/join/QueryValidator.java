//package com.raulb.db_unify_be.join;
//
//
//import com.raulb.db_unify_be.entity.ParsedQuery;
//import com.raulb.db_unify_be.metadata.ColumnMetadata;
//import com.raulb.db_unify_be.metadata.TableMetadata;
//import com.raulb.db_unify_be.service.MetadataService;
//import com.raulb.db_unify_be.service.TableMetadataService;
//import lombok.RequiredArgsConstructor;
//import net.sf.jsqlparser.expression.Expression;
//import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
//import net.sf.jsqlparser.schema.Column;
//import net.sf.jsqlparser.statement.select.Join;
//import org.springframework.stereotype.Component;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Component
//@RequiredArgsConstructor
//public class QueryValidator {
//    private final MetadataService metadataService;
//
//    public boolean isValid(ParsedQuery parsedQuery) {
//        try {
//            validateJoins(parsedQuery);
//            return true;
//        } catch (IllegalArgumentException e) {
//            System.out.println("Invalid query: " + e.getMessage());
//            return false;
//        }
//    }
//
//    private void validateJoins(ParsedQuery parsedQuery) {
//        List<Join> joins = parsedQuery.getJoins();
//        Map<String, TableMetadata> tableMetadataMap = new HashMap<>();
//
//        for (String tableName : parsedQuery.getTables()) {
//            TableMetadata tableMetadata = metadataService.getTableMetadata(tableName);
//            if (tableMetadata == null) {
//                throw new IllegalArgumentException("Table " + tableName + " does not exist.");
//            }
//            tableMetadataMap.put(tableName, tableMetadata);
//        }
//
//        for (Join join : joins) {
//            Expression onExpression = join.getOnExpression();
//            if (onExpression instanceof EqualsTo equalsTo) {
//                Column leftColumn = (Column) equalsTo.getLeftExpression();
//                Column rightColumn = (Column) equalsTo.getRightExpression();
//
//                String leftTableName = leftColumn.getTable().getName();
//                String rightTableName = rightColumn.getTable().getName();
//
//                if (!tableMetadataMap.containsKey(leftTableName)) {
//                    throw new IllegalArgumentException("Table " + leftTableName + " does not exist.");
//                }
//                if (!tableMetadataMap.containsKey(rightTableName)) {
//                    throw new IllegalArgumentException("Table " + rightTableName + " does not exist.");
//                }
//
//                TableMetadata leftTableMetadata = tableMetadataMap.get(leftTableName);
//                TableMetadata rightTableMetadata = tableMetadataMap.get(rightTableName);
//
//                ColumnMetadata leftColumnMetadata = leftTableMetadata.getColumns().get(leftColumn.getColumnName());
//                ColumnMetadata rightColumnMetadata = rightTableMetadata.getColumns().get(rightColumn.getColumnName());
//
//                if (leftColumnMetadata == null) {
//                    throw new IllegalArgumentException("Column " + leftColumn.getColumnName() + " does not exist in table " + leftTableName);
//                }
//                if (rightColumnMetadata == null) {
//                    throw new IllegalArgumentException("Column " + rightColumn.getColumnName() + " does not exist in table " + rightTableName);
//                }
//
//                if (!leftColumnMetadata.getType().equals(rightColumnMetadata.getType())) {
//                    throw new IllegalArgumentException("Data types of columns do not match: " +
//                            leftColumn.getFullQualifiedName() + " and " +
//                            rightColumn.getFullQualifiedName());
//                }
//            } else {
//                throw new IllegalArgumentException("Unsupported join condition: " + onExpression);
//            }
//        }
//    }
//}
