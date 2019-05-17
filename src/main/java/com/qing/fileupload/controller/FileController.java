package com.qing.fileupload.controller;

import com.qing.fileupload.dto.UploadResponse;
import com.qing.fileupload.service.FileService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping(value = "/file")
public class FileController {

    private FileService fileService;

    @PostMapping(value = "/upload")
    public UploadResponse uploadFile(@RequestParam(value = "file") MultipartFile file) {
        String fileName = fileService.storeFile(file);
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path("/file/downloadFile/").path(fileName).toUriString();
        return new UploadResponse(fileName, downloadUrl, file.getContentType(), file.getSize());
    }

    @PostMapping(value = "/uploadMultiple")
    public List<UploadResponse> uploadMultipleFile(@RequestParam(value = "files") MultipartFile[] files) {
        return Arrays.stream(files).map(this::uploadFile).collect(Collectors.toList());
    }

    @GetMapping("/downloadFile/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        // Load file as Resource
        Resource resource = fileService.loadFileAsResource(fileName);
        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }
        // Fallback to the default content type if type could not be determined
        if(contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
