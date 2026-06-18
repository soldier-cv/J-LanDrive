package com.jlandrive.service;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.jlandrive.model.FileInfo;
import com.jlandrive.model.FileListResponse;
import com.jlandrive.model.UploadResult;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    /**
     * List files in the specified path.
     * If path is empty or "/", list system roots.
     */
    public FileListResponse listFiles(String path) {
        // If path is empty or root, list system drives
        if (StrUtil.isEmpty(path) || "/".equals(path)) {
            File[] roots = File.listRoots();
            List<FileInfo> fileInfos = new ArrayList<>();
            if (roots != null) {
                for (File root : roots) {
                    fileInfos.add(buildFileInfo(root, true));
                }
            }
            return FileListResponse.builder()
                    .currentPath("/")
                    .list(fileInfos)
                    .build();
        }

        File currentDir = new File(path);
        if (!currentDir.exists() || !currentDir.isDirectory()) {
            return FileListResponse.builder()
                    .currentPath(path)
                    .list(new ArrayList<>())
                    .build();
        }

        File[] files = currentDir.listFiles();
        List<FileInfo> fileInfos = new ArrayList<>();
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::isDirectory).reversed().thenComparing(File::getName));
            for (File file : files) {
                fileInfos.add(buildFileInfo(file, false));
            }
        }

        return FileListResponse.builder()
                .currentPath(path)
                .list(fileInfos)
                .build();
    }

    /**
     * Get file for download.
     */
    public File getFile(String path) {
        return new File(path);
    }

    /**
     * 通过稳定标识解析文件，兼容下载器对路径参数的重写。
     */
    public File getFileByDownloadId(String downloadId) {
        if (StrUtil.isBlank(downloadId)) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(downloadId);
            return new File(new String(decoded, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid downloadId: {}", downloadId);
            return null;
        }
    }

    /**
     * Stream zip download or single file download.
     * <p>
     * If a single file is selected, it streams the file directly with its original content type (or octet-stream).
     * If multiple files or a directory is selected, it packages them into a ZIP file and streams it.
     * </p>
     *
     * @param paths    List of absolute paths to download.
     * @param response HttpServletResponse to write the stream to.
     */
    public void downloadZip(List<String> paths, HttpServletResponse response) {
        if (paths == null || paths.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Smart download: if only 1 file and it's not a directory, download directly
        if (paths.size() == 1) {
            File singleFile = new File(paths.get(0));
            if (singleFile.exists() && singleFile.isFile()) {
                try {
                    prepareAttachmentHeaders(response, singleFile.getName(), "application/octet-stream");
                    response.setContentLengthLong(singleFile.length());
                    FileUtil.writeToStream(singleFile, response.getOutputStream());
                    return;
                } catch (IOException e) {
                    log.error("Error streaming single file", e);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            }
        }

        response.setContentType("application/zip");
        String fileName = "batch_download_" + DatePattern.PURE_DATETIME_FORMAT.format(new Date()) + ".zip";
        setContentDisposition(response, fileName);

        try (ZipOutputStream zipOut = new ZipOutputStream(new java.io.BufferedOutputStream(response.getOutputStream()), StandardCharsets.UTF_8)) {
            zipOut.setLevel(java.util.zip.Deflater.BEST_SPEED);

            for (String path : paths) {
                File file = new File(path);
                if (!file.exists()) continue;
                addToZip(file, file.getName(), zipOut);
            }
            zipOut.flush();
        } catch (IOException e) {
            log.error("Error streaming zip", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Recursively add file or directory to zip output stream.
     *
     * @param file     File to add.
     * @param fileName Name of the file in the zip.
     * @param zipOut   ZipOutputStream.
     * @throws IOException If I/O error occurs.
     */
    private void addToZip(File file, String fileName, ZipOutputStream zipOut) throws IOException {
        if (file.isHidden()) return;

        if (file.isDirectory()) {
            if (!fileName.endsWith("/")) {
                fileName += "/";
            }
            zipOut.putNextEntry(new ZipEntry(fileName));
            zipOut.closeEntry();

            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    addToZip(child, fileName + child.getName(), zipOut);
                }
            }
        } else {
            zipOut.putNextEntry(new ZipEntry(fileName));
            FileUtil.writeToStream(file, zipOut);
            zipOut.closeEntry();
        }
    }

    /**
     * Upload a file to the specified path.
     * <p>
     * Handles duplicate filenames by appending a counter (e.g., file(1).txt).
     * </p>
     *
     * @param path Target directory path.
     * @param file MultipartFile to upload.
     * @throws IOException If I/O error occurs.
     */
    public UploadResult upload(String path, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("上传文件为空");
        }

        File dir = new File(path);
        if ("/".equals(path)) {
            throw new IOException("不能直接上传到根视图，请先进入具体目录");
        }
        if (dir.exists() && !dir.isDirectory()) {
            throw new IOException("目标路径不是目录");
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("创建目标目录失败");
        }

        String originalFilename = FileUtil.getName(file.getOriginalFilename());
        if (StrUtil.isBlank(originalFilename)) {
            throw new IOException("文件名无效");
        }
        File dest = FileUtil.file(dir, originalFilename);

        // Handle duplicate names
        if (dest.exists()) {
            String name = FileUtil.mainName(originalFilename);
            String ext = FileUtil.extName(originalFilename);
            int counter = 1;
            while (dest.exists()) {
                String newName = name + "(" + counter + ")" + (StrUtil.isEmpty(ext) ? "" : "." + ext);
                dest = FileUtil.file(dir, newName);
                counter++;
            }
        }

        try (InputStream inputStream = file.getInputStream()) {
            FileUtil.writeFromStream(inputStream, dest);
        }

        return UploadResult.builder()
                .originalName(originalFilename)
                .savedName(dest.getName())
                .path(dest.getAbsolutePath())
                .build();
    }

    private final org.springframework.core.env.Environment environment;

    /**
     * Get system shortcuts (Desktop, Documents, etc.).
     *
     * @return List of FileInfo representing shortcuts.
     */
    public List<FileInfo> getShortcuts() {
        List<FileInfo> shortcuts = new ArrayList<>();
        // Allow overriding user home via --user.home=... argument
        String userHome = environment.getProperty("user.home", System.getProperty("user.home"));

        addShortcut(shortcuts, userHome + "/Desktop", "Desktop");
        addShortcut(shortcuts, userHome + "/Documents", "Documents");
        addShortcut(shortcuts, userHome + "/Downloads", "Downloads");
        addShortcut(shortcuts, userHome + "/Pictures", "Pictures");
        addShortcut(shortcuts, userHome + "/Videos", "Videos");
        addShortcut(shortcuts, userHome + "/Music", "Music");

        return shortcuts;
    }

    private void addShortcut(List<FileInfo> list, String path, String name) {
        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            list.add(FileInfo.builder()
                    .name(name)
                    .path(file.getAbsolutePath())
                    .downloadId(createDownloadId(file))
                    .type("DIR")
                    .build());
        }
    }

    /**
     * 统一构造文件信息，避免前端自己拼接路径。
     */
    private FileInfo buildFileInfo(File file, boolean rootDisplay) {
        String displayName = rootDisplay ? file.getAbsolutePath() : file.getName();
        return FileInfo.builder()
                .name(displayName)
                .path(file.getAbsolutePath())
                .downloadId(createDownloadId(file))
                .type(file.isDirectory() ? "DIR" : "FILE")
                .size(rootDisplay ? file.getTotalSpace() : file.length())
                .time(rootDisplay ? "" : DateUtil.date(file.lastModified()).toString("yyyy-MM-dd HH:mm"))
                .build();
    }

    private String createDownloadId(File file) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(file.getAbsolutePath().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 同时输出 filename 与 filename*，提升中文名和下载器兼容性。
     */
    public void prepareAttachmentHeaders(HttpServletResponse response, String fileName, String contentType) {
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", buildContentDisposition(fileName));
    }

    public String buildContentDisposition(String fileName) {
        String fallbackName = fileName.replace("\"", "");
        String encodedName = URLUtil.encode(fallbackName, StandardCharsets.UTF_8);
        return "attachment; filename=\"" + fallbackName + "\"; filename*=UTF-8''" + encodedName;
    }

    private void setContentDisposition(HttpServletResponse response, String fileName) {
        response.setHeader("Content-Disposition", buildContentDisposition(fileName));
    }
}
