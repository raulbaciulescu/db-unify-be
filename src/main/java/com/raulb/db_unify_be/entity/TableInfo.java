package com.raulb.db_unify_be.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class TableInfo {
    private String tableName;
    private List<ColumnInfo> columns;
}
