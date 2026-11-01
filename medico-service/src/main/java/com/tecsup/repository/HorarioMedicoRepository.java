package com.tecsup.repository;

import com.tecsup.model.HorarioMedico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HorarioMedicoRepository extends JpaRepository<HorarioMedico, Integer> {

    List<HorarioMedico> findByMedico_IdMedicoOrderByDiaSemanaAscHoraInicioAsc(Integer idMedico);

    void deleteByMedico_IdMedico(Integer idMedico);
}
