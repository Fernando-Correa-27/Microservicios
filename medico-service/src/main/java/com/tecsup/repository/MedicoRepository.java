package com.tecsup.repository;

import com.tecsup.model.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, Integer> {

    boolean existsByCmp(String cmp);

    boolean existsByCmpAndIdMedicoNot(String cmp, Integer idMedico);
}
