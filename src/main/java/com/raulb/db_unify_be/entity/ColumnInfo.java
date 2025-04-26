package com.raulb.db_unify_be.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ColumnInfo {
    private String columnName;
    private String dataType;
}
