package com.jlandrive.controller;

import com.jlandrive.model.FileListResponse;
import com.jlandrive.model.UploadResult;
import com.jlandrive.model.ZipDownloadPreparation;
import com.jlandrive.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FilterInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    @GetMapping("/download/zip")
    public ResponseEntity<Resource> downloadZipByIds(@RequestParam("ids") List<String> ids) throws IOException {
        return downloadZipByIds(ids, null);
    }

    @GetMapping("/download/zip/{fileName:.+}")
    public ResponseEntity<Resource> downloadZipByIds(
            @RequestParam("ids") List<String> ids,
            @PathVariable(required = false) String fileName
    ) throws IOException {
        List<File> files = fileService.getFilesByDownloadIds(ids);
        if (files.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (files.size() == 1 && files.get(0).isFile()) {
            File file = files.get(0);
            Resource resource = new org.springframework.core.io.FileSystemResource(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(file.getName()))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(file.length())
                    .body(resource);
        }

        File zipFile = fileService.createTempZip(files);
        String downloadFileName = normalizeZipFileName(fileName);
        Resource resource = new InputStreamResource(new DeleteOnCloseFileInputStream(zipFile));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(downloadFileName))
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(zipFile.length())
                .body(resource);
    }

    @PostMapping("/download/zip/prepare")
    public ZipDownloadPreparation prepareZipDownload(@RequestBody List<String> ids) throws IOException {
        return fileService.prepareZipDownload(ids);
    }

    @GetMapping("/download/zip/prepared/{token}/{fileName:.+}")
    public ResponseEntity<Resource> downloadPreparedZip(
            @PathVariable String token,
            @PathVariable String fileName
    ) throws IOException {
        FileService.PreparedZip preparedZip = fileService.getPreparedZip(token);
        if (preparedZip == null || !preparedZip.file().exists()) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        Resource resource = new InputStreamResource(new DeleteOnClosePreparedZipInputStream(token, preparedZip.file()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(normalizeZipFileName(fileName)))
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(preparedZip.file().length())
                .body(resource);
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
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", exception.getMessage()));
    }

    private String buildContentDisposition(String fileName) {
        return fileService.buildContentDisposition(fileName);
    }

    private String normalizeZipFileName(String fileName) {
        String defaultName = "batch_download.zip";
        if (fileName == null || fileName.isBlank()) {
            return defaultName;
        }

        String decodedName = java.net.URLDecoder.decode(fileName, StandardCharsets.UTF_8);
        String safeName = decodedName
                .replace("\\", "_")
                .replace("/", "_")
                .replace("\"", "")
                .trim();
        if (safeName.isBlank()) {
            return defaultName;
        }
        return safeName.toLowerCase().endsWith(".zip") ? safeName : safeName + ".zip";
    }

    private static class DeleteOnCloseFileInputStream extends FilterInputStream {
        private final File file;

        DeleteOnCloseFileInputStream(File file) throws IOException {
            super(new FileInputStream(file));
            this.file = file;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (file.exists() && !file.delete()) {
                    file.deleteOnExit();
                }
            }
        }
    }

    private class DeleteOnClosePreparedZipInputStream extends FilterInputStream {
        private final String token;

        DeleteOnClosePreparedZipInputStream(String token, File file) throws IOException {
            super(new FileInputStream(file));
            this.token = token;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                fileService.releasePreparedZip(token);
            }
        }
    }
}
