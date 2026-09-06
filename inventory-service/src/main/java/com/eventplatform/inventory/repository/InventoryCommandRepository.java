package com.eventplatform.inventory.repository;

import com.eventplatform.inventory.domain.InventoryCommand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryCommandRepository extends JpaRepository<InventoryCommand, String> {
}
