package com.example.myapplication.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "C_COMM_HISTORY", primaryKeys = {"RENBAN"})
public class CommHistoryEntity {
    @ColumnInfo(name = "RENBAN")
    public Integer renban;

    @ColumnInfo(name = "COMM_KIND")
    public Integer commKind;

    @ColumnInfo(name = "COMM_DETAIL")
    public String commDetail;

    @ColumnInfo(name = "COMM_RESULT")
    public Integer commResult;

    @ColumnInfo(name = "START_YMDHMS")
    public String startYmdhms;

    @ColumnInfo(name = "INSERT_PROC_NAME")
    public String insertProcName;

    @ColumnInfo(name = "INSERT_YMD")
    public String insertYmd;

    @ColumnInfo(name = "UPDATE_PROC_NAME")
    public String updateProcName;
}