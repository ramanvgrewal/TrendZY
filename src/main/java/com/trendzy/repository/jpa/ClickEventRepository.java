package com.trendzy.repository.jpa;

import com.trendzy.model.jpa.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
}
