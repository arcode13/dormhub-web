package com.dormhub.api;

import com.dormhub.model.SeniorResidence;
import com.dormhub.service.SeniorResidenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/senior-residence")
public class SeniorResidenceApiController {

    @Autowired
    private SeniorResidenceService seniorResidenceService;

    @GetMapping
    public ResponseEntity<List<SeniorResidence>> getAllSeniorResidence() {
        List<SeniorResidence> seniorResidenceList = seniorResidenceService.getAllSeniorResidence();
        return ResponseEntity.ok(seniorResidenceList);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSeniorResidence(@PathVariable int id) {
        seniorResidenceService.deleteSeniorResidence(id);
        return ResponseEntity.ok().body("{\"message\": \"Senior Residence berhasil dihapus\"}");
    }
}