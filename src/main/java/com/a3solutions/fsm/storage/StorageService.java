package com.a3solutions.fsm.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.storage
 * @project A3 Field Service Management Backend
 * @date 11/19/25
 */
public interface StorageService {
    /**
     * Store the file and return the public or retrievable URL/path.
     */
    String store(MultipartFile file);
}
