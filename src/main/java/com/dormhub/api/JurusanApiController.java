package com.dormhub.api;

import com.dormhub.model.Jurusan;
import com.dormhub.service.JurusanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jurusan")
public class JurusanApiController {

    @Autowired
    private JurusanService jurusanService;

    @GetMapping
    public ResponseEntity<List<Jurusan>> getAllJurusan() {
        List<Jurusan> jurusanList = jurusanService.getAllJurusan();
        return ResponseEntity.ok(jurusanList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Jurusan> getJurusanById(@PathVariable int id) {
        Jurusan jurusan = jurusanService.findById(id);
        if (jurusan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(jurusan);
    }

    @PostMapping
    public ResponseEntity<Jurusan> createJurusan(@RequestBody Jurusan jurusan) {
        Jurusan savedJurusan = jurusanService.saveJurusan(jurusan);
        return ResponseEntity.ok(savedJurusan);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Jurusan> updateJurusan(@PathVariable int id, @RequestBody Jurusan updatedJurusan) {
        Jurusan jurusan = jurusanService.findById(id);
        if (jurusan == null) {
            return ResponseEntity.notFound().build();
        }
        jurusan.setNama(updatedJurusan.getNama());
        jurusanService.saveJurusan(jurusan);
        return ResponseEntity.ok(jurusan);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJurusan(@PathVariable int id) {
        jurusanService.deleteJurusan(id);
        return ResponseEntity.noContent().build();
    }
}
