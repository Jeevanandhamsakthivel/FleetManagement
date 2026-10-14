package com.fleet.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fleet.domain.VehicleDocument;
import com.fleet.repo.VehicleDocumentRepository;
import com.fleet.security.CurrentUser;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final VehicleDocumentRepository documents;
    private final CurrentUser currentUser;

    public DocumentController(VehicleDocumentRepository documents, CurrentUser currentUser) {
        this.documents = documents;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<VehicleDocument> list() {
        return documents.findAllByCompanyIdOrderByExpiryDateAsc(currentUser.companyId());
    }

    @PostMapping
    public VehicleDocument create(@RequestBody VehicleDocument body) {
        body.id = null;
        body.companyId = currentUser.companyId();
        body.createdAt = null;
        return documents.save(body);
    }

    @PutMapping("/{id}")
    public VehicleDocument update(@PathVariable Long id, @RequestBody VehicleDocument body) {
        VehicleDocument d = require(id);
        body.id = d.id;
        body.companyId = d.companyId;
        body.createdAt = d.createdAt;
        return documents.save(body);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        documents.delete(require(id));
    }

    private VehicleDocument require(Long id) {
        VehicleDocument d = documents.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (!d.companyId.equals(currentUser.companyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your document");
        }
        return d;
    }
}
