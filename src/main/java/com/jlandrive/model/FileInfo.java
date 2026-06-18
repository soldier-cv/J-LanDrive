package com.jlandrive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    private String name;
    private String path; // 绝对路径
    private String downloadId; // 用于下载的稳定标识，避免路径被下载器错误转义
    private String type; // DIR, FILE
    private long size;
    private String time;
}
