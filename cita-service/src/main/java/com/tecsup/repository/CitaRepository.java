package com.tecsup.repository;

import com.tecsup.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Integer> {

    List<Cita> findByEstadoOrderByFechaAscHoraAsc(String estado);
}
