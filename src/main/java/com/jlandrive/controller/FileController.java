package com.jlandrive.controller;

import com.jlandrive.model.FileListResponse;
import com.jlandrive.model.UploadResult;
import com.jlandrive.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping("/files")
    public FileListResponse listFiles(@RequestParam(defaultValue = "/") String path) {
        return fileService.listFiles(path);
    }

    @GetMapping("/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String id
    ) throws IOException {
        File file = fileService.getFileByDownloadId(id);
        if (file == null && path != null) {
            file = fileService.getFile(path);
        }
        if (file == null || !file.exists() || file.isDirectory()) {
            return ResponseEntity.notFound().build();
        }

        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(file.getName()))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(resource);
    }

    @PostMapping("/download/zip")
    public void downloadZip(@RequestParam("paths") List<String> paths, HttpServletResponse response) {
        fileService.downloadZip(paths, response);
    }

    @PostMapping("/upload")
    public UploadResult upload(@RequestParam(defaultValue = "/") String path, @RequestParam("file") MultipartFile file) throws IOException {
        return fileService.upload(path, file);
    }

    @GetMapping("/shortcuts")
    public List<com.jlandrive.model.FileInfo> getShortcuts() {
        return fileService.getShortcuts();
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleIoException(IOException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", exception.getMessage()));
    }

    private String buildContentDisposition(String fileName) {
        return fileService.buildContentDisposition(fileName);
    }
}
