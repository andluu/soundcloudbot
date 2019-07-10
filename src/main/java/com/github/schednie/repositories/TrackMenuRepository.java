package com.github.schednie.repositories;

import com.github.schednie.model.TrackMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackMenuRepository extends JpaRepository<TrackMenu, Long> {
}
