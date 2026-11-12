package pe.sga.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface IStorageService {

    void init();

    String store(MultipartFile file);

    String store(MultipartFile file, String subpath);

    Resource loadAsResource(String pathOrFilename);

    void delete(String pathOrFilename);
}
