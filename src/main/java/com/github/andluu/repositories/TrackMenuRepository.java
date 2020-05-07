package com.github.andluu.repositories;

import com.github.andluu.model.TrackMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackMenuRepository extends JpaRepository<TrackMenu, Long> {
}
